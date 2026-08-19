import { useEffect, useState } from "react";

type SessionStatus = {
  authenticated: boolean;
  user: {
    id: number;
    name: string;
    email: string;
    role: string;
    accountStatus: string;
  } | null;
};

export function SessionStatusPanel() {
  const [sessionStatus, setSessionStatus] = useState<SessionStatus | null>(null);
  const [error, setError] = useState(false);
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");

  useEffect(() => {
    let cancelled = false;

    fetch("/api/v1/auth/session", {
      headers: { Accept: "application/json" }
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error(`Session API returned ${response.status}`);
        }
        return response.json() as Promise<SessionStatus>;
      })
      .then((status) => {
        if (!cancelled) {
          setSessionStatus(status);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(true);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogout = async () => {
    setIsLoggingOut(true);
    setLogoutError("");

    try {
      const csrfResponse = await fetch("/api/v1/auth/csrf", {
        headers: { Accept: "application/json" }
      });
      if (!csrfResponse.ok) {
        throw new Error("CSRF token could not be fetched");
      }

      const csrfToken = readCookie("XSRF-TOKEN");
      if (!csrfToken) {
        throw new Error("CSRF cookie was not set");
      }

      const logoutResponse = await fetch("/api/v1/auth/logout", {
        method: "POST",
        headers: {
          Accept: "application/json",
          "X-CSRF-TOKEN": csrfToken
        }
      });
      if (!logoutResponse.ok) {
        throw new Error("Logout failed");
      }

      window.location.replace("/login");
    } catch {
      setLogoutError("ログアウトに失敗しました。時間をおいて再度お試しください。");
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <section aria-live="polite">
      <h2>Session status</h2>
      {error && <p role="alert">認証状態を取得できませんでした。</p>}
      {!error && !sessionStatus && <p>認証状態を確認しています...</p>}
      {sessionStatus && (
        sessionStatus.authenticated && sessionStatus.user ? (
          <>
            <p>ログイン済みです。</p>
            <p>
              {sessionStatus.user.name}（{sessionStatus.user.email}）
            </p>
            {logoutError && <p className="form-error" role="alert">{logoutError}</p>}
            <button className="primary-button" type="button" onClick={handleLogout} disabled={isLoggingOut}>
              {isLoggingOut ? "ログアウトしています..." : "ログアウト"}
            </button>
          </>
        ) : (
          <>
            <p>現在は未ログインです。</p>
            <a className="secondary-button" href="/login">ログインへ</a>
          </>
        )
      )}
    </section>
  );
}

function readCookie(name: string): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`));

  return cookie ? decodeURIComponent(cookie.substring(name.length + 1)) : null;
}
