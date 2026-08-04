import { FormEvent, useState } from "react";
import { AlertTriangle, ArrowLeft, LoaderCircle } from "lucide-react";

type CsrfTokenResponse = {
  token: string;
};

type ApiError = {
  message?: string;
};

export function AccountWithdrawalPage() {
  const [password, setPassword] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const submitWithdrawal = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!confirmed || password.length === 0 || submitting) {
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const csrfResponse = await fetch("/api/v1/auth/csrf", {
        credentials: "same-origin",
      });
      if (!csrfResponse.ok) {
        throw new Error("退会手続きを開始できませんでした。");
      }

      const { token } = (await csrfResponse.json()) as CsrfTokenResponse;
      const response = await fetch("/api/v1/users/me", {
        method: "DELETE",
        credentials: "same-origin",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-TOKEN": token,
        },
        body: JSON.stringify({ password }),
      });

      if (!response.ok) {
        const apiError = (await response.json().catch(() => ({}))) as ApiError;
        throw new Error(apiError.message ?? "退会処理に失敗しました。");
      }

      window.location.assign("/?withdrawn=true");
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "退会処理に失敗しました。");
      setSubmitting(false);
    }
  };

  return (
    <main className="withdrawal-shell">
      <section className="withdrawal-card" aria-labelledby="withdrawal-title">
        <a className="withdrawal-back" href="/">
          <ArrowLeft aria-hidden="true" size={18} />
          マイページへ戻る
        </a>

        <div className="withdrawal-heading">
          <span className="withdrawal-warning-icon" aria-hidden="true">
            <AlertTriangle size={28} />
          </span>
          <div>
            <p className="withdrawal-eyebrow">アカウント設定</p>
            <h1 id="withdrawal-title">退会の確認</h1>
          </div>
        </div>

        <div className="withdrawal-notice">
          <h2>退会する前にご確認ください</h2>
          <ul>
            <li>退会完了後は、このアカウントでログインできません。</li>
            <li>作業履歴とプロフィール情報は、退会から30日後に削除されます。</li>
            <li>参加中のルームから退出し、現在の作業計測は終了します。</li>
          </ul>
        </div>

        <form className="withdrawal-form" onSubmit={submitWithdrawal}>
          <label htmlFor="withdrawal-password">
            本人確認のためパスワードを入力
          </label>
          <input
            id="withdrawal-password"
            name="password"
            type="password"
            autoComplete="current-password"
            maxLength={72}
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />

          <label className="withdrawal-confirmation">
            <input
              type="checkbox"
              checked={confirmed}
              onChange={(event) => setConfirmed(event.target.checked)}
            />
            <span>退会後はログインできず、30日後に対象データが削除されることを確認しました。</span>
          </label>

          {error && (
            <p className="withdrawal-error" role="alert" aria-live="assertive">
              {error}
            </p>
          )}

          <div className="withdrawal-actions">
            <a className="button-secondary" href="/">
              キャンセル
            </a>
            <button
              className="button-danger"
              type="submit"
              disabled={!confirmed || password.length === 0 || submitting}
            >
              {submitting && <LoaderCircle className="spin" aria-hidden="true" size={18} />}
              {submitting ? "退会処理中…" : "退会する"}
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}
