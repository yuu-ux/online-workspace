# ファイル・ディレクトリ構成

## カレントディレクトリ

- OnlineWorkspaceApplication.java ... エントリーポイント(URLルーティング)
- configs/security/ApiSecurityConfig.java ... APIの認証・CSRF設定
- configs/security/WebSecurityConfig.java ... Web・Swagger UIの認証設定

## configs/securityディレクトリ

セキュリティ設定（SecurityFilterChain）をまとめる場所

## controllersディレクトリ

- `controllers/api/`: JSONを返すREST Controller。URLは `/api/v1/**` とする。
- `controllers/mvc/`: HTML画面を返すMVC Controller。Gleam中心の現在は使用しない。

認証系API（/api/v1/auth/**）のコントローラーは `controllers/api/auth/` にまとめる。

<br>

## configsディレクトリ

設定関連をまとめたソースコード群

<br>

## controllersディレクトリ

APIとしてアクセスする場所

※ コントローラ

<br>

## formsディレクトリ

コントローラで受け取った入力値のバリデーションチェックなどを行う場所

<br>

## modelsディレクトリ

DBのスキーマをJavaの世界で表現するための箱

<br>

## servicesディレクトリ

入力値を処理して出力値を作成するための場所

<br>

## repositoriesディレクトリ

データベースにアクセスして情報を取得するための場所
