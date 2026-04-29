# 実装する機能

- 認証
    - ログイン
    - ユーザー登録
- オンラインスペース
    - スペースの作成
        - 人数制限
        - 誰でも入れる / フレンドのみ

# 作成ページ

- ユーザー
    - 登録
    - ログイン
    - マイページ
        - ユーザー情報の編集
- スペース一覧(トップページ)
- スペース作成画面
- スペース詳細

# テーブル設計

## ユーザー

- id
- name
- email
- password
- password_confirm
- created_at
- modified_at

## スペース

- id
- title
- description
- created_at
- modified_at


