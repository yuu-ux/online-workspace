# ELK 起動・検証手順

この構成では、Elasticsearchが生成するHTTP CAを共有し、LogstashとKibanaが
CA検証付きHTTPSでElasticsearchへ接続します。`ELASTIC_PASSWORD` は必須です。
Elasticsearchの自動セキュリティ設定は起動時に `config/certs` を作り直すため、
証明書だけではなく設定ディレクトリ全体を名前付きvolumeとして共有します。
`http.publish_host` にComposeのサービス名を指定し、クライアント側ではホスト名検証も行います。

ログは `online-workspace-*` に保存され、ILMで30日後に削除されます。30日を超えて
保存する必要がある場合は、削除前にElasticsearch Snapshot Repositoryへ
スナップショットを取得する運用を別途用意してください。このCompose設定は
外部オブジェクトストレージへのアーカイブを自動では行いません。

## 1. 環境変数を設定する

プロジェクトルートで `.env.example` を `.env` にコピーし、コメントを確認しながら
すべての `change-this-*` を変更します。

```bash
cp .env.example .env
# .envを編集して、パスワードとAPIキーを変更する
```

`.env` はGit管理対象外です。実際のパスワードやAPIキーをコミットしないでください。

## 2. Composeを起動する

`compose.yaml` が監視サービスの構成も自動的に取り込むため、次の1コマンドで
アプリケーション、Elasticsearch、Logstash、Kibana、Prometheus、Grafanaを起動できます。

```bash
docker compose up -d
```

`elk-policy` がILMポリシーとインデックステンプレートを登録します。
`kibana-setup` が `elastic/kibana` のサービスアカウントトークンを自動発行し、
Elasticsearchの設定volumeに保存します。Kibanaは初期化成功後にそのトークンで
接続し、再起動時も再利用します。トークンの手動発行や `.env` への追記は不要です。
`elk-policy` と `kibana-setup` は正常完了後に終了する初期化用サービスです。
Logstashの読み取り位置はデフォルトのsincedbで管理し、`logstash_data` volumeに
永続化します。保存先ディレクトリはLogstashが自動作成します。
Logstashは `elk-policy` の正常完了を待って起動し、最初のログからILM設定を適用します。
既存の `elasticsearch_data` volume を使う場合、パスワードは初回作成時の値が
維持されるため、`.env` と一致させてください。

## 3. 自動検証を実行する

以下は認証、TLS、ILMポリシー、アプリケーションログのLogstash転送、
Elasticsearch検索、Kibanaヘルスを順に確認します。

```bash
set -a && source .env && set +a
bash scripts/tests/172/elk_validation.sh
```

## 4. Kibanaでログを検索する

`http://localhost:5601` を開き、ユーザー `elastic` と `ELASTIC_PASSWORD` で
ログインします。DiscoverでData View `online-workspace-*` を作成し、検証スクリプト
が表示した `elk-validation-...` を検索してください。

## 補足

初回設定後は、以下の起動だけで構成を再利用できます。

```bash
docker compose up -d
```
