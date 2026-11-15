# Android client build

## Native Android client (recommended)

The integration workflow `.github/workflows/build-openmausbot-android.yml` builds a native Kotlin + Jetpack Compose companion APK from the community draft PR [OpenMausBot #4942](https://github.com/milind-soni/OpenMausBot/pull/4942), pinned to commit `2fd3645c912422be6d1ffdc0d650239a96180cbc` in `johnson020202/OpenMausBot`.

It uses the upstream companion sidecar protocol (HTTP + SSE), not the internal harness API. The PR describes QR/deep-link/manual pairing, multi-computer support, bot/room lists, streaming chat, Markdown, attachments, approvals, questions, per-bot model selection, notifications and optional offline TTS.

**Status and caution:** as of 2026-11-15 the PR is still open and marked Draft. Its author reports a successful debug build and screen testing against a protocol-equivalent mock on a Xiaomi 15 Pro / Android 16, but explicitly says real-harness end-to-end verification and several background/media paths have not been fully verified. This integration pack does not claim otherwise. Review the PR, its source, permissions and license before distributing the APK.

### Build it

1. Push this integration pack into your OpenMausBot fork.
2. Open **Actions → Build OpenMausBot Native Android Companion → Run workflow**.
3. After the job succeeds, open the run and download artifact `OpenMausBot-Native-Android-debug`.
4. Install the debug APK on a test Android device and pair it with your own server.

The workflow checks out only `apps/android` from the exact pinned commit and runs its Gradle unit tests plus `assembleDebug`. It does not modify OpenMausBot's engine or publish a release to Google Play.

## Lightweight WebView fallback

`mobile/android/` is an independently authored fallback client that wraps the responsive OpenMausBot web UI. It is not the native companion and has fewer capabilities. See `docs/MOBILE_LIMITATIONS.md`.
