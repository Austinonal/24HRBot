# Upstream audit (2026-11-15)

## OpenMausBot — primary base

Repository: https://github.com/milind-soni/OpenMausBot

- Default branch: `main`; root `package.json` declares version `0.1.158`, Node `>=24`, `pnpm@10.33.0`.
- License reported by GitHub repository metadata: Apache-2.0.
- `deploy/docker-compose.yml` already defines the server and Caddy as persistent services:
  - `omb` and `caddy` use `restart: unless-stopped`.
  - `data:/data` persists user data; Caddy state uses persistent volumes.
  - `omb` healthcheck polls `http://127.0.0.1:8799/api/health` every 30 seconds.
  - Caddy terminates HTTPS and reaches the server over private Unix sockets.
- `docs/deploy-vps.md` supports running the whole server on a VPS, with 2 CPU, 4 GB RAM minimum, and 20 GB free disk in the documented baseline; 8 GB RAM is suggested for more headroom.
- `docs/routine-schedules.md` says routine scheduling runs inside the OpenMausBot server. If the computer sleeps or the app quits, schedules cannot dispatch. The VPS deployment is the documented route for true always-on execution. It also documents retry/recovery for interrupted unattended runs and a bounded catch-up policy.
- `docs/phone-web-app.md` says the browser UI can be installed on a phone home screen, but it does not add offline chat, push notifications, or native phone computer control.
- The public `main` tree includes a Node `companion/` sidecar and phone docs, but no Android Gradle/Expo source directory was found in that inspected tree. Separately, an independent community Draft PR #4942 adds `apps/android/` (Kotlin + Jetpack Compose) from contributor fork `johnson020202/OpenMausBot`, commit `2fd3645c912422be6d1ffdc0d650239a96180cbc`. As of 2026-11-15 it is open/unmerged. The contributor reports build and mock-device screen checks, while noting real-harness E2E and some background/media paths remain unverified.
- The Android companion's public distribution page and Google Play listing describe it as a remote client requiring an OpenMausBot server/computer. Its capabilities are distinct from this WebView wrapper.

Key references:
- https://github.com/milind-soni/OpenMausBot/blob/main/deploy/docker-compose.yml
- https://github.com/milind-soni/OpenMausBot/blob/main/docs/deploy-vps.md
- https://github.com/milind-soni/OpenMausBot/blob/main/docs/routine-schedules.md
- https://github.com/milind-soni/OpenMausBot/blob/main/docs/phone-web-app.md
- https://github.com/milind-soni/OpenMausBot/blob/main/docs/ios-companion.md
- https://www.openmausbot.com/download

## Sleepless Agent

Repository: https://github.com/context-machine-lab/sleepless-agent

- License: MIT.
- README architecture: messaging interface → SQLite task queue → daemon event loop → agent executor → result manager/workspace/Git.
- Uses isolated workspaces and planner/worker/evaluator roles; includes usage-aware throttling, auto task generation and monitoring.
- This is a useful service-management pattern, but the implementation is tied to Claude Code SDK/CLI and its Slack interface. It should not be dropped wholesale into OpenMausBot's TypeScript harness.

## Auto Deep Researcher 24x7

Repository: https://github.com/Xiangyue-Zhang/auto-deep-researcher-24x7

- License: Apache-2.0.
- Main loop uses THINK → EXECUTE → REFLECT; uses leader/worker dispatch, local/SSH execution backends and periodic monitoring.
- Adds a bounded two-tier memory system, append-only JSONL research/experiment ledger, durable insights/dead-end journals, stagnation checks and optional per-hour cycle caps.
- The project is purpose-built for deep-learning experiments (GPU/SSH/Slurm) rather than a general mobile assistant. Only its reliability/memory patterns are applicable to the requested general research bot.

## AionUI

Repository: https://github.com/iOfficeAI/AionUi

- License: Apache-2.0.
- Root contains a separate `mobile/` project using Expo/React Native, camera and secure storage libraries.
- It is its own agent/client runtime; its mobile screens are not a drop-in OpenMausBot companion. Reusing it properly would require implementing and testing the OpenMausBot API/SSE/pairing protocol in a new mobile client.

## Grok Bot design principle

OpenMausBot's own README describes the intended interaction pattern as a roster of bots, each with its own personality, model, thread memory, computer and connected apps. The base is already presented as an independent open-source alternative inspired by Grok Bot; it is not affiliated with xAI. The extension therefore preserves that bot-centric design rather than copying proprietary Grok code.

## What this ZIP changes

This extension does **not** include code copied from Sleepless Agent, Deep Researcher or AionUI. It implements operations scripts, an independently authored mobile WebView fallback and research prompt/templates. Its Android build workflow additionally checks out the separately developed community Android PR #4942 at a pinned commit and builds its APK; the Android source is fetched by the workflow rather than bundled in this ZIP. Review the contributor source and preserve the applicable Apache-2.0 notices before redistribution. Original OpenMausBot LICENSE/NOTICE and third-party notices must remain with a derived fork.
