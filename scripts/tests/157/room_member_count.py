#!/usr/bin/env python3
"""Verify realtime room creation and member-count updates.

The script creates three temporary users, creates a room with capacity three,
and verifies that a user watching the room list sees the new room and its
count, then sees the count change from 1 to 2 and back to 1.

Usage:
  python3.14 scripts/tests/157/room_member_count.py --speed human

BASE_URL and HEADED=1 can be used to override the defaults.
"""

from __future__ import annotations

import argparse
import os
import re
import time
from dataclasses import dataclass
from pathlib import Path

from playwright.sync_api import BrowserContext, Page, TimeoutError, expect, sync_playwright


BASE_URL = os.getenv("BASE_URL", "https://127.0.0.1:8443").rstrip("/")
PASSWORD = "RoomMemberCount157-test-password"
SLOW_MO_BY_SPEED = {"fast": 0, "human": 700}


@dataclass(frozen=True)
class User:
    name: str
    email: str
    password: str = PASSWORD


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify realtime room creation and member-count updates."
    )
    parser.add_argument(
        "--speed",
        choices=tuple(SLOW_MO_BY_SPEED),
        default="fast",
        help="操作速度。fastは高速、humanはブラウザを表示して確認用の速度で実行します。",
    )
    return parser.parse_args()


def register_user(context: BrowserContext, user: User) -> None:
    csrf_response = context.request.get(f"{BASE_URL}/api/v1/auth/csrf")
    if csrf_response.status != 204:
        raise RuntimeError(
            f"CSRFトークンの取得に失敗しました: HTTP {csrf_response.status}"
        )
    csrf_cookie = next(
        (cookie["value"] for cookie in context.cookies() if cookie["name"] == "XSRF-TOKEN"),
        None,
    )
    if not csrf_cookie:
        raise RuntimeError("CSRFトークンCookieが設定されませんでした")
    response = context.request.post(
        f"{BASE_URL}/api/v1/auth/register",
        data={"name": user.name, "email": user.email, "password": user.password},
        headers={"X-CSRF-TOKEN": csrf_cookie},
    )
    if response.status not in (200, 201):
        raise RuntimeError(
            f"ユーザー登録に失敗しました: HTTP {response.status} {response.text()}"
        )


def login(page: Page, user: User) -> None:
    page.goto(BASE_URL, wait_until="networkidle")
    if page.get_by_text("ルーム一覧").count() > 0:
        return
    page.get_by_role("button", name="ログイン").click()
    expect(page.get_by_role("heading", name="ログイン")).to_be_visible()
    page.locator('input[type="email"]').fill(user.email)
    page.locator('input[type="password"]').fill(user.password)
    with page.expect_response(
        lambda response: response.url.endswith("/api/v1/auth/login")
        and response.request.method == "POST"
        and response.status == 200
    ):
        page.get_by_role("button", name="ログイン").click()
    expect(page.get_by_text("ルーム一覧")).to_be_visible()


def enter_room(page: Page, room_name: str) -> None:
    heading = page.get_by_role("heading", name=room_name, exact=True)
    expect(heading).to_be_visible()
    card = heading.locator("xpath=ancestor::div[.//button][1]")
    button = card.get_by_role("button", name="このルームに入室する")
    expect(button).to_be_enabled()
    with page.expect_response(
        lambda response: response.request.method == "POST"
        and response.url.endswith("/members/me")
    ) as join_response:
        button.click()
    if join_response.value.status not in (200, 201, 204):
        raise RuntimeError(
            f"ルーム参加APIが失敗しました: HTTP {join_response.value.status}"
        )
    expect(page.get_by_role("button", name="ホームへ")).to_be_visible()


def expect_member_count(page: Page, count: int) -> None:
    expect(page.locator("body")).to_contain_text(
        re.compile(rf"参加人数:?\s*{count} / 3 人")
    )


def debug_websocket(page: Page, label: str) -> None:
    if os.getenv("DEBUG_WS") != "1":
        return

    def on_socket(socket) -> None:
        print(f"{label} websocket: {socket.url}")
        socket.on("framesent", lambda frame: print(f"{label} sent: {frame}"))
        socket.on("framereceived", lambda frame: print(f"{label} received: {frame}"))

    page.on("websocket", on_socket)
    page.on("console", lambda message: print(f"{label} console: {message.text}"))


def main() -> None:
    args = parse_args()
    suffix = str(int(time.time()))
    users = [
        User(f"room-member-count-{suffix}-1", f"room-member-count-{suffix}-1@example.com"),
        User(f"room-member-count-{suffix}-2", f"room-member-count-{suffix}-2@example.com"),
        User(f"room-member-count-{suffix}-3", f"room-member-count-{suffix}-3@example.com"),
    ]
    room_name = f"room-member-count-{suffix}"
    contexts: list[BrowserContext] = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(
            headless=os.getenv("HEADED") != "1" and args.speed == "fast",
            slow_mo=SLOW_MO_BY_SPEED[args.speed],
        )
        try:
            for user in users:
                context = browser.new_context(ignore_https_errors=True)
                contexts.append(context)
                register_user(context, user)

            creator, guest, observer = [context.new_page() for context in contexts]
            debug_websocket(creator, "creator")
            debug_websocket(guest, "guest")
            debug_websocket(observer, "observer")
            login(creator, users[0])
            login(observer, users[2])
            observer.wait_for_timeout(2000)
            creator.get_by_role("button", name="＋ 新しいルームを作成").click()
            expect(creator.get_by_role("heading", name="ルーム作成")).to_be_visible()
            creator.locator('input[type="text"]').nth(0).fill(room_name)
            creator.locator("textarea").fill("Playwright realtime member count verification")
            creator.locator('input[type="number"]').fill("3")
            with creator.expect_response(
                lambda response: response.url.endswith("/api/v1/rooms")
                and response.request.method == "POST"
                and response.status in (200, 201)
            ) as create_response:
                creator.get_by_role("button", name="ルームを作成する").click()
            room_id = create_response.value.json()["id"]
            expect(creator.get_by_role("button", name="ホームへ")).to_be_visible()
            expect_member_count(creator, 1)
            creator.wait_for_timeout(1000)
            expect(observer.get_by_role("heading", name=room_name, exact=True)).to_be_visible()
            expect_member_count(observer, 1)

            login(guest, users[1])
            enter_room(guest, room_name)
            expect_member_count(creator, 2)
            expect_member_count(observer, 2)

            with guest.expect_response(
                lambda response: response.request.method == "DELETE"
                and response.url.endswith("/members/me")
                and response.status == 204
            ):
                guest.get_by_role("button", name="ホームへ").click()
            expect(guest.get_by_text("ルーム一覧")).to_be_visible()
            expect_member_count(creator, 1)
            expect_member_count(observer, 1)

            print(
                "Realtime room-list/member-count verification: PASS "
                f"(room={room_name}, room_id={room_id}, counts=1->2->1)"
            )
        except (AssertionError, TimeoutError):
            screenshot = Path("scripts/tests/157/room-member-count-failure.png")
            if contexts and contexts[0].pages:
                contexts[0].pages[0].screenshot(path=screenshot, full_page=True)
            print(f"Playwright verification failed. Screenshot: {screenshot}")
            raise
        finally:
            for context in contexts:
                for page in context.pages:
                    try:
                        if page.get_by_role("button", name="ホームへ").count() > 0:
                            page.get_by_role("button", name="ホームへ").click()
                    except (AssertionError, TimeoutError):
                        pass
                context.close()
            browser.close()


if __name__ == "__main__":
    main()
