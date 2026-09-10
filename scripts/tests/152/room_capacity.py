#!/usr/bin/env python3
"""Verify that a full room cannot be opened from the home page.

The script creates four temporary users, creates a room with a capacity of
three, and uses one isolated browser context per user. The first three users
enter successfully; the fourth user must remain on the home page and see a
disabled entry button.

Usage:
  pip install playwright
  playwright install chromium
  python3 scripts/tests/152/room_capacity.py

BASE_URL and HEADED=1 can be used to override the defaults.
"""

from __future__ import annotations

import os
import time
from dataclasses import dataclass
from pathlib import Path

from playwright.sync_api import BrowserContext, Page, TimeoutError, expect, sync_playwright


BASE_URL = os.getenv("BASE_URL", "https://127.0.0.1:8443").rstrip("/")
PASSWORD = "RoomCapacity152-test-password"


@dataclass(frozen=True)
class User:
    name: str
    email: str
    password: str = PASSWORD


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


def go_home(page: Page) -> None:
    page.get_by_role("button", name="ホームへ").click()
    expect(page.get_by_text("ルーム一覧")).to_be_visible()


def room_entry_button(page: Page, room_name: str):
    heading = page.get_by_role("heading", name=room_name, exact=True)
    expect(heading).to_be_visible()
    card = heading.locator("xpath=ancestor::div[.//button][1]")
    return card.get_by_role("button")


def enter_room(page: Page, room_name: str) -> None:
    button = room_entry_button(page, room_name)
    expect(button).to_be_enabled()
    with page.expect_response(
        lambda response: "/api/v1/rooms/" in response.url
        and response.request.method == "POST"
        and response.url.endswith("/members/me")
    ) as join_response:
        button.click()
    if join_response.value.status not in (200, 201, 204):
        raise RuntimeError(
            f"ルーム参加APIが失敗しました: HTTP {join_response.value.status} "
            f"{join_response.value.text()}"
        )
    expect(page.get_by_role("button", name="ホームへ")).to_be_visible()


def main() -> None:
    suffix = str(int(time.time()))
    users = [
        User(f"room-capacity-{suffix}-1", f"room-capacity-{suffix}-1@example.com"),
        User(f"room-capacity-{suffix}-2", f"room-capacity-{suffix}-2@example.com"),
        User(f"room-capacity-{suffix}-3", f"room-capacity-{suffix}-3@example.com"),
        User(f"room-capacity-{suffix}-4", f"room-capacity-{suffix}-4@example.com"),
    ]
    room_name = f"room-capacity-{suffix}"
    contexts: list[BrowserContext] = []

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=os.getenv("HEADED") != "1")
        try:
            for user in users:
                context = browser.new_context(ignore_https_errors=True)
                contexts.append(context)
                register_user(context, user)

            pages = [context.new_page() for context in contexts]
            login(pages[0], users[0])

            pages[0].get_by_role("button", name="＋ 新しいルームを作成").click()
            expect(pages[0].get_by_role("heading", name="ルーム作成")).to_be_visible()
            text_inputs = pages[0].locator('input[type="text"]')
            text_inputs.nth(0).fill(room_name)
            pages[0].locator("textarea").fill("Playwright room capacity verification")
            pages[0].locator('input[type="number"]').fill("3")
            with pages[0].expect_response(
                lambda response: response.url.endswith("/api/v1/rooms")
                and response.request.method == "POST"
                and response.status in (200, 201)
            ) as create_response:
                pages[0].get_by_role("button", name="ルームを作成する").click()
            room_id = create_response.value.json()["id"]
            expect(pages[0].get_by_role("button", name="ホームへ")).to_be_visible()
            go_home(pages[0])

            for page, user in zip(pages[1:3], users[1:3]):
                login(page, user)
                enter_room(page, room_name)

            login(pages[3], users[3])
            full_button = room_entry_button(pages[3], room_name)
            expect(full_button).to_be_disabled()
            expect(full_button).to_contain_text("満員")
            expect(pages[3]).to_have_url(f"{BASE_URL}/")
            expect(pages[3].get_by_role("button", name="ホームへ")).not_to_be_visible()

            print(
                f"Room capacity verification: PASS "
                f"(room={room_name}, room_id={room_id}, capacity=3, users=4)"
            )
        except (AssertionError, TimeoutError):
            screenshot = Path("scripts/tests/152/room-capacity-failure.png")
            if contexts:
                contexts[-1].pages[0].screenshot(path=screenshot, full_page=True)
            print(f"Playwright verification failed. Screenshot: {screenshot}")
            raise
        finally:
            for page in [context.pages[0] for context in contexts if context.pages]:
                try:
                    if page.get_by_role("button", name="ホームへ").count() > 0:
                        page.get_by_role("button", name="ホームへ").click()
                except (AssertionError, TimeoutError):
                    pass
            for context in contexts:
                context.close()
            browser.close()


if __name__ == "__main__":
    main()
