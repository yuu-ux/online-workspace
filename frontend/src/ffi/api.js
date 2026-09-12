function cookie(name) {
  return document.cookie
    .split("; ")
    .find((entry) => entry.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

export async function request(method, path, body, callback) {
  try {
    const headers = { Accept: "application/json" };
    if (method !== "GET") {
      await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
      headers["X-CSRF-TOKEN"] = decodeURIComponent(cookie("XSRF-TOKEN") || "");
    }
    if (body) headers["Content-Type"] = "application/json";

    const response = await fetch(path, {
      method,
      headers,
      body: body || undefined,
      credentials: "same-origin",
    });
    callback(response.status, await response.text());
  } catch (_) {
    callback(0, "");
  }
}

export function has_selected_file(inputId) {
  return Boolean(document.getElementById(inputId)?.files?.length);
}

export async function upload_file(inputId, path, callback) {
  try {
    const input = document.getElementById(inputId);
    const file = input?.files?.[0];
    if (!file) {
      callback(422, JSON.stringify({ message: "画像を選択してください。" }));
      return;
    }

    await fetch("/api/v1/auth/csrf", { credentials: "same-origin" });
    const form = new FormData();
    form.append("file", file);
    const response = await fetch(path, {
      method: "POST",
      headers: {
        Accept: "application/json",
        "X-CSRF-TOKEN": decodeURIComponent(cookie("XSRF-TOKEN") || ""),
      },
      body: form,
      credentials: "same-origin",
    });
    callback(response.status, await response.text());
  } catch (_) {
    callback(0, "");
  }
}
