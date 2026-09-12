# ELK 起動・検証手順

この構成では、Elasticsearchが生成するHTTP CAを共有し、LogstashとKibanaが
CA検証付きHTTPSでElasticsearchへ接続します。`ELASTIC_PASSWORD` は必須です。

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
set -a && source .env && set +a
```

`.env` はGit管理対象外です。実際のパスワードやAPIキーをコミットしないでください。

## 2. Composeを起動する

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  up -d
```

`elk-policy` がILMポリシーとインデックステンプレートを登録します。
既存の `elasticsearch_data` volume を使う場合、パスワードは初回作成時の値が
維持されるため、`.env` と一致させてください。

## 3. 自動検証を実行する

以下は認証、TLS、ILMポリシー、アプリケーションログのLogstash転送、
Elasticsearch検索、Kibanaヘルスを順に確認します。

```bash
bash scripts/tests/172/elk_validation.sh
```

## 4. Kibanaでログを検索する

`http://localhost:5601` を開き、ユーザー `elastic` と `ELASTIC_PASSWORD` で
ログインします。DiscoverでData View `online-workspace-*` を作成し、検証スクリプト
が表示した `elk-validation-...` を検索してください。

## 補足

初回設定後は、以下の起動だけで構成を再利用できます。

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  up -d
```
