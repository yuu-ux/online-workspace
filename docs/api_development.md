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
4. 正常系に加え、400、401、403、404、409、422、429 など対象 operation の異常系をテストする。
5. 次の lint を実行し、error がないことを確認する。

```bash
npx --yes @redocly/cli@2.43.2 lint online-workspace@v1
```

同じ lint は `OpenAPI lint` GitHub Actions でも実行される。

フロントエンドで利用できる TypeScript 型は次のコマンドで `build/generated/openapi.d.ts` に生成できる。React 基盤の導入後は、生成先をフロントエンドの API client 配下へ変更する。

```bash
npx --yes openapi-typescript@7.13.0
```

## レビュー観点

- `operationId` が一意で、フロントエンドの関数名として理解できるか
- request / response の型、required、nullable、format が実装と一致するか
- mutation の成功 status code と失敗時の error code が実装と一致するか
- 公開 API、セッション認証、API キー認証の指定が正しいか
- 一覧 API が `page` / `size` と `PageMeta` を使っているか
- 新しい schema を既存 schema の重複で表現していないか
