# Android client capability boundary

## Native companion beta

The primary APK workflow builds the native Kotlin + Jetpack Compose client from community draft PR [OpenMausBot #4942](https://github.com/milind-soni/OpenMausBot/pull/4942), pinned to commit `2fd3645c912422be6d1ffdc0d650239a96180cbc`.

The PR author describes QR/deep-link/manual pairing, multiple computers, bot and room lists, chat history, streaming replies over SSE, Markdown, attachments, stopping runs, approval/question cards, per-bot model selection, foreground notifications and optional offline TTS.

This is **not a complete port of the desktop application**. It is a companion client for the server-side agent. Features that depend on the desktop shell, local host OS, a desktop browser window, local machine files, or native computer-control integration may not be exposed in the Android app. The PR is still marked Draft/unmerged as of 2026-11-15. Its author reports build and UI testing against a protocol-equivalent mock on one Android 16 device; real OpenMausBot harness end-to-end compatibility and several media/background paths were not fully verified in that report.

The Android foreground-service model also does not guarantee that Android or an OEM battery manager will keep a network connection alive indefinitely when the app is backgrounded or force-stopped. The server-side agent remains the always-on component; the phone is only a controller/notification client.

## WebView fallback

The separate `mobile/android/` source in this integration pack wraps the existing responsive web UI. It is not the native companion protocol implementation.

Implemented in the wrapper:

- HTTPS server URL entry and persistence in app preferences.
- Existing responsive OpenMausBot web UI displayed in WebView.
- Web session cookies/storage, file upload chooser, download manager, back/reload/change server controls.
- TLS certificate errors are rejected and public cleartext HTTP is blocked.

Not implemented by the WebView wrapper:

- Native QR camera pairing or native companion API/SSE implementation.
- Push notifications after the app is closed or guaranteed background SSE.
- Offline chat, native voice controls, or full local-computer/desktop control.
- A guarantee that every desktop-only panel works on a phone-sized screen.

Use HTTPS for any server reachable outside a private trusted LAN. Never expose the internal OpenMausBot harness port directly to the public internet; use the documented companion/pairing/reverse-proxy deployment path.
