# API Contract Update Procedure

`docs/openapi.yaml` is the source of truth for the React and Spring Boot API contract. For Issues that change the API implementation, update OpenAPI in the same PR as the implementation.

## Changes Requiring an Update

- Adding, changing, or removing paths, HTTP methods, or query/path/header parameters
- Adding, changing, or removing request/response bodies, status codes, or error codes
- Changing authentication methods or authorization conditions
- Changing validation such as enums, character counts, numeric ranges, or required fields
- Changing pagination or common schemas

WebSocket event contracts are managed in [`docs/websocket_events.en.md`](websocket_events.en.md). When a change affects both the REST API and WebSocket, update both contracts.

## Procedure

1. Confirm the acceptance criteria for the target Issue, then update the operation and schemas in `docs/openapi.yaml` first.
2. Implement the Spring Boot Controller, DTOs, validation, and security to match the contract.
3. Regenerate the React types or API client from OpenAPI and update the screen implementation.
4. In addition to the happy path, test error cases such as 400, 401, 403, 404, 409, 422, 429, and 500 for the target operation.
5. Run the following lint and confirm that there are no errors.

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
```

The same lint also runs in the `OpenAPI lint` GitHub Actions workflow.

Frontend TypeScript types can be generated into `build/generated/openapi.d.ts` with the following command. After the React foundation is introduced, change the output location to the frontend API client directory.

```bash
npx --yes openapi-typescript@7.13.0
```

## Authentication and Rate Limiting

- React first calls `GET /api/v1/auth/csrf`, then sets the value of the issued `XSRF-TOKEN` cookie in the `X-CSRF-TOKEN` header for session-authenticated state-changing requests.
- The API uses an SPA-oriented CSRF request handler. React and Swagger UI set the value of the `XSRF-TOKEN` cookie in the `X-CSRF-TOKEN` header.
- Like the registration process in #16, `POST /api/v1/auth/login` trims whitespace around the email address and compares it after converting it to lowercase with `Locale.ROOT`. On success, the server-side session is stored in the `JSESSIONID` cookie.
- After five consecutive login failures, attempts from the same email address and source address are returned as `429 Too Many Requests` for 15 minutes starting with the sixth attempt. The 429 response returns `Retry-After: 900`, and a successful login resets the failure count. The source address is the address recognized by the server. When operating behind a reverse proxy, do not expose the application directly and configure forwarded headers appropriately on the proxy. The current implementation is a bounded in-memory mechanism for a single application instance; replace it with a shared store when scaling to multiple instances.
- The `JSESSIONID` cookie uses `HttpOnly=true` and `SameSite=Lax`. These settings can be changed per environment with `SESSION_COOKIE_HTTP_ONLY`, `SESSION_COOKIE_SECURE`, and `SESSION_COOKIE_SAME_SITE`; their defaults are `true`, `true`, and `lax`, respectively.
- The `JSESSIONID` cookie is issued as a session cookie rather than a persistent cookie. Authentication remains valid during page reloads, but keeping users logged in after the browser closes (Remember Me) is out of scope.

### Calling Authentication APIs from Gleam

1. Call `GET /api/v1/auth/csrf` and read the `XSRF-TOKEN` cookie from the response.
2. Send the login information as JSON to `POST /api/v1/auth/login` and set the CSRF token in the `X-CSRF-TOKEN` header. On success, user information is returned in the response and a `JSESSIONID` cookie is issued.
3. Call `GET /api/v1/auth/session` when reloading the page or starting the application. Even when not logged in, the response is `200` with `authenticated: false`.
4. When a logged-in user logs out, send `POST /api/v1/auth/logout` with the `X-CSRF-TOKEN` header. The browser automatically sends the stored `JSESSIONID` cookie. On success, the response is `204`, the server session is invalidated, and the `JSESSIONID` cookie is deleted.

Login failures return `401`; calling logout without authentication returns `401`; a missing or invalid CSRF token returns `403`; invalid input format returns `422`; and requests under rate limiting return `429`. For `429`, use `Retry-After: 900` as the retry wait time. The CSRF cookie is always issued with `Secure=true`, `SameSite=Lax`, and without HttpOnly, so it must be used over HTTPS.

- The five operations subject to API key evaluation are `GET /public/rooms`, `POST /public/rooms`, `GET /public/rooms/{roomId}`, `PUT /public/rooms/{roomId}`, and `DELETE /public/rooms/{roomId}`.
- The five evaluated operations require an API key and cannot be used with session authentication. The API key is a single shared key configured through an environment variable; after authentication, requests are processed as the configured fixed principal.
- Apply a fixed-window rate limit shared by all API key requests to the five evaluated operations. When the limit is exceeded, return `429 Too Many Requests` and a `Retry-After` header. The default is 60 requests per minute, managed within a single application instance.
- `/actuator/health` and `/actuator/prometheus` are monitoring endpoints outside the API prefix and require a separate management API key from the room APIs. Session authentication, the API rate limit, and the API problem+json error format do not apply.

## Review Checklist

- Is `operationId` unique and understandable as a frontend function name?
- Do the request/response types, `required`, `nullable`, and `format` match the implementation?
- Do the success status codes for mutations and error codes on failure match the implementation?
- Are the specifications for the public API, session authentication, and API key authentication correct?
- Do list APIs use `page` / `size` and `PageMeta`?
- Is a new schema represented without duplicating an existing schema?
