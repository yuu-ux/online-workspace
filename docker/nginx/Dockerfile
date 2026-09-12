FROM ghcr.io/gleam-lang/gleam:v1.17.0-node-alpine AS frontend-build

WORKDIR /app

RUN apk add --no-cache erlang erlang-dev rebar3

COPY frontend/gleam.toml frontend/manifest.toml frontend/package*.json ./

RUN chown -R node:node /app

USER node

RUN npm ci
RUN gleam deps download

COPY --chown=node:node frontend/ ./

RUN npm run build

FROM nginx:1.27-alpine

RUN apk add --no-cache openssl

ENV CERT_DIR=/etc/nginx/tls

COPY --from=frontend-build /app/dist /var/www/static
COPY docker/nginx/generate-local-tls.sh /docker-entrypoint.d/05-generate-local-tls.sh
