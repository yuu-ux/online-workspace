import { useEffect, useState } from "react";

type SessionStatus = {
  authenticated: boolean;
  user: unknown | null;
};

export function SessionStatusPanel() {
  const [sessionStatus, setSessionStatus] = useState<SessionStatus | null>(null);
  const [error, setError] = useState(false);

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

  return (
    <section aria-live="polite">
      <h2>Session status</h2>
      {error && <p role="alert">認証状態を取得できませんでした。</p>}
      {!error && !sessionStatus && <p>認証状態を確認しています...</p>}
      {sessionStatus && (
        <p>
          {sessionStatus.authenticated
            ? "ログイン済みです。"
            : "現在は未ログインです。"}
        </p>
      )}
    </section>
  );
}
