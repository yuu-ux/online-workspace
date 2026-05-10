# 必須要件

- Google Chrome の最新安定バージョンと互換性があること
- ブラウザのコンソールに警告やエラーメッセージが表示されてはならない
- プライバシーポリシー・利用規約の作成
- CSSフレームワークを使用すること
- 合計14ポイント取得
- README は必ず記述する必要がある

# 実装モジュール

- フロント・バックエンドフレームワーク使用(2pt)
- WebSocket を使用してリアルタイム機能を実装(2pt)
- ユーザーが他のユーザーと相互にやり取りできるようにする(2pt)
    - チャットシステム
    - プロフィールシステム
    - フレンドシステム
- 以下の要件を満たしたエンドポイントを最低5つ作成する(2pt)
    - APIキーで守られた
    - DB操作ができる
    - GET/POST/PUT/DELETE を含む
    - rate limitがある
    - APIドキュメントがある
- ORM の使用(1pt)
- 作成、更新、削除をした時にユーザーへ通知を出す(1pt)
    - 画面上のフラッシュメッセージを表示する
- リアルタイムの共同作業機能(1pt)
- SSRの実装(1pt)
    - ランディングページとかトップページならできるかも
    - 作業部屋とかはCSRになるりそう
- 標準低なユーザー管理および認証機能(2pt)
    - ユーザーがプロフィールの更新ができること
    - プロフィール画像を設定できること
    - 他のユーザーをフレンドに追加できることまた、オンラインステータスを確認できること
    - プロフィールページが確認できること
- OAuth の実装(1pt)
- 2段階認証の実装(1pt)
- Major: Monitoring system with Prometheus and Grafana.(2pt)
    - Set up Prometheus to collect metrics.
    - Configure exporters and integrations.
    - Create custom Grafana dashboards.
    - Set up alerting rules.
    - Secure access to Grafana.
