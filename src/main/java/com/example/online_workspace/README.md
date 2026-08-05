# ファイル・ディレクトリ構成

## カレントディレクトリ

- OnlineWorkspaceApplication.java ... エントリーポイント(URLルーティング)
- SwaggerUiSecurityConfig.java ...... OpenAPIの定義をUI上に表示するためのもの

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
