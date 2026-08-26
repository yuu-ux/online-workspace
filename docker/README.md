# ELK 起動・ログ確認手順

## 1. コンテナを起動する

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  up -d
```

## 2. Elasticsearch のパスワードを確認・設定する

Elasticsearch 初回起動時に、`elastic` ユーザーのパスワードがログへ出力されます。

まず、Elasticsearch のログからパスワードを確認します。

```bash
docker compose \
  -f compose.observability.yaml \
  logs elasticsearch
```

パスワードが確認できない場合は、以下のコマンドで `elastic` ユーザーのパスワードを再設定します。

```bash
docker compose \
  -f compose.observability.yaml \
  exec elasticsearch \
  bin/elasticsearch-reset-password -u elastic
```

表示されたパスワードを `.env` に設定します。

```env
ELASTIC_PASSWORD=<elastic ユーザーのパスワード>
```

## 3. `.env` を環境変数として読み込む

```bash
set -a && source .env && set +a
```

読み込めていることを確認します。

```bash
echo "$ELASTIC_PASSWORD"
```

## 4. Logstash を再起動する

Logstash は `ELASTIC_PASSWORD` を使用して Elasticsearch に接続するため、パスワード設定後に Logstash を再作成します。

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  up -d --force-recreate logstash
```

## 5. Elasticsearch にログが保存されていることを確認する

```bash
curl -k \
  -u elastic:"$ELASTIC_PASSWORD" \
  'https://localhost:9200/_cat/indices?v'
```

以下のような `online-workspace-*` の index が存在すれば、ログが Elasticsearch に保存されています。

```text
online-workspace-2026.08.20
```

## 6. Kibana の初期設定を行う

Kibana が未設定の場合のみ、enrollment token を生成します。

```bash
docker compose \
  -f compose.observability.yaml \
  exec elasticsearch \
  bin/elasticsearch-create-enrollment-token --scope kibana
```

生成された token を Kibana の初期設定画面に入力します。

```text
http://localhost:5601
```

## 7. Kibana にログインする

```text
ユーザー名: elastic
パスワード: .env の ELASTIC_PASSWORD
```

## 8. Kibana でログを確認する

Kibana の **Discover** を開きます。

Data View が未作成の場合は、以下のパターンで作成します。

```text
online-workspace-*
```

作成後、Discover からアプリケーションログを確認できます。

## 補足

`elasticsearch_data` volume が残っている場合、Elasticsearch のパスワードや Kibana の初期設定は基本的に保持されます。

その場合、次回以降は以下のコマンドで起動するだけで確認できます。

```bash
docker compose \
  -f compose.yaml \
  -f compose.observability.yaml \
  up -d
```

Kibana:

```text
http://localhost:5601
```
