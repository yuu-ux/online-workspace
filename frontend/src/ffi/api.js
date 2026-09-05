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
