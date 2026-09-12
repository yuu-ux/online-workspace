FROM nginx:1.27-alpine

RUN apk add --no-cache openssl

ENV CERT_DIR=/etc/nginx/tls

COPY generate-local-tls.sh /docker-entrypoint.d/05-generate-local-tls.sh
