# ブランチ命名規則

ブランチ名は、次の形式で作成します。

```text
type/issue番号3桁-内容
```

## type

- `feature/`: 新機能の追加
- `modify/`: 既存機能の変更
- `fix/`: バグ修正
- `refactor/`: リファクタリング
- `docs/`: ドキュメントの追加・修正
- `test/`: テストの追加・修正

## 命名ルール

- Issue番号は3桁で記載する（例: `002`）。
- 内容は英小文字を使用し、単語はハイフン（`-`）で区切る。
- スペース、日本語、大文字、アンダースコア（`_`）は使用しない。

## 例

```text
feature/123-add-user-profile
fix/005-fix-login-validation
docs/072-add-branch-naming-convention
```
