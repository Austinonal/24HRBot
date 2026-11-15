# Android companion: UI and security notes

## Interface direction

The Android companion uses a Grok-inspired interaction pattern rather than copying Grok branding or assets:

- Dark-first, low-distraction canvas with high-contrast text.
- A short onboarding / pairing screen with one primary QR action and a secondary paste-link path.
- Explicit confirmation of the computer name and address before pairing.
- Minimal message surfaces and a persistent message composer in chat.
- Thai interface copy for controls, status labels, pairing, and approvals. Bot-generated content remains in the language returned by the selected model.

The app still requires a pairing step because it is a companion for a desktop OpenMausBot instance; it is not a standalone AI service. After a successful pairing, the app opens the bot list and chat experience.

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
