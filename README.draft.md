*This project has been created as part of the 42 curriculum by \[login1\], \[login2\], \[login3\], \[login4\].*

# **Online Workspace (ft\_transcendence)**

## **概要 (Description)**

Online Workspaceは、包括的なコラボレーションプラットフォームおよびリアルタイムコミュニケーションWebアプリケーションです。堅牢なユーザー管理システム、リアルタイムのチャットメッセージング、および「Room（ルーム）」を通じた組織化ツールを備えています。高度なオブザーバビリティ（可観測性）、セキュリティ、パフォーマンスを重視し、マイクロサービスにインスパイアされた最新のアーキテクチャで構築されています。

## **実行手順 (Instructions)**

### **前提条件**

> * Docker および Docker Compose  
> * Java 17以上 (ローカル開発用)  
> * Node.js および npm (フロントエンド開発用)  
> * Gleam (フロントエンド開発用)

### **セットアップと実行**

> 1. リポジトリのクローン:  
>    git clone \<repository\_url\>  
>    cd online-workspace

> 2. 環境変数の設定:  
>    サンプルファイルをコピーし、必要なクレデンシャル情報を入力します。  
>    cp .env.example .env

> 3. ローカルTLS証明書の生成 (HTTPSに必須):  
>    bash scripts/generate-local-tls.sh

> 4. Docker Composeを使用したビルドと起動:  
>    docker-compose up \--build \-d

>    *監視スタック（ELK、Prometheus、Grafana）を含める場合は以下を実行します:*  
>    docker-compose \-f compose.yaml \-f compose.observability.yaml up \--build \-d

> 5. ブラウザから https://localhost (または設定したドメイン) にアクセスします。

## **リソース (Resources)**

> * **フレームワーク:** Spring Boot公式ドキュメント、Gleam言語ドキュメント、Vite  
> * **DevOps:** Dockerドキュメント、Elastic Stack (ELK) ガイド、Prometheus & Grafanaドキュメント  
> * **AIの使用について:** 本プロジェクトの開発において、以下のタスクにAIツール（ChatGPT、GitHub Copilot等）を使用しました：  
  * Nginxの設定およびDockerfileの初期セットアップの補助。  
  * ELKスタックとSpring Bootバックエンドの統合に関するトラブルシューティングのアイデア出し。  
  * バックエンドの単体テスト用ボイラープレートコードの生成。  
    *（注：AIによって生成されたコードはすべてチームで徹底的にレビュー、テスト、修正され、プロジェクトの要件に準拠し、完全に理解した上で使用しています。）*

## **チーム情報 (Team Information)**

> * **\[login1\] (Product Owner / Developer):** プロジェクトのビジョン定義、機能の優先順位付け、フロントエンドのチャットインターフェースの実装を担当。  
> * **\[login2\] (Scrum Master / Developer):** タスク分配の整理、GitHub Issuesの管理、ユーザー認証およびセッション管理のバックエンド開発を担当。  
> * **\[login3\] (Tech Lead / Developer):** システム全体のアーキテクチャ設計、Dockerインフラストラクチャの構築、CI/CDワークフローの実装を担当。  
> * **\[login4\] (Developer):** 監視スタック（ELK、Prometheus、Grafana）の実装、およびルーム管理機能の開発を担当。

## **プロジェクト管理 (Project Management)**

> * **組織体制:** Discordで毎週の定例ミーティングを行い、進捗状況とブロッカー（障害）について話し合いました。  
> * **タスク管理:** GitHub IssuesとGitHub Projects（カンバンボード）を使用して、タスク、バグ、機能リクエストを追跡しました。  
> * **コードレビュー:** mainブランチへのマージはすべてPull Request経由とし、少なくとも1名のピアレビューによる承認を必須としました。

## **技術スタック (Technical Stack)**

> * **フロントエンド:**  
  * **Gleam:** 堅牢なUIコンポーネントを構築するために使用した、型安全な関数型言語。  
  * **Vite:** 高速な開発とバンドルを実現する次世代のフロントエンドツール。  
  * **HTML/CSS:** カスタムスタイリング。  
> * **バックエンド:**  
  * **Java / Spring Boot:** 堅牢でセキュア、かつスケーラブルなREST APIおよびWebSocketサーバーを提供。  
  * **Gradle:** ビルド自動化ツール。  
> * **データベース:**  
  * **リレーショナルデータベース (PostgreSQL/MySQL等):** Flywayマイグレーション（V1\_\_create\_mvp\_schema.sql等）により管理され、スキーマの一貫性を保証。  
> * **インフラストラクチャ & DevOps:**  
  * **Docker & Docker Compose:** 一貫したデプロイのためのコンテナ化。  
  * **Nginx:** HTTPSルーティングを処理するリバースプロキシ。  
  * **オブザーバビリティ (可観測性):**  
    * **ELK Stack (Elasticsearch, Logstash, Kibana):** ログの中央集約と分析。  
    * **Prometheus & Grafana:** システムメトリクスの収集とヘルス状況の可視化。  
> * **セキュリティ:** Spring Security (APIキー認証、CSRF保護、レート制限)。

## **データベーススキーマ (Database Schema)**

データベーススキーマは以下のコアエンティティを中心に構成されています：

> * users / user\_accounts: 認証情報とプロフィール詳細を保存。  
> * rooms / room\_categories: コラボレーションスペースの管理。  
> * room\_memberships: どのユーザーがどのルームに属しているかを追跡。  
> * chat\_messages: リアルタイムのやり取りの履歴を保存。  
>   *（スキーマの視覚的表現については docs/db\_schema.md または docs/system\_architecture.pdf を参照してください）*

## **機能リスト (Features List)**

> * **ユーザー認証:** パスワードのハッシュ化を伴うセキュアなサインアップ、ログイン、およびセッション管理。  
> * **プロフィール管理:** ユーザーは自身のプロフィールを表示・更新でき、アカウントの退会リクエストが可能。  
> * **リアルタイムチャット:** ルーム内またはユーザー間でのWebSocketベースのメッセージング。  
> * **ルーム管理:** 整理されたディスカッションのためのルームの作成、一覧表示、カテゴリ分け。  
> * **セキュリティ監査:** ログイン試行の追跡とAPIのレート制限（Rate Limiting）。  
> * **監視ダッシュボード:** アプリケーションのメトリクスとログのリアルタイムモニタリング。

## **モジュール (Modules)**

獲得目標ポイント: **14ポイント**

> * **\[Major \- 2pts\] フロントエンドとバックエンドのフレームワーク使用:** バックエンドにSpring Boot (Java)、フロントエンドにGleamを使用。  
> * **\[Major \- 2pts\] リアルタイム機能:** ライブチャットメッセージングのためにWebSockets（WebSocketConfig.java, ws.js）を実装。  
> * **\[Major \- 2pts\] ユーザーインタラクション:** 基本的なチャット（ChatMessageController）、プロフィール表示（ProfileController）、およびユーザー同士の関係性構築を含む。  
> * **\[Major \- 2pts\] 公開API:** APIキー、レート制限（ApiRateLimitFilter）、およびドキュメント（Swagger UI/OpenAPI）を備えた、完全に保護されたREST API。  
> * **\[Major \- 2pts\] ログ管理用インフラ:** 包括的なログ集約のためにDocker経由でELKスタック（Elasticsearch, Logstash, Kibana）をデプロイ。  
> * **\[Major \- 2pts\] モニタリングシステム:** システムメトリクスとヘルス追跡のためにPrometheusとGrafanaを統合。  
> * **\[Minor \- 1pt\] ORMの使用:** Spring BootバックエンドでJPA/Hibernateを利用（UserRepository, RoomRepository等）。  
> * **\[Minor \- 1pt\] データベースマイグレーション (独自の選択モジュール):** 強固でバージョン管理されたデータベーススキーマの進化（src/main/resources/db/migration/）のためにFlywayを利用し、すべての環境で一貫した状態を保証するため選択。

## **個人の貢献 (Individual Contributions)**

### yonuma

- **担当ロール:** Developer
- **担当領域:** ユーザー認証、フレンド・プロフィール、ルーム・リアルタイム更新

#### ユーザー登録・認証

- ユーザー登録APIと登録画面を実装
- 名前・メールアドレス・パスワードの入力検証を実装
- パスワードのBCryptハッシュ化、メールアドレスの正規化、重複チェックを実装
- ログイン・ログアウト・セッション認証を実装
- JSESSIONIDによるサーバー側セッション管理を実装
- 停止・退会済みユーザーのセッション再検証を実装
- CSRF対策、ログイン試行回数制限、認証監査ログを追加
- 認証・登録関連のOpenAPI定義と統合テストを追加

#### フレンド・プロフィール

- フレンド一覧・検索・追加・解除のAPI連携を実装
- プロフィール画面でフレンド状態を表示・更新できるように実装
- 非公開プロフィールでもフレンド状態を確認できるように実装
- 自分自身をフレンドにできないように制御
- フレンド操作のAPIエラー時に画面状態を元へ戻す処理を実装
- マイページに自分のプロフィール情報を表示
- 外部アイコンURLの表示、未設定・読み込み失敗時のフォールバック表示を実装
- アイコンURLのHTTPS制限と、外部画像表示のCSP設定を追加

#### ルーム・リアルタイム更新

- ルーム一覧・詳細表示をAPIと連携
- ルームの入室・退出処理と入室可否の表示を実装
- 満員ルームへの遷移防止とエラーメッセージ表示を実装
- ルーム作成日時・詳細日時を日本時間で表示
- ブラウザ更新後のルーム復帰を実装
- タブを閉じたときに退室処理を実行
- WebSocketによる入退室通知と参加人数のリアルタイム更新を実装
- API通知とWebSocket通知による参加人数の二重カウントを防止
- ルーム一覧APIに説明文を追加し、ホーム画面で表示

#### 主な課題と解決方法

- API入退室通知とWebSocket接続通知が重複して参加人数に反映される問題に対し、参加者一覧の更新と参加人数の更新を分離し、サーバーが通知する現在人数を利用するようにした。
- 非公開プロフィールでフレンド状態が取得できない問題に対し、公開範囲を守りながらフレンド状態だけを返すようにAPIを調整した。
- 外部アイコンがCSPやHTTP URLによって表示できない問題に対し、HTTPS URLだけを受け付け、画像表示用のCSPを設定した。

> * **\[login1\]:**  
  * Gleamを用いたフロントエンドアーキテクチャの設計。  
  * リアルタイムWebSocketクライアントロジック（ws.js）の実装。  
  * *直面した課題:* 関数型のGleamとJavaScriptの相互運用。*解決策:* 厳密なFFIバインディング（api.js, ws.js）を記述した。  
> * **\[login2\]:**  
  * CSRFおよびレート制限を含むSpring Securityの設定を開発。  
  * 認証およびセッションのRESTエンドポイントを構築。  
  * *直面した課題:* セッション管理におけるCORSとセキュアCookieの処理。*解決策:* OriginとSameSite Cookie属性を明示的に処理するようにSpring Securityを設定した。  
> * **\[login3\]:**  
  * Docker ComposeファイルとNginxリバースプロキシの構成。  
  * 自動テストとビルドのためのGitHub Actionsのセットアップ。  
  * *直面した課題:* ローカルDocker環境でのHTTPSの管理。*解決策:* 自己署名証明書の生成を自動化する generate-local-tls.sh スクリプトを作成した。  
> * **\[login4\]:**  
  * PrometheusメトリクスをSpring Bootに統合（WebSocketMetrics.java）。  
  * ELKスタックのパイプラインとGrafanaプロビジョニングダッシュボードのセットアップ。  
  * *直面した課題:* Spring BootからLogstashへログを効果的にルーティングすること。*解決策:* 構造化されたJSONログをLogstashコンテナに送信するためのカスタムLogbackアペンダーを構成した。
