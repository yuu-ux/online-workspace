# API 契約の更新手順

`docs/openapi.yaml` を React と Spring Boot の API 契約の正とする。API の実装を変更する Issue では、実装と同じ PR で OpenAPI も更新する。

## 更新が必要な変更

- path、HTTP method、query/path/header parameter の追加・変更・削除
- request / response body、status code、error code の追加・変更・削除
- 認証方式または認可条件の変更
- enum、文字数、数値範囲、必須項目などの validation 変更
- ページネーションや共通 schema の変更

WebSocket のイベント契約は `docs/websocket_events.md` で管理する。REST API と WebSocket の両方に影響する変更では両方を更新する。

## 作業手順

1. 対象 Issue の受入条件を確認し、先に `docs/openapi.yaml` の operation と schema を更新する。
2. Spring Boot の Controller、DTO、validation、security を契約に合わせて実装する。
3. React の型または API client を OpenAPI から再生成し、画面実装を更新する。
4. 正常系に加え、400、401、403、404、409、422、429、500 など対象 operation の異常系をテストする。
5. 次の lint を実行し、error がないことを確認する。

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
```

同じ lint は `OpenAPI lint` GitHub Actions でも実行される。

フロントエンドで利用できる TypeScript 型は次のコマンドで `build/generated/openapi.d.ts` に生成できる。React 基盤の導入後は、生成先をフロントエンドの API client 配下へ変更する。

```bash
npx --yes openapi-typescript@7.13.0
```

## 認証とrate limit

- Reactは最初に `GET /api/v1/auth/csrf` を呼び、発行された `XSRF-TOKEN` cookieの値をsession認証の状態変更リクエストで `X-CSRF-TOKEN` headerへ設定する。
- APIはSPA向けのCSRF request handlerを使用する。ReactやSwagger UIは `XSRF-TOKEN` cookieの値を `X-CSRF-TOKEN` headerへ設定する。
- `POST /api/v1/auth/login` は #16 の登録処理と同じく、メールアドレスの前後の空白を除去して `Locale.ROOT` で小文字化して照合する。成功時は `JSESSIONID` cookie にサーバー側セッションを保存する。
- ログインに5回連続で失敗した場合、6回目以降の同じメールアドレス・接続元アドレスからの試行を15分間 `429 Too Many Requests` とする。429レスポンスには `Retry-After: 900` を返し、成功したログインで失敗回数をリセットする。接続元アドレスは信頼済みリバースプロキシが設定する `X-Real-IP` を使用し、直接接続時はサーバーが認識した接続元アドレスを使用する。現状は単一アプリケーションインスタンス向けの上限付きインメモリ管理であり、複数インスタンス化時は共有ストアへ差し替える。
- `JSESSIONID` cookieは `HttpOnly=true`、`SameSite=Lax`、ローカル開発では `Secure=false` を既定値とする。本番では `SESSION_COOKIE_SECURE=true` を設定する。`SESSION_COOKIE_HTTP_ONLY`、`SESSION_COOKIE_SECURE`、`SESSION_COOKIE_SAME_SITE` で環境ごとに変更できる。
- `JSESSIONID` cookieは永続CookieにせずセッションCookieとして発行する。ページリロード中は認証状態を維持するが、ブラウザ終了後のログイン状態維持（Remember Me）は対象外とする。

### Reactからの認証API呼び出し

1. `GET /api/v1/auth/csrf` を呼び出し、レスポンスの `XSRF-TOKEN` cookieを読み取る。
2. `POST /api/v1/auth/login` に `{ "email": "user@example.com", "password": "..." }` をJSONで送信し、`X-CSRF-TOKEN` headerへCSRF tokenを設定する。成功時はレスポンスにユーザー情報が返り、`JSESSIONID` cookieが発行される。
3. リロード時やアプリ起動時は `GET /api/v1/auth/session` を呼び出す。未ログイン時も `200` で `authenticated: false` が返る。
4. ログアウト時は再度CSRF tokenを取得し、`POST /api/v1/auth/logout` に `JSESSIONID` cookieと `X-CSRF-TOKEN` headerを付けて送信する。成功時は `204` で、サーバーセッションと `JSESSIONID` cookieが無効化される。

ログイン失敗時は `401`、入力形式エラーは `422`、レート制限中は `429` となる。`429` では `Retry-After: 900` を再試行待機時間として使用する。
- API keyの採点対象は `GET /rooms`、`POST /rooms`、`GET /rooms/{roomId}`、`PUT /rooms/{roomId}`、`DELETE /rooms/{roomId}` の5 operationとする。
- 採点対象5 operationはsession認証またはAPI key認証を受け付ける。API keyはユーザーまたはservice principalへ紐付け、同じ認可ルールを適用する。
- 採点対象5 operationにはkey単位のrate limitを適用し、超過時は `429 Too Many Requests` と `Retry-After` headerを返す。

## レビュー観点

- `operationId` が一意で、フロントエンドの関数名として理解できるか
- request / response の型、required、nullable、format が実装と一致するか
- mutation の成功 status code と失敗時の error code が実装と一致するか
- 公開 API、セッション認証、API キー認証の指定が正しいか
- 一覧 API が `page` / `size` と `PageMeta` を使っているか
- 新しい schema を既存 schema の重複で表現していないか
