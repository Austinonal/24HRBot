# Android computer selection and AI brain routing

This note describes what the native Android companion can select and what must
remain on the paired OpenMausBot Host.

## Choose a Bot's computer from Android

The per-Bot sheet reads the current Bot row from `GET /api/bots` and writes the
selection through the existing `PATCH /api/bots/{botId}` route. It exposes:

| Android choice | Stored value | Meaning |
| --- | --- | --- |
| Automatic | `null` | Let OpenMausBot choose the computer for the task. |
| This computer · CUA | `local` | Use the computer-control provider owned by the Host. The Host must be online and its CUA permissions must already be granted. |
| Cloud computer | `cloud` | Use the configured remote Boat computer. Cloud provisioning or runtime may incur provider charges. |
| Isolated Local VM | `vm` | Use the Host's isolated Local VM provider. |
| Browser | `browser` | Use the Host's embedded browser surface. |
| Off | `off` | Do not give this Bot a computer surface. |

This is a Bot-level preference, not a VM purchase or a computer wake-up
command. Selecting Cloud does not create an account or approve a bill. The
provider must already be configured on the Host, and any paid provisioning or
wake action must use the Host's existing cost-confirmation flow.

The phone is a controller. It does not embed the CUA driver, start a local
desktop daemon, or make a sleeping personal PC reachable. For the Local CUA
choice, the OpenMausBot Host that owns the desktop must remain online.

## Choose the AI brain from Android

The existing model picker continues to read the Host's available engine/model
catalog from `GET /api/instances`. The Bot can then select a model already
available to that Host. That catalog may include a compatible OpenAI/GPT API
engine, Gemini API engine, other configured providers, or supported local
engines.

Provider setup and secrets remain Host-managed. The companion may read the
available model list and change a Bot's model, but it cannot read or write API
keys, provider credentials, browser cookies, or OAuth session tokens. Add or
remove provider credentials only through the supported Host-side settings and
the provider's own rules.

## Keep the roles separate

- **OpenAI/Gemini API or another configured engine = the brain.** It produces
  the Bot's language-model responses.
- **CUA Driver / computer surface = the hands.** It inspects and operates a
  desktop app or browser on the selected computer. It is not an LLM and does
  not supply GPT or Gemini inference by itself.

A CUA computer-use task may be initiated by a Bot whose brain is an API-backed
model. The model stays the selected engine; CUA supplies computer actions
through the Host's existing permission and approval boundaries.

This integration does not turn a signed-in consumer ChatGPT or Gemini app into
an API endpoint, imitate a person typing prompts into those consumer apps, or
reuse their browser sessions/cookies as a hidden inference bridge. Use the
provider's supported API/engine for model inference. CUA remains available for
ordinary, user-authorized desktop tasks with the project's existing approval
controls.

## Existing implementation surfaces

- Android UI: `scripts/native-android/BotSheet.kt`
- Android template installer: `scripts/patch-native-android.mjs`
- Companion allowlist: `companion/src/routes.ts`
- Bot computer-field validation: `server/routes/bot-patch.ts`
- Host computer providers and lifecycle: `server/routes/computers.ts`
- Host CUA integration decision: `docs/computer-use-integration.md`
