import { type FormEvent, useState } from "react";

type RegistrationForm = {
  name: string;
  email: string;
  password: string;
  passwordConfirmation: string;
};

type RegistrationErrors = Partial<Record<keyof RegistrationForm, string>>;

export function RegistrationPage() {
  const [form, setForm] = useState<RegistrationForm>({
    name: "",
    email: "",
    password: "",
    passwordConfirmation: ""
  });
  const [errors, setErrors] = useState<RegistrationErrors>({});
  const [formError, setFormError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRegistered, setIsRegistered] = useState(false);

  const updateField = (field: keyof RegistrationForm, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setFormError("");
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const validationErrors = validateRegistrationForm(form);
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    setFormError("");

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

      const registrationResponse = await fetch("/api/v1/auth/register", {
        method: "POST",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          "X-CSRF-TOKEN": csrfToken
        },
        body: JSON.stringify({
          name: form.name.trim(),
          email: form.email.trim(),
          password: form.password
        })
      });

      if (registrationResponse.status === 409) {
        setFormError("このメールアドレスはすでに登録されています。");
        return;
      }
      if (registrationResponse.status === 400) {
        setFormError("入力内容を確認してください。");
        return;
      }
      if (!registrationResponse.ok) {
        throw new Error("Registration failed");
      }

      setIsRegistered(true);
    } catch {
      setFormError("登録に失敗しました。時間をおいて再度お試しください。");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isRegistered) {
    return (
      <main className="app-shell">
        <section className="panel registration-panel" aria-live="polite">
          <p className="eyebrow">Online Workspace</p>
          <h1>登録が完了しました</h1>
          <p>登録したメールアドレスでログインしてください。</p>
          <a className="primary-button" href="http://localhost:8080/login">
            ログイン画面へ
          </a>
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <section className="panel registration-panel">
        <p className="eyebrow">Online Workspace</p>
        <h1>ユーザー登録</h1>
        <p>名前、メールアドレス、パスワードを入力してください。</p>

        {formError && <p className="form-error" role="alert">{formError}</p>}

        <form onSubmit={handleSubmit} noValidate>
          <label className="field">
            <span>ユーザー名</span>
            <input
              type="text"
              value={form.name}
              maxLength={100}
              autoComplete="name"
              aria-invalid={Boolean(errors.name)}
              onChange={(event) => updateField("name", event.target.value)}
            />
            {errors.name && <small className="field-error">{errors.name}</small>}
          </label>

          <label className="field">
            <span>メールアドレス</span>
            <input
              type="email"
              value={form.email}
              maxLength={255}
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              onChange={(event) => updateField("email", event.target.value)}
            />
            {errors.email && <small className="field-error">{errors.email}</small>}
          </label>

          <label className="field">
            <span>パスワード</span>
            <input
              type="password"
              value={form.password}
              minLength={8}
              maxLength={72}
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              onChange={(event) => updateField("password", event.target.value)}
            />
            {errors.password && <small className="field-error">{errors.password}</small>}
          </label>

          <label className="field">
            <span>確認用パスワード</span>
            <input
              type="password"
              value={form.passwordConfirmation}
              autoComplete="new-password"
              aria-invalid={Boolean(errors.passwordConfirmation)}
              onChange={(event) => updateField("passwordConfirmation", event.target.value)}
            />
            {errors.passwordConfirmation && (
              <small className="field-error">{errors.passwordConfirmation}</small>
            )}
          </label>

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? "登録しています..." : "登録する"}
          </button>
        </form>

        <a className="secondary-button" href="http://localhost:8080/login">
          ログインへ戻る
        </a>
      </section>
    </main>
  );
}

function validateRegistrationForm(form: RegistrationForm): RegistrationErrors {
  const errors: RegistrationErrors = {};

  if (!form.name.trim()) {
    errors.name = "ユーザー名を入力してください。";
  }
  if (!form.email.trim()) {
    errors.email = "メールアドレスを入力してください。";
  } else if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) {
    errors.email = "メールアドレスの形式が正しくありません。";
  }
  if (form.password.length < 8) {
    errors.password = "パスワードは8文字以上で入力してください。";
  }
  if (form.password !== form.passwordConfirmation) {
    errors.passwordConfirmation = "パスワードが一致しません。";
  }

  return errors;
}

function readCookie(name: string): string | null {
  const cookie = document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`));

  return cookie ? decodeURIComponent(cookie.substring(name.length + 1)) : null;
}
