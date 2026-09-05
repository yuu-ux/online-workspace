#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cert_dir="$repo_root/docker/nginx/certs"

mkdir -p "$cert_dir"
openssl req -x509 -newkey rsa:2048 -sha256 -nodes -days 30 \
	-keyout "$cert_dir/localhost.key" \
	-out "$cert_dir/localhost.crt" \
	-subj "/CN=localhost" \
	-addext "subjectAltName=DNS:localhost,IP:127.0.0.1"
chmod 600 "$cert_dir/localhost.key"

echo "Created $cert_dir/localhost.crt and localhost.key"
