# Webセキュリティ方針

## HTTPS / WSS

ブラウザからのREST API・画面配信はHTTPS、`/ws` のWebSocket通信はWSSを使用する。
TLSはNginxで終端し、Dockerネットワーク内のNginxからSpring Boot / Viteへの通信はHTTPとする。

ローカルでは、最初に自己署名証明書を生成してから起動する。

```bash
./scripts/generate-local-tls.sh
docker compose up
```

- HTTPS: `https://localhost:8443`
- WSS endpoint: `wss://localhost:8443/ws`（SockJSクライアントから接続する）
- `http://localhost:8088` はHTTPSへリダイレクトする

自己署名証明書のため、ブラウザでは初回だけ警告が表示される。CLIでは次のように確認する。

```bash
curl -kI https://localhost:8443/
curl -I http://localhost:8088/
curl -kI https://localhost:8443/ws/info
```

HTTPSレスポンスにはHSTS、CSP、Permissions-Policy、Referrer-Policy、
X-Content-Type-Options、X-Frame-Optionsを付与する。WebSocketのUpgradeはNginxからSpring Bootへ転送する。

## CSRF / CORS / Cookie

- React、REST API、WebSocketは同一オリジンのNginx proxy経由で利用し、クロスオリジン通信は許可しない。
- ブラウザは`GET /api/v1/auth/csrf`で`XSRF-TOKEN` Cookieを取得し、状態変更リクエストで同じ値を`X-CSRF-TOKEN`ヘッダーへ設定する。
- `SESSION` Cookieは`Secure`、`HttpOnly`、`SameSite=Lax`とする。
- `XSRF-TOKEN` CookieはReactから読むため`HttpOnly`を付けず、`Secure`、`SameSite=Lax`とする。
- Nginxが付ける`X-Forwarded-*`はSpring Bootで解釈し、外部URLとCookieのHTTPS属性を維持する。

## SQLインジェクション / XSS

- MyBatisの入力値は`#{...}`、JdbcTemplateの入力値は`?`でバインドし、文字列連結やMyBatisの`${...}`を使用しない。
- Reactの通常のJSX表示による自動エスケープを使用し、`dangerouslySetInnerHTML`、`innerHTML`、`eval`を使用しない。
- CSPを追加の防御層として使用する。

確認例:

```bash
rg '\$\{' src/main/java/com/example/online_workspace/repositories
rg 'dangerouslySetInnerHTML|innerHTML|document\.write|eval\(' frontend/src
```

いずれも該当なしが正常となる。

## 監査ログ

認証成功・失敗、ログアウト成功、401 / 403の認可拒否を`SECURITY_AUDIT` loggerへ記録する。
ログには時刻、対象区分、成否、認証方式または拒否理由のクラス名、HTTPメソッド、statusのみを含める。
ユーザー名・メールアドレス、URL・query、request / response body、Cookie、token、API keyは記録しない。
