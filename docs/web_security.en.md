# Web Security Policy

## HTTPS / WSS

The REST API and screens delivered to browsers use HTTPS, and WebSocket communication on `/ws` uses WSS.
TLS terminates at Nginx, while communication from Nginx to Spring Boot / Vite within the Docker network uses HTTP.

Locally, a self-signed certificate is generated automatically when the `proxy` container starts.

```bash
docker compose up
```

- HTTPS: `https://localhost:8443`
- WSS endpoint: `wss://localhost:8443/ws` (connect from the SockJS client)
- `http://localhost:8088` redirects to HTTPS

Because the certificate is self-signed, the browser displays a warning the first time. Verify it from the CLI as follows.

```bash
curl -kI https://localhost:8443/
curl -I http://localhost:8088/
curl -kI https://localhost:8443/ws/info
```

HTTPS responses include HSTS, CSP, Permissions-Policy, Referrer-Policy, X-Content-Type-Options, and X-Frame-Options. Nginx forwards the WebSocket Upgrade to Spring Boot.

## CSRF / CORS / Cookies

- React, the REST API, and WebSocket are used through the same-origin Nginx proxy, and cross-origin communication is not permitted.
- The browser obtains the `XSRF-TOKEN` cookie with `GET /api/v1/auth/csrf` and sets the same value in the `X-CSRF-TOKEN` header for state-changing requests.
- The `JSESSIONID` cookie uses `Secure`, `HttpOnly`, and `SameSite=Lax`.
- The `XSRF-TOKEN` cookie is not `HttpOnly` so that React can read it, and uses `Secure` and `SameSite=Lax`.
- Spring Boot interprets the `X-Forwarded-*` headers set by Nginx and preserves HTTPS attributes on external URLs and cookies.

## SQL Injection / XSS

- Bind MyBatis input values with `#{...}` and JdbcTemplate input values with `?`; do not use string concatenation or MyBatis `${...}`.
- Use the automatic escaping provided by normal React JSX rendering; do not use `dangerouslySetInnerHTML`, `innerHTML`, or `eval`.
- Use CSP as an additional layer of protection.

Example checks:

```bash
rg '\$\{' src/main/java/com/example/online_workspace/repositories
rg 'dangerouslySetInnerHTML|innerHTML|document\.write|eval\(' frontend/src
```

No matches is the expected result for either command.

## Audit Logs

Log successful and failed authentication, successful logout, and authorization denials with 401 / 403 to the `SECURITY_AUDIT` logger.
Logs contain only the timestamp, target category, success or failure, the authentication method or rejection-reason class name, the HTTP method, and the status.
Do not record usernames or email addresses, URLs or queries, request or response bodies, cookies, tokens, or API keys.
