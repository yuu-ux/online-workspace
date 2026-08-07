# ファイル・ディレクトリ構成

## カレントディレクトリ

- OnlineWorkspaceApplication.java ... エントリーポイント(URLルーティング)
- SwaggerUiSecurityConfig.java ... API/API Web security 設定

## configs/securityディレクトリ

セキュリティ設定（SecurityFilterChain）をまとめる場所

## controllers/authディレクトリ

認証系API（/api/v1/auth/**）のコントローラーをまとめる場所

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
