# Oumatjie

Oumatjie is an accessibility-first Android email client for people who find conventional email apps confusing or difficult to use.

The MVP supports:

- Google account authorization without collecting a Gmail password
- A simplified inbox and message reader
- Large, clearly labelled controls with no required gestures
- Gmail attachment download and in-app PDF viewing, including password-protected PDFs
- Safe actions such as mark read and move to Trash, both with plain-language confirmation
- An in-app text size setting, on top of (not instead of) the system font scale
- Atkinson Hyperlegible typography throughout, a font designed for low-vision and aging readers
- A calm, dismissible note the first time you ever receive mail from a given address
- Read-aloud for any message, using the device's built-in text-to-speech — works fully offline
- Optional, off-by-default AI features (your own Anthropic API key): a calm scam/phishing check
  on messages you open, and one-tap summarization — see
  [docs/AI_ASSISTANT.md](docs/AI_ASSISTANT.md) for exactly what's sent and when

Every screen above is implemented against the real Gmail API. Until a Google Cloud project is
configured (see [docs/SETUP.md](docs/SETUP.md)), the app runs entirely on an offline demo
inbox — "Continue with Google" fails gracefully and explains that, while "Try the demo inbox"
always works, including opening a real sample PDF end to end. All of this — the demo inbox,
attachment download, the PDF viewer, mark-as-read, Trash with confirmation, text size scaling,
rotation, and the Google sign-in cancellation path — has been run on a real emulator, not just
compiled; docs/DECISIONS.md has the detail, including two real crashes that pass only caught
because of that. Reading real Gmail through `data/gmail/` has not, since no Google Cloud
project has been created yet — see docs/SETUP.md §4 for exactly what that leaves unverified.

## Architecture

- `auth/` — `AuthManager` requests Gmail scopes through Google Play services' Authorization
  API. It never sees a password; the app is identified by package name and signing
  certificate, not an embedded client ID.
- `data/` — `MailRepository` is the app's mail contract. `MockMailRepository` backs the demo
  inbox; `data/gmail/GmailMailRepository` backs a real account, translating the Gmail REST API
  into Oumatjie's own models (`data/gmail/MimePayloadParsing.kt` walks the MIME tree). Both
  implementations share `data/attachments/` for downloading and caching documents.
  `data/settings/SettingsRepository` persists the text size choice, AI-feature toggle, and API
  key with DataStore. `data/senders/KnownSendersRepository` remembers which sender addresses
  have ever been seen, for first-contact flagging.
- `session/` — `SessionViewModel` owns whether the user is signed in, in the demo, or signed
  out, and drives which repository the rest of the app is given. `session/SessionRepository`
  remembers *that* the user signed in before (never a token), enabling a silent re-authorize on
  cold start.
- `ai/` — `AiProvider` is the provider-agnostic interface for scam checking and summarization.
  `AnthropicAiProvider` calls Claude Haiku 4.5 with the user's own API key; `DemoAiProvider` is
  an offline fallback used only for summarization, never for the scam check (see
  docs/DECISIONS.md for why). See [docs/AI_ASSISTANT.md](docs/AI_ASSISTANT.md) for the full
  design, including what's implemented and what's still a spec.
- `ui/` — one package per screen (`signin/`, `mail/`, `settings/`), a shared
  `ui/components/` set of large-touch-target building blocks, and `ui/navigation/` wiring them
  together with Navigation Compose. `ui/GranifyApp.kt` is the composition root.
- `pdf/` — `PdfViewerActivity` hosts androidx.pdf's `PdfViewerFragment` (a classic
  View/Fragment screen, not Compose) behind a small, replaceable boundary. The fragment shows
  its own password prompt with retry; Oumatjie only reacts to the final success/error result.
- `di/AppContainer.kt` — plain hand-written singletons (no DI framework); built once in
  `OumatjieApplication`.

## Technology

- Kotlin, Jetpack Compose, and Navigation Compose
- Android Gradle Plugin 9.2 with built-in Kotlin
- Minimum Android 9 (API 28)
- Compile SDK 37; target SDK 36
- Retrofit + kotlinx.serialization for the Gmail REST API
- Google Play services Authorization API for Gmail access (no password, no embedded client ID)
- AndroidX PDF viewer behind a replaceable viewer boundary
- DataStore Preferences for the text size setting

## Open the project

1. Install the latest stable Android Studio.
2. Open this repository as an existing project.
3. Allow Android Studio to install SDK Platform 37, SDK Platform 36, and Build Tools 36 when prompted.
4. Select Android Studio's bundled JDK for Gradle; the project compiles to Java 17 compatibility.
5. Run the `app` configuration on an API 28+ emulator or Android phone.

Do not create or commit `local.properties`, signing keys, OAuth secrets, access tokens, or downloaded email data.

See [docs/SETUP.md](docs/SETUP.md) for the full workstation and Google Cloud checklist.

## Documentation

- [docs/PRODUCT_PRINCIPLES.md](docs/PRODUCT_PRINCIPLES.md) — who this app is for and the
  interaction/privacy rules every feature is expected to follow. Read this first; the other
  documents all build on it.
- [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md) — the visual language: color, type, shape,
  spacing, motion, and haptics as design tokens, plus the shared components built on them. Read
  this before adding any new screen or UI component.
- [docs/SETUP.md](docs/SETUP.md) — workstation setup and the Google Cloud checklist needed to
  test against a real Gmail account.
- [docs/DECISIONS.md](docs/DECISIONS.md) — the tactical log: concrete decisions, hurdles hit
  while building, and known gaps, each with its reasoning. Start here to understand why the
  code looks the way it does.
- [docs/ROADMAP.md](docs/ROADMAP.md) — the strategic log: a retrospective on the project so
  far, UI/UX/motion design direction, and the research behind it. Start here to understand
  where the project is headed next.
- [docs/AI_ASSISTANT.md](docs/AI_ASSISTANT.md) — full design specification for the AI assistant
  features. Scam warnings, summarization, first-contact flagging, and read-aloud are
  implemented; calendar awareness, notifications, categorization, and a chat panel are still
  design only.
- [docs/NEEDS_YOUR_INPUT.md](docs/NEEDS_YOUR_INPUT.md) — a queue of points where work paused
  because it genuinely needs the project owner's decision, credentials, or account access.
- [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) — a drafted privacy policy, accurate to this
  codebase's actual behavior, prepared for publication at oumatjie.com. Needs a legal review
  before it's published — see the drafting note at the end of that file.
- [docs/PLAY_STORE_READINESS.md](docs/PLAY_STORE_READINESS.md) — a concrete checklist for what's
  already satisfied, what's account/paperwork-only, and what's still a real engineering gap
  before this can go on the Play Store.
- [AGENTS.md](AGENTS.md) — for whoever is doing hands-on work here: machine/tooling setup,
  standing working preferences, and the session-start/session-end documentation ritual.
- [HANDOFF.md](HANDOFF.md) — start here after a break from this project. Written for a new
  chat session (human or Claude) to get oriented in one read.
