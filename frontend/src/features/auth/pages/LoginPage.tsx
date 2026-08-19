import { type FormEvent, useState } from "react";

type LoginForm = {
  email: string;
  password: string;
};

export function LoginPage() {
  const [form, setForm] = useState<LoginForm>({ email: "", password: "" });
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const registered = new URLSearchParams(window.location.search).get("registered") === "1";

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFormError("");

    if (!form.email.trim() || !form.password) {
      setFormError("メールアドレスとパスワードを入力してください。");
      return;
    }
    if (form.password.length > 72 || !/^[\x21-\x7E]+$/.test(form.password)) {
      setFormError("パスワードは半角の英字・数字・記号（空白を除く）で入力してください。");
      return;
    }

    setIsSubmitting(true);

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

      const loginResponse = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          "X-CSRF-TOKEN": csrfToken
        },
        body: JSON.stringify({
          email: form.email.trim(),
          password: form.password
        })
      });

      if (loginResponse.status === 401) {
        setFormError("メールアドレスまたはパスワードが正しくありません。");
        return;
      }
      if (loginResponse.status === 429) {
        setFormError("ログイン試行回数が上限に達しました。15分後に再度お試しください。");
        return;
      }
      if (loginResponse.status === 400 || loginResponse.status === 422) {
        setFormError("入力内容を確認してください。");
        return;
      }
      if (!loginResponse.ok) {
        throw new Error("Login failed");
      }

      window.location.replace("/");
    } catch {
      setFormError("ログインに失敗しました。時間をおいて再度お試しください。");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="app-shell">
      <section className="panel registration-panel">
        <p className="eyebrow">Online Workspace</p>
        <h1>ログイン</h1>
        {registered && <p role="status">登録が完了しました。ログインしてください。</p>}
        {formError && <p className="form-error" role="alert">{formError}</p>}

        <form onSubmit={handleSubmit} noValidate>
          <label className="field">
            <span>メールアドレス</span>
            <input
              type="email"
              value={form.email}
              maxLength={255}
              autoComplete="email"
              onChange={(event) => setForm((current) => ({ ...current, email: event.target.value }))}
            />
          </label>

          <label className="field">
            <span>パスワード</span>
            <input
              type="password"
              value={form.password}
              maxLength={72}
              autoComplete="current-password"
              onChange={(event) => setForm((current) => ({ ...current, password: event.target.value }))}
            />
          </label>

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "ログインしています..." : "ログインする"}
          </button>
        </form>

        <a className="secondary-button" href="/register">
          ユーザー登録へ
        </a>
      </section>
    </main>
  );
}

function readCookie(name: string): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`));

  return cookie ? decodeURIComponent(cookie.substring(name.length + 1)) : null;
}
