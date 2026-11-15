# Android app: cloud-server client and security notes

## Interface direction

The Android companion uses a Grok-inspired interaction pattern rather than copying Grok branding or assets:

- Dark-first, low-distraction canvas with high-contrast text.
- A short onboarding / pairing screen with one primary QR action and a secondary paste-link path.
- Explicit confirmation of the OpenMausBot Server name and HTTPS address before pairing.
- Minimal message surfaces and a persistent message composer in chat.
- Thai interface copy for controls, status labels, pairing, and approvals. Bot-generated content remains in the language returned by the selected model.

The Android app is a native mobile client, not a local AI runtime. **A Windows desktop .exe is not required on the user's personal PC.** The AI engines, Bot processes, scheduler, API and persistent workspace must still run on an always-on OpenMausBot Server. That server can be deployed on a supported VPS or another always-on Linux host, so the user's own PC can remain off after setup.

For a cloud-first setup, deploy the OpenMausBot Server using [the VPS deployment guide](deploy-vps.md), then pair the Android app with a server-issued QR/deep link that advertises a reachable HTTPS endpoint. The server, not the phone, stores provider sign-ins, Bot state and chat history. The phone keeps only its revocable device token, encrypted with Android Keystore.

The pinned Android client supports direct pairing to a reachable HTTPS server endpoint. **Relay-only pairing QR codes are not yet verified end to end in this Android build.** Until that path is implemented and tested on a physical phone, use a normal HTTPS server address or supported HTTPS tunnel for pairing; do not assume a relay-only QR will work. The Android app itself does not provision or pay for the cloud server.

The **AI accounts** screen reads provider status from the paired OpenMausBot Server and may start, poll, or cancel only the primary
Codex device-code sign-in. It validates the provider URL and short-lived code
before opening or displaying them. The phone cannot read provider credentials,
change engine configuration, sign out, or initiate browser OAuth callbacks.
Claude consumer sign-in is not offered in this screen because the service
requires users to be at least 18; a self-attested checkbox is not an
age-verification method. Google Antigravity account status is displayed when
available, but its browser OAuth callback must be completed on the paired Host,
not through the phone Companion.

## Security controls in this build

- Pairing credentials are exchanged for a revocable device token.
- The local device-token record is encrypted with AES/GCM using a key stored by Android Keystore. Existing plaintext records from older app versions are migrated after a successful parse.
- Android app backup remains disabled.
- The app no longer requests broad photo-library or external-storage permissions. Image attachment uses Android's system Photo Picker, which grants access only to items the user selects.
- Camera access remains for QR scanning. Network access remains for the companion connection. Notifications are used for bot replies and foreground connection status.
- HTTPS is required for remote hosts. Cleartext HTTP is accepted only for explicitly selected private/local addresses, loopback, `.local` names, and Tailscale `.ts.net` routes. API redirects are not followed automatically.
- The app does not request all-files access or a list of every installed app.

## Virus scanning and device scope

This app is not an antivirus product and cannot scan the entire phone. Android's application sandbox intentionally prevents an ordinary app from reading other apps' private data. This build therefore avoids requesting invasive permissions and uses Google Play Protect / the phone's own security app to assess installed packages.

For a system-side check, open Google Play Store → profile icon → **Play Protect** → **Scan**. Keep **Scan apps with Play Protect** enabled. Detection is controlled by Android/Play Protect and cannot be guaranteed by the app itself.

The CI workflow runs source-level checks for excessive permissions, insecure remote HTTP routes, unencrypted device tokens, and obvious embedded credentials. These checks are not a full professional security audit, a malware verdict, or a substitute for Play Protect.

## Important distribution note

The generated artifact is a **debug APK** built and signed with the CI debug signing key. It is useful for personal testing, but it is not a production release package and does not carry a stable release signing identity. A release build should use a private release key managed outside the repository and should undergo independent review before distribution.
