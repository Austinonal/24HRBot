# Target architecture

```text
                     phone / laptop browser
                              |
                  HTTPS + OpenMausBot pairing
                              |
                         Caddy :443
                              |
                 OpenMausBot server container
                 - harness / HTTP / SSE
                 - bots and model drivers
                 - routines / automation scheduler
                 - approvals and notifications
                 - SQLite + persistent /data volume
                              |
               +--------------+--------------+
               |                             |
       research bot workspace        cloud computer backend
       - PROJECT_BRIEF.md            - Boat cloud desktop, or
       - MEMORY_LOG.md               - self-hosted VPS Docker VM
       - RESEARCH_LOG.md
       - SOURCES.jsonl
       - INSIGHTS.md
       - DEAD_ENDS.md

Host-side watchdog ---> Docker health state ---> restart unhealthy service
Host-side backup timer ---> stop OMB briefly ---> archive /data ---> start OMB
```

## Always-on control plane vs. on-demand computer

The control plane (OpenMausBot server, routine scheduler, queue, chat history and credentials) must live on a VPS/always-on server. The bot's computer can be a cloud desktop or a self-hosted container. It should be woken only when an automation actually needs browser/GUI control. A desktop-hour allowance is not the same as backend-server uptime.

## Recovery boundaries

- Docker `restart: unless-stopped` restarts a process that exits.
- The upstream Compose healthcheck detects an unhealthy app endpoint.
- This pack's watchdog watches Docker health and restarts `omb` after repeated unhealthy checks.
- Upstream routine recovery may retry a failed unattended run and recover an interrupted run after server restart.
- None of these mechanisms repairs a failed VPS host, broken DNS, expired domain, provider outage, exhausted AI account, or exhausted monthly quotas. For those, use provider monitoring and an off-host backup.

## Persistent storage

OpenMausBot's existing named volume `/data` is the authoritative app storage. It should not be removed during updates. The research archive belongs inside a bot's persistent working folder (not an ephemeral local phone cache). This pack backs up the `/data` volume and the deployment config.
