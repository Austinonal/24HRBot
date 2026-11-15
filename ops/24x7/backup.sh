#!/usr/bin/env bash
set -Eeuo pipefail

COMPOSE_DIR="${OMB_COMPOSE_DIR:-/opt/openmausbot}"
COMPOSE_FILE="${OMB_COMPOSE_FILE:-compose.yaml}"
BACKUP_DIR="${OMB_BACKUP_DIR:-/var/backups/openmausbot}"

compose() {
  docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/$COMPOSE_FILE" "$@"
}

[[ -f "$COMPOSE_DIR/$COMPOSE_FILE" ]] || { echo "Compose file not found" >&2; exit 2; }
command -v docker >/dev/null 2>&1 || { echo "Docker not found" >&2; exit 2; }
mkdir -p "$BACKUP_DIR"
chmod 0700 "$BACKUP_DIR"

cid="$(compose ps -a -q omb | head -n 1)"
[[ -n "$cid" ]] || { echo "omb container not found; start the stack first" >&2; exit 3; }
data_volume="$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}' "$cid")"
[[ -n "$data_volume" ]] || { echo "Could not identify the persistent /data volume" >&2; exit 4; }

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
stage="$(mktemp -d)"
out="$BACKUP_DIR/openmausbot-data-$stamp.tar.gz"
stopped=0

finish() {
  rc=$?
  if (( stopped == 1 )); then
    compose start omb >/dev/null 2>&1 || echo "WARNING: failed to start omb after backup; check docker compose" >&2
  fi
  rm -rf "$stage"
  exit "$rc"
}
trap finish EXIT

mkdir -p "$stage/config"
cp "$COMPOSE_DIR/$COMPOSE_FILE" "$stage/config/compose.yml"
[[ -f "$COMPOSE_DIR/.env" ]] && cp "$COMPOSE_DIR/.env" "$stage/config/.env" || true
[[ -f "$COMPOSE_DIR/Caddyfile" ]] && cp "$COMPOSE_DIR/Caddyfile" "$stage/config/Caddyfile" || true
chmod 0600 "$stage/config/"* 2>/dev/null || true

# A stopped container makes the volume archive internally consistent, including SQLite/WAL files.
compose stop -t 60 omb
stopped=1

docker run --rm \
  --mount "type=volume,src=$data_volume,dst=/data,readonly" \
  --mount "type=bind,src=$stage,dst=/backup" \
  alpine:3.21 sh -ec 'tar -czf /backup/data.tar.gz -C /data .'

cat > "$stage/manifest.txt" <<EOF
created_utc=$stamp
data_volume=$data_volume
compose_dir=$COMPOSE_DIR
compose_file=$COMPOSE_FILE
note=Restore data.tar.gz to the matching /data volume; config files are included for reference only.
EOF

tar -czf "$out" -C "$stage" .
chmod 0600 "$out"
sha256sum "$out" > "$out.sha256"
chmod 0600 "$out.sha256"

compose start omb
stopped=0

find "$BACKUP_DIR" -maxdepth 1 -type f \( -name 'openmausbot-data-*.tar.gz' -o -name 'openmausbot-data-*.tar.gz.sha256' \) -mtime +14 -delete

echo "Backup created: $out"
