#!/usr/bin/env bash
set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run with sudo/root." >&2
  exit 1
fi

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_DIR="${OMB_COMPOSE_DIR:-/opt/openmausbot}"
COMPOSE_FILE="${OMB_COMPOSE_FILE:-compose.yaml}"
BACKUP_DIR="${OMB_BACKUP_DIR:-/var/backups/openmausbot}"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker CLI not found. Install Docker Engine and Compose plugin first." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose plugin not found." >&2
  exit 1
fi
if [[ ! -f "$COMPOSE_DIR/$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_DIR/$COMPOSE_FILE" >&2
  echo "For source checkout use OMB_COMPOSE_DIR=/path/to/OpenMausBot/deploy and OMB_COMPOSE_FILE=docker-compose.yml" >&2
  exit 1
fi

docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/$COMPOSE_FILE" config -q

install -D -m 0755 "$SCRIPT_DIR/watchdog.sh" /usr/local/sbin/openmausbot-watchdog
install -D -m 0755 "$SCRIPT_DIR/backup.sh" /usr/local/sbin/openmausbot-backup
install -D -m 0755 "$SCRIPT_DIR/restore.sh" /usr/local/sbin/openmausbot-restore
install -d -m 0700 "$BACKUP_DIR"

cat > /etc/openmausbot-24x7.env <<EOF
OMB_COMPOSE_DIR=$COMPOSE_DIR
OMB_COMPOSE_FILE=$COMPOSE_FILE
OMB_BACKUP_DIR=$BACKUP_DIR
OMB_WATCHDOG_INTERVAL=30
OMB_WATCHDOG_FAILURES=3
OMB_WATCHDOG_COOLDOWN=300
OMB_WATCHDOG_STARTUP_GRACE=300
EOF
chmod 0600 /etc/openmausbot-24x7.env

install -D -m 0644 "$SCRIPT_DIR/systemd/openmausbot-watchdog.service" /etc/systemd/system/openmausbot-watchdog.service
install -D -m 0644 "$SCRIPT_DIR/systemd/openmausbot-backup.service" /etc/systemd/system/openmausbot-backup.service
install -D -m 0644 "$SCRIPT_DIR/systemd/openmausbot-backup.timer" /etc/systemd/system/openmausbot-backup.timer

systemctl daemon-reload
systemctl enable --now openmausbot-watchdog.service
systemctl enable --now openmausbot-backup.timer

echo "Installed. Check: systemctl status openmausbot-watchdog && systemctl list-timers openmausbot-backup.timer"
