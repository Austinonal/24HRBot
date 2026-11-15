#!/usr/bin/env bash
set -Eeuo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 /path/to/openmausbot-data-YYYYMMDDTHHMMSSZ.tar.gz" >&2
  exit 2
fi
ARCHIVE="$(realpath "$1")"
COMPOSE_DIR="${OMB_COMPOSE_DIR:-/opt/openmausbot}"
COMPOSE_FILE="${OMB_COMPOSE_FILE:-compose.yaml}"

compose() {
  docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/$COMPOSE_FILE" "$@"
}

[[ -f "$ARCHIVE" ]] || { echo "Archive not found: $ARCHIVE" >&2; exit 2; }
[[ -f "$COMPOSE_DIR/$COMPOSE_FILE" ]] || { echo "Compose file not found" >&2; exit 2; }

stage="$(mktemp -d)"
stopped=0
finish() {
  rc=$?
  if (( stopped == 1 )); then
    compose start omb >/dev/null 2>&1 || echo "WARNING: failed to restart omb; check docker compose" >&2
  fi
  rm -rf "$stage"
  exit "$rc"
}
trap finish EXIT

tar -xzf "$ARCHIVE" -C "$stage"
[[ -s "$stage/data.tar.gz" ]] || { echo "Archive has no data.tar.gz" >&2; exit 3; }
tar -tzf "$stage/data.tar.gz" >/dev/null

cat <<EOF
WARNING: This overwrites the current OpenMausBot /data volume.
Current Compose configuration and .env will NOT be overwritten.
Restore to a matching or compatible OpenMausBot version.
Archive: $ARCHIVE
EOF
read -r -p 'Type RESTORE to continue: ' answer
[[ "$answer" == "RESTORE" ]] || { echo "Cancelled."; exit 0; }

cid="$(compose ps -a -q omb | head -n 1)"
[[ -n "$cid" ]] || { echo "omb container not found; start the stack once first" >&2; exit 4; }
data_volume="$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/data"}}{{.Name}}{{end}}{{end}}' "$cid")"
[[ -n "$data_volume" ]] || { echo "Could not identify /data volume" >&2; exit 5; }

compose stop -t 60 omb
stopped=1

docker run --rm \
  --mount "type=volume,src=$data_volume,dst=/data" \
  --mount "type=bind,src=$stage,dst=/backup,readonly" \
  alpine:3.21 sh -ec 'tar -xzf /backup/data.tar.gz -C /data'

compose start omb
stopped=0
echo "Restore finished. Check: docker compose ps and docker compose logs omb"
