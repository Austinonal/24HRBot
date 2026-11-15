#!/usr/bin/env bash
set -Eeuo pipefail

COMPOSE_DIR="${OMB_COMPOSE_DIR:-/opt/openmausbot}"
COMPOSE_FILE="${OMB_COMPOSE_FILE:-compose.yaml}"
INTERVAL="${OMB_WATCHDOG_INTERVAL:-30}"
FAIL_LIMIT="${OMB_WATCHDOG_FAILURES:-3}"
COOLDOWN="${OMB_WATCHDOG_COOLDOWN:-300}"
STARTUP_GRACE="${OMB_WATCHDOG_STARTUP_GRACE:-300}"

compose() {
  docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/$COMPOSE_FILE" "$@"
}
log() {
  printf '%s openmausbot-watchdog: %s\n' "$(date -Is)" "$*" >&2
}

if [[ ! -f "$COMPOSE_DIR/$COMPOSE_FILE" ]]; then
  log "Compose file missing: $COMPOSE_DIR/$COMPOSE_FILE"
  exit 2
fi

failures=0
last_restart=0

while true; do
  # A consistent backup intentionally stops the container. Do not race it by
  # bringing the service back up while the data volume is being archived.
  if command -v systemctl >/dev/null 2>&1 && systemctl is-active --quiet openmausbot-backup.service; then
    sleep "$INTERVAL"
    continue
  fi

  if ! docker info >/dev/null 2>&1; then
    log "Docker daemon is not reachable; waiting"
    sleep "$INTERVAL"
    continue
  fi

  cid="$(compose ps -a -q omb 2>/dev/null | head -n 1 || true)"
  if [[ -z "$cid" ]]; then
    failures=$((failures + 1))
    log "omb container is missing ($failures/$FAIL_LIMIT)"
    if (( failures >= FAIL_LIMIT )) && (( $(date +%s) - last_restart >= COOLDOWN )); then
      log "recreating omb service"
      compose up -d omb || log "compose up failed; will retry later"
      last_restart="$(date +%s)"
      failures=0
    fi
    sleep "$INTERVAL"
    continue
  fi

  state="$(docker inspect --format '{{.State.Status}}' "$cid" 2>/dev/null || echo unknown)"
  health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$cid" 2>/dev/null || echo unknown)"

  if [[ "$state" == "running" && ( "$health" == "healthy" || "$health" == "none" ) ]]; then
    failures=0
  elif [[ "$state" == "running" && "$health" == "starting" ]]; then
    started="$(docker inspect --format '{{.State.StartedAt}}' "$cid" 2>/dev/null || true)"
    started_epoch=0
    if [[ -n "$started" ]]; then
      started_epoch="$(date -d "$started" +%s 2>/dev/null || echo 0)"
    fi
    if (( started_epoch > 0 && $(date +%s) - started_epoch < STARTUP_GRACE )); then
      failures=0
    else
      failures=$((failures + 1))
    fi
  else
    failures=$((failures + 1))
    log "omb state=$state health=$health ($failures/$FAIL_LIMIT)"
  fi

  if (( failures >= FAIL_LIMIT )) && (( $(date +%s) - last_restart >= COOLDOWN )); then
    log "restarting omb after repeated failed health checks"
    if [[ "$state" == "running" ]]; then
      compose restart -t 30 omb || log "compose restart failed; will try to recreate next time"
    else
      compose up -d omb || log "compose up failed; will retry later"
    fi
    last_restart="$(date +%s)"
    failures=0
  fi

  sleep "$INTERVAL"
done
