#!/usr/bin/env bash
set -euo pipefail

COMPOSE=(docker compose -f compose.yaml -f compose.observability.yaml)
: "${ELASTIC_PASSWORD:?Set ELASTIC_PASSWORD before running this script}"

marker="elk-validation-$(date +%s)-$$"
es_curl=("${COMPOSE[@]}" exec -T elasticsearch curl --fail --silent --show-error
  --cacert /usr/share/elasticsearch/config/certs/http_ca.crt
  -u "elastic:${ELASTIC_PASSWORD}")

echo "Checking Elasticsearch authentication and TLS..."
"${es_curl[@]}" https://localhost:9200/_security/_authenticate >/dev/null

echo "Checking the configured retention policy..."
policy="$("${es_curl[@]}" https://localhost:9200/_ilm/policy/online-workspace-logs)"
grep -q '"min_age":"30d"' <<<"$policy"

echo "Writing a marker to the application log..."
"${COMPOSE[@]}" exec -T backend sh -c \
  "printf '%s INFO  [elk-validation] marker=%s\\n' \"\$(date -Iseconds)\" '$marker' >> /var/log/app/application.log"

echo "Waiting for Logstash to index the marker..."
for _ in $(seq 1 30); do
  result="$("${es_curl[@]}" \
    -G 'https://localhost:9200/online-workspace-*/_search' \
    --data-urlencode "q=message:${marker}" \
    --data-urlencode 'size=1' || true)"
  if grep -q "\"value\"[[:space:]]*:[[:space:]]*1" <<<"$result"; then
    echo "Log forwarding and Elasticsearch search: PASS (marker=$marker)"
    break
  fi
  sleep 2
done

if ! grep -q "\"value\"[[:space:]]*:[[:space:]]*1" <<<"${result:-}"; then
  echo "Log forwarding and Elasticsearch search: FAIL (marker=$marker)" >&2
  exit 1
fi

echo "Checking Kibana..."
curl --fail --silent http://localhost:5601/api/status >/dev/null
echo "Kibana health: PASS"
echo
echo "Open http://localhost:5601, create Data View 'online-workspace-*', and search for:"
echo "  $marker"
