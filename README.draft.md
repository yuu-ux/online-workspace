*This project has been created as part of the 42 curriculum by \[yehara\], \[tmuranak\], \[kofujita\], \[yonuma\].*

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

> * **\[yehara\] (Product Owner / Developer):** TODO
> * **\[kofujita\] (Scrum Master / Developer):** TODO
> * **\[tmuranak\] (Tech Lead / Developer):** フロントエンド周りGleamという純粋関数型言語を利用し、状態遷移の記述を得意とする言語、フレームワークを導入した。
> * **\[yonuma\] (Developer):** TODO

## **プロジェクト管理 (Project Management)**

> * **組織体制:** Discordで毎週の定例ミーティングを行い、進捗状況とブロッカー（障害）について話し合いました。  
> * **タスク管理:** GitHub IssuesとGitHub Projects（カンバンボード）を使用して、タスク、バグ、機能リクエストを追跡しました。  
> * **コードレビュー:** mainブランチへのマージはすべてPull Request経由とし、少なくとも1名のピアレビューによる承認を必須としました。

## **技術スタック (Technical Stack)**

> * **フロントエンド:**  
  * **Gleam:** 堅牢なUIコンポーネントを構築するために使用した、型安全な関数型言語。  
  * **HTML/Tailwind CSS:** ウェブデザイン
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

> * **tmuranak (Tech Lead / Developer)**
  * Gleamを用いたフロントエンドアーキテクチャの設計。  
  * リアルタイムWebSocketクライアントロジック（ws.js）の実装。  
  * *直面した課題:*
  * サーバーサイドのAPIとの接続の困難さがあると感じたため、設計段階から必要になる可能性のある機能を積極的に切り分けた

> * **yonuma (Developer / Developer)**
  * ユーザー登録、ログイン・ログアウト、セッション認証などのユーザー認証機能を実装した。
  * フレンド検索・追加・解除、プロフィール・マイページ表示、フレンド状態やアイコン表示を実装した。
  * ルーム一覧・詳細・入退室、F5やタブ閉じへの対応、WebSocketによる参加人数のリアルタイム更新を実装した。
  * API通知とWebSocket通知による参加人数の二重カウント、非公開プロフィールのフレンド状態、外部アイコン表示に関する問題を解決した。
  * *直面した問題:*
  * sprintg security を触ったことがなかったので最初は何をしているのか分からなかった。この分野を実務で触っているチームメンバーのyeharaに相談し、疑問点を解消するように努めた。

> * **kofujita (Scrum Master / Developer)**  
  * 環境周りのレビュー。
  * データベース・バックエンドのレビュー。
  * 人手が足りていない場所のお手伝い。
  * 細かい場所はコミット参照。
  * *直面した課題:* 
  * 時間がなかった。-> 時間の余裕を見つけて、他の人のレビューをした。

> * **yehara (Product Owner / Developer)**
  * 
  * 要件定義、システム構成、DBスキーマ、OpenAPI契約を策定し、Flyway・Swagger UI・OpenAPI lintを用いた開発基盤を整備した。
  * Spring BootのAPI基盤を構築し、共通エラー形式、ルーム作成・一覧・参加・退出、チャット、フレンド管理などのAPIと統合テストを実装した。
  * ログイン・ログアウト・セッション管理を実装し、CSRF対策、APIキー認証、レート制限、アカウント状態の再検証、セキュリティ監査ログを整備した。
  * WebSocketによるリアルタイムチャットと在席状態を実装し、認証・ルーム・フレンド機能をGleamフロントエンドへ接続した。
  * Docker Compose、Nginx、ローカルHTTPS証明書の自動生成、CI、フロントエンドビルド環境を整備した。
  * ELK、Actuator、Prometheus、Grafanaの監視基盤を構築し、ダッシュボード、障害アラート、Discord通知を自動設定した。
  * 残り期間と最終要件に合わせ、管理機能、ブロック機能、タイマー・作業履歴などの対象外機能を整理・削除し、実装範囲を完成可能な形へ調整した。
  * *直面した課題:* 
  * 当初予定していた機能に対し、残された時間が少なかったため、要件満たせるようにしつつ不要な機能を削除した
