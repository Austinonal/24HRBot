# 24/7 operations hardening

These scripts assume the upstream Compose project has a service named `omb`, a persistent volume mounted at `/data`, and a Docker healthcheck. They work with either the upstream generated deployment (`compose.yaml`) or source checkout (`deploy/docker-compose.yml`).

## Install

Run from the extension/repository root as root, after bringing the OpenMausBot stack up at least once:

```bash
sudo OMB_COMPOSE_DIR="$PWD/deploy" OMB_COMPOSE_FILE="docker-compose.yml" bash ops/24x7/install.sh
```

To use `/opt/openmausbot/compose.yaml`, omit the environment overrides.

The installer writes only `/etc/openmausbot-24x7.env`, helper scripts in `/usr/local/sbin`, and systemd units. It does not edit OpenMausBot source or rewrite the Compose file.

## Watchdog

`openmausbot-watchdog.service` checks the `omb` container every 30 seconds. Three consecutive unhealthy/exited checks trigger one restart; after a restart there is a 5-minute cooldown. A starting container is given a 5-minute grace period. Logs are available via:

```bash
sudo journalctl -u openmausbot-watchdog -f
```

To stop the watchdog intentionally before maintenance:

```bash
sudo systemctl stop openmausbot-watchdog
```

Then stop/start Compose manually. Re-enable it afterward.

## Backup

A systemd timer starts a backup each day at 03:15 server-local time with a small randomized delay. The backup briefly stops only `omb` so SQLite and attached files are captured as one consistent volume snapshot. Caddy remains running; web requests may briefly receive an unavailable response.

Archives are stored at `/var/backups/openmausbot` by default, with 14-day local retention. Set `OMB_BACKUP_DIR` in `/etc/openmausbot-24x7.env` to use another mounted path. The bundle includes the `/data` volume plus `.env`, Compose YAML and `Caddyfile` when present. Backups may contain credentials and conversations: directory/file permissions are restrictive. Copy them to a separate encrypted destination for disaster recovery.

Manual backup:

```bash
sudo /usr/local/sbin/openmausbot-backup
```

Manual restore (destructive; requires typing `RESTORE`):

```bash
sudo /usr/local/sbin/openmausbot-restore /var/backups/openmausbot/openmausbot-data-YYYYMMDDTHHMMSSZ.tar.gz
```

Restore only to a matching/compatible OpenMausBot version. The script restores `/data` but leaves `.env` and Compose configuration unchanged so a restore cannot silently overwrite your domain or current deployment options.

## Important

- These tools are best-effort recovery, not a high-availability cluster or a 100% uptime guarantee.
- They require root/Docker access. Access to `/var/run/docker.sock` is effectively root-level access on the host.
- The backup pauses the agent briefly; schedule at a quiet time by editing the timer.
- Set server timezone explicitly if you expect 03:15 Asia/Bangkok:

```bash
sudo timedatectl set-timezone Asia/Bangkok
```
