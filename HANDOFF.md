# Handoff

Read this file first, in full, before touching anything else — it's written to get a new chat
session (human or Claude) fully oriented in one read. It links out to the files that carry the
real depth; this file itself is the map, not the territory. See also
[`AGENTS.md`](AGENTS.md) — this file covers *where the project stands*; AGENTS.md covers *how
to work on it* (machine setup, standing preferences, the documentation-update ritual). Read
both; they're deliberately kept separate rather than merged.

## What Oumatjie is

An accessibility-first Android email client for people who find conventional email apps
confusing or difficult to use — older adults specifically, but built on the principle (see
[`docs/PRODUCT_PRINCIPLES.md`](docs/PRODUCT_PRINCIPLES.md)) that age is not a single disability
or a single user profile. Native Kotlin + Jetpack Compose, built against the real Gmail REST
API with a full offline demo inbox as a fallback and teaching tool. Not a wrapper, not a
web view — a real, from-scratch native app.

## Read these next, in this order

1. [`docs/PRODUCT_PRINCIPLES.md`](docs/PRODUCT_PRINCIPLES.md) — short. The constraints
   everything else answers to. Skipping this is how you end up re-deciding something already
   settled.
2. [`docs/DECISIONS.md`](docs/DECISIONS.md) — the "Verification summary" section at the top
   tells you exactly what's actually been proven to work, as opposed to what merely compiles.
   The rest is a dated decision/hurdle/gap log — skim headings, read what's relevant to what
   you're about to do.
3. [`docs/ROADMAP.md`](docs/ROADMAP.md) — a retrospective on the project so far, then design
   direction for what's next (typography, button hierarchy, motion/haptics/sound, visual
   identity), then the research behind it. Read the "Design direction: final plan" section
   closely — it's the authoritative, reconciled spec; the sections above it in the file are the
   reasoning trail that produced it, kept visible on purpose rather than deleted.
4. [`docs/AI_ASSISTANT.md`](docs/AI_ASSISTANT.md) — full design spec for the AI-assistant
   features, including *why* the build order is scam-detection-first rather than
   categorization-first (it's not what the user originally asked for first — the reasoning for
   the change is in there and in ROADMAP.md's research section).
5. [`docs/DESIGN_SYSTEM.md`](docs/DESIGN_SYSTEM.md) — color, type, shape, spacing, motion, and
   haptics as design tokens, plus every shared UI component and when to use it. Read before
   adding any new screen or component.
6. [`docs/SETUP.md`](docs/SETUP.md) — workstation and Google Cloud setup, only needed once
   you're actually building or testing against a real Gmail account.
7. [`docs/PRIVACY_POLICY.md`](docs/PRIVACY_POLICY.md) and
   [`docs/PLAY_STORE_READINESS.md`](docs/PLAY_STORE_READINESS.md) — only needed once Play Store
   submission is actually in progress; both are real drafts, not placeholders, but the privacy
   policy specifically needs a legal review before publishing (see its own drafting note).
8. [`docs/TOOLING.md`](docs/TOOLING.md) — Graphify and repo-hygiene tooling (Dependabot, gitleaks,
   detekt/ktlint, CodeQL), researched 2026-08-25. Worth reading early if you're a local/Claude
   Code session — the project owner asked for this to be set up early, and it needed real network
   access this project hasn't had until now. Has a domain warning worth reading before installing
   anything (`graphify.net` ≠ the real project).

## Current state, as of 2026-08-25 (four sessions total — see below)

**The repo is now live on GitHub, with CI and an autonomous GitHub Action, both working** —
`https://github.com/ReubenMiddleton/oumatjie` (private). This is the single biggest state change
in the project's history and supersedes several "not yet done" notes further down this file that
predate it:
- `.github/workflows/ci.yml` runs unit tests + a debug build on every push/PR.
- `.github/workflows/claude.yml` responds to `@claude` mentions in issues/PRs, authenticated via
  a subscription token (`CLAUDE_CODE_OAUTH_TOKEN`), not a metered API key.
- `.github/workflows/claude-ci-watch.yml` checks CI health once daily and opens a fix PR (never
  auto-merges) if something's broken — meant specifically for stretches with nobody watching.
- **The very first real CI run immediately found and this session fixed a genuine bug**:
  `SessionViewModel` was calling `AuthManager.authorize()` twice per sign-in instead of once
  (`GmailMailRepository.fetchAccountEmail()` was silently re-deriving its own token instead of
  reusing the one already granted). Two full sessions of careful hand-review missed this; the
  first real compiler run caught it immediately. See DECISIONS.md's "First real CI run found a
  real bug hand-review missed" — read it, it's the clearest evidence yet for why every
  "hand-verified, not compiled" caveat in this file matters and shouldn't be treated as probably-fine.
- **`CLAUDE.md`** (repo root, new) now exists specifically so Claude Code and the GitHub Actions
  above don't have to rediscover this file from scratch — it's a short pointer to this file, to
  AGENTS.md's standing rules, and to a prioritized list of things only a local session (with a
  real compiler, shell, and network) can do that no session before it could. Read it if you're
  Claude Code picking this project up locally for the first time.
- Full setup reasoning — why direct git push from a cloud sandbox wasn't possible, why a
  subscription token instead of a metered key, why PRs instead of auto-merge, why the scheduled
  workflow deliberately avoids the default `GITHUB_TOKEN` — is in DECISIONS.md's "CI/CD and
  autonomous GitHub Action set up" entry. NEEDS_YOUR_INPUT.md's matching entry is now Resolved.

**A later 2026-08-25 session** researched Graphify (`graphify.net`, per the project owner's
request) and complementary repo-hygiene tooling, at the project owner's explicit request, to be
handed off for a local/Claude Code session to actually install — this cloud sandbox has no PyPI
access, confirmed directly, so nothing below could be installed here. **Important finding**: the
domain the request pointed at, `graphify.net`, is not the official project — the real project's
own site (`graphify.com`) publishes a page stating `graphify.net` is unaffiliated and not an
official source. Full writeup, an honest assessment of the (real, YC-backed, but very new)
official project, and a prioritized list of complementary tools (Dependabot, gitleaks,
detekt/ktlint, CodeQL) is in the new `docs/TOOLING.md` — see DECISIONS.md's "Graphify and
repo-hygiene tooling researched" entry for the short version.

**Same session, in response to "any downside to making the repo public now":** found and fixed a
real gap in `claude.yml` — its `@claude`-mention trigger had no check on who left the mention,
which is moot on a private repo but not on a public one (any GitHub account could have triggered
it, running with write access, authenticated as the owner's own subscription token). Now gated to
`github.actor == github.repository_owner`. No other downside found — a credential grep of the
current tree came up clean, and the only other nuance (an unlicensed public repo is visible but
not legally reusable) is the already-tracked LICENSE decision, not a new one. See DECISIONS.md's
"`claude.yml` gated to the repo owner only" entry.

**A 2026-08-25 session** worked autonomously on self-selected, no-input-needed follow-ups while
the project owner was away (explicit instruction: "do as much as possible without my input").
Four things, same verification caveat as everything else on this list — hand-reviewed, **not run
through a compiler**:

1. **A static accessibility (TalkBack) audit** — read every screen's Compose source looking for
   structural issues a real TalkBack pass would catch. Found and fixed two real gaps: unread mail
   was signalled by card background color alone (a WCAG 1.4.1 "Use of Color" violation, invisible
   to TalkBack and unreliable for low-vision/colorblind readers) — fixed with an explicit "Unread"
   text label; and there was no heading navigation anywhere, so a TalkBack user had no way to jump
   between sections — fixed with `Modifier.semantics { heading() }` on every screen's headings.
   Confirmed two other suspected issues were already fine (Card semantics merging, no bare icons
   anywhere). This narrows what a real TalkBack pass would find — it doesn't replace one. See
   DECISIONS.md's "Static accessibility audit" entry.
2. **Tier 1 (no-AI) mail categorization built** — `data/categories/MailCategory.kt` and
   `CategoryAssigner.kt`, a small fixed starter set (Bills, Receipts, Newsletters, Family)
   assigned by local keyword rules over subject/preview text, displayed as a label on each mail
   card. `Family` is deliberately never auto-assigned (no reliable text signal exists for it —
   see DECISIONS.md). The category model deliberately separates a stable `id` from an editable
   `label` so a future rename/merge screen — the one part of AI_ASSISTANT.md's Tier 1 spec still
   not built — is a UI-only addition later, not a data migration. 7 new unit tests plus 2 more in
   `MailViewModelTest.kt`.
3. **Resolved the open "sound as a feedback channel" question** flagged in ROADMAP.md's
   retrospective. Researched and decided: don't add sound this pass — haptics plus visible text
   confirmations already cover every feedback moment, and a real implementation risk surfaced
   during research (Android's UI sound-effect stream types don't automatically respect the
   device's silent/vibrate mode; an app has to check `AudioManager.getRingerMode()` itself). See
   DECISIONS.md's "Sound as a feedback channel" entry and DESIGN_SYSTEM.md's Haptics section
   (which now documents the three requirements sound would have to meet if it's ever added).
4. **Prepared, not decided, a LICENSE comparison** (`docs/LICENSE_COMPARISON.md`, new) — a
   factual, non-recommending MIT-vs-Apache-2.0 comparison for the project owner's own eventual
   choice, per NEEDS_YOUR_INPUT.md's "ask, don't guess" framing on that item.

**A second 2026-08-24 session** did four things, all hand-verified but — same caveat
as everything else on this list — **not run through a compiler**:

1. **Renamed the project from Granify to Oumatjie**, end to end — the domain oumatjie.com is
   real and owned by the project owner. `applicationId` is now `com.oumatjie.app`; the Kotlin
   package/namespace deliberately stays `com.granify.app` (a few `.kt` file *basenames* like
   `GranifyApplication.kt` stay too, containing renamed classes like `OumatjieApplication` — see
   DECISIONS.md's rename entry for the full reasoning on both). Every identifier, string,
   resource name, and doc mention was renamed; one test fixture
   (`MimePayloadParsingTest.kt`'s base64 constant) needed a hand-computed fix a blind rename
   would have silently broken. **A real, non-trivial hurdle happened and was fully recovered
   from mid-session** — a device-bridge caching issue silently reverted several already-edited
   files back to stale content partway through the rename. Caught, diagnosed, and fixed before
   anything was written back to the real device; see DECISIONS.md's "Stale device-bridge upload
   cache" entry for the full story and the lesson for future sessions working this way.
2. **Formalized a design system** (`docs/DESIGN_SYSTEM.md`, new) — color, type, shape, spacing,
   motion, and haptics as named, documented tokens, plus a table of every shared UI component and
   when to use it. Refactored the code to actually match: a two-radius shape system replaces four
   independently hardcoded corner-radius values across three files, and a new shared
   `OumatjieInfoCard` component (`ui/components/GranifyComponents.kt`) replaces six near-identical
   hand-rolled card blocks that had already started to drift from each other. See DESIGN_SYSTEM.md
   for the full token reference and DECISIONS.md for the refactor's specifics, including one
   deliberate behavior change (informational cards are now consistently full-width).
3. **Added a splash screen** (`androidx.core:core-splashscreen:1.2.0`) — themed consistently back
   to `minSdk 28`, scoped to `MainActivity` only (not the whole app) so `PdfViewerActivity` keeps
   the Material3 theme its fragment needs. No new icon asset — it reuses the existing adaptive
   launcher icon, which is both platforms' documented default. See DECISIONS.md, "Splash screen."
4. **Wrote two new production-readiness documents**: `docs/PRIVACY_POLICY.md` (a real draft,
   accurate to this codebase's actual data handling, written for publication at oumatjie.com —
   needs a legal review before it's actually published, see its own drafting note) and
   `docs/PLAY_STORE_READINESS.md` (a concrete checklist separating what's already satisfied, what's
   pure account/paperwork work only the project owner can do, and what's still a real engineering
   gap). Also considered and deliberately did not build static App Shortcuts — see ROADMAP.md's
   "Considered and deliberately not built" note for why.

**The first 2026-08-24 session** (chronologically earlier, same date) built: Atkinson Hyperlegible
wired into every Typography style and the classic-View theme; a three-tier button hierarchy
(Hero/Standard/Tertiary) with a haptic tick on every tap; a fade-in/out animation on inbox list
items; session persistence (silent re-authorize on cold start); first-contact sender flagging; an
offline read-aloud feature (`TextToSpeech`); an AI provider abstraction with a real Anthropic
Claude Haiku 4.5 implementation and an offline demo fallback; an AI scam/phishing calm-warning
check and one-tap summarization; a Settings UI for AI features; a jargon copy-editing review pass
(concluded: no changes needed); and new unit tests for all of it.

**Important caveat on everything above, both sessions**: every one of these sessions ran in a
sandboxed environment with **no network access to any package registry and no way to invoke this
machine's real Gradle/JDK/SDK toolchain** — nothing above has actually been compiled, built, or
run. Every file was written only after reading every existing pattern it needed to match, re-read
in full afterward, and every real compile-risk API used this session
(`androidx.core.splashscreen`'s exact API surface, Material3's `Shapes`/`CardColors` constructors)
was explicitly checked against Google's own documentation/source rather than assumed — but
"carefully hand-verified" and "compiled and run" are different claims, and the gap between them
matters more with each session that adds more unverified surface area. **The very first thing the
next session with working tools should do is `./gradlew testDebugUnitTest assembleDebug` and fix
whatever a real compiler finds** before trusting anything else in this list. Full detail:
DECISIONS.md's "Verification summary (session 2026-08-24 addendum)" (written by the first
2026-08-24 session; still the right starting point, though it now predates this session's
changes).

**Carried over from 2026-08-17, still true**: full app shell — sign-in, demo inbox, real Gmail
inbox and message reading, attachment download, in-app PDF viewing with password support,
mark-as-read, move-to-Trash with confirmation, in-app text size setting, adaptive app icon,
R8/ProGuard release build — all run end-to-end on a real booted emulator as of that date (not
this session's new work, which hasn't had that chance yet).

**Deliberately not built**: Archive, reply/compose, search, pagination past the first 25
messages, calendar-aware reading, AI-flagged notifications, a chat-style AI panel
(AI_ASSISTANT.md's features 5, 6, 8), categorization's Tier 2 (AI-assisted) and its rename/merge
UI (Tier 1 itself is now built — see the 2026-08-25 session above), and sound as a UI feedback
channel (evaluated and deliberately declined, not just skipped — see DECISIONS.md). Each has a
documented reason in DECISIONS.md's Gaps/Decisions sections — these are scope cuts, not
oversights.

**Not yet exercised at all**: anything against a real Gmail account, and (new this session)
anything against a real Anthropic API call — no key has been entered anywhere, by design (see
NEEDS_YOUR_INPUT.md). No Google Cloud project exists either (SETUP.md §3 is the checklist).

**TalkBack has still never actually been tested on a real device/emulator** — a static
read-through audit (2026-08-25 session, see above) fixed what it could find by reading source, but
that's not a substitute for turning TalkBack on and listening. Still the top open gap.

**The repository is now committed and pushed — see the top of this section.** `git log` on
`main` has real history as of 2026-08-25; this superseded the long-standing "nothing is
committed" gap that every earlier version of this file flagged as top priority.

## Recommended next steps, in priority order

1. **Run the real build and test suite locally, or watch CI do it.** `ci.yml` now runs
   `testDebugUnitTest` and `assembleDebug` on every push — one real bug already found and fixed
   this way (see above). Still worth running `assembleRelease` + an emulator smoke pass locally
   (matching the 2026-08-17 verification standard) once there's a local/Claude Code session,
   since CI doesn't cover either of those yet. Pay particular attention to the rename (does
   `com.oumatjie.app` actually register correctly as an OAuth-matchable `applicationId` alongside
   the unchanged `com.granify.app` namespace?) and the `androidx.core:core-splashscreen`
   dependency, since those are among the least-precedented changes still fully unverified. See
   DECISIONS.md's verification-summary addendum for what was and wasn't possible to check without
   a working toolchain before this session.
2. **Set up Graphify and the repo-hygiene tools from `docs/TOOLING.md`.** The project owner
   flagged this as a priority once a local/Claude Code session exists, specifically because it
   should improve efficiency on everything else on this list. Read the domain warning at the top
   of that doc first — `graphify.net` (what was originally linked) is not the official project.
3. **Get a LICENSE decision from the user.** The user has said they want this repo public
   eventually; there's currently no license at all. This is a real decision (MIT vs Apache 2.0
   vs something else) with consequences for how others can use the code — ask, don't guess. A
   factual comparison is ready to hand them: `docs/LICENSE_COMPARISON.md` (2026-08-25 session).
   See also DECISIONS.md's "No LICENSE file" gap.
4. **Rename the real Windows-machine artifacts that this session's text-only rename couldn't
   reach**: the `granify_test` AVD (cosmetic only — see AGENTS.md) and, if desired, a real IDE
   "Rename package" refactor of `com.granify.app` → `com.oumatjie.app` now that Android Studio and
   a real compiler are available to verify it (this session deliberately left the Kotlin
   namespace unchanged — see DECISIONS.md's rename entry for why).
5. **Get an Anthropic API key from the user and try the AI features for real** (NEEDS_YOUR_INPUT.md
   has the exact steps) — the scam-check and summarization features have never been exercised
   against a real model response, only against hand-written fakes.
6. **Start the Play Store readiness clock.** `docs/PLAY_STORE_READINESS.md` is new this session
   and lays out the order — the OAuth restricted-scope/CASA verification and the Play Developer
   account identity verification both have long, unpredictable lead times and are worth starting
   in parallel with everything else on this list, not saved for last. `docs/PRIVACY_POLICY.md`
   needs a real legal review before it's published at oumatjie.com.
7. **Implement AI_ASSISTANT.md's remaining features, in the order specified there**:
   calendar-aware reading (5) needs the real `READ_CALENDAR` permission and a device/emulator to
   test against; AI-flagged notifications (6) need `POST_NOTIFICATIONS`; categorization's Tier 1
   is now built (2026-08-25) — what's left is Tier 2 (AI-assisted suggestion) and a rename/merge
   UI for the starter categories; the chat panel (8) is explicitly last, with AI_ASSISTANT.md
   itself suggesting reconsidering whether it's needed once the rest exist. The home-screen widget
   (Jetpack Glance) and static App Shortcuts, both considered in ROADMAP.md, are reasonable next
   candidates once there's compiler access to verify them.
8. **A real TalkBack pass** on an emulator or device — never actually done, across every session
   so far.
9. **Real Google Cloud project setup** (SETUP.md §3) whenever testing against an actual Gmail
   inbox becomes the priority — currently the single biggest thing that's built but unverified
   against a live account. Note the OAuth client now needs to be registered against
   `com.oumatjie.app` (the `applicationId`), not `com.granify.app` — SETUP.md §3 already reflects
   this.

## Key files map

| File | What it's for |
|---|---|
| `README.md` | Public-facing project overview, architecture summary, how to open the project |
| `docs/PRODUCT_PRINCIPLES.md` | The constraints — who this is for, interaction rules, privacy rules |
| `docs/SETUP.md` | Workstation setup, Google Cloud checklist, current integration status |
| `docs/DECISIONS.md` | Tactical log: dated decisions, hurdles, known limitations, gaps |
| `docs/ROADMAP.md` | Strategic log: retrospective, design direction, research findings |
| `docs/AI_ASSISTANT.md` | Full AI-assistant design spec — architecture, feature order, privacy design |
| `docs/DESIGN_SYSTEM.md` | Design tokens (color, type, shape, spacing, motion, haptics) and shared components |
| `docs/PRIVACY_POLICY.md` | Drafted privacy policy for oumatjie.com — needs legal review before publishing |
| `docs/PLAY_STORE_READINESS.md` | Checklist: what's done, what's paperwork-only, what's a real engineering gap |
| `docs/LICENSE_COMPARISON.md` | Factual, non-recommending MIT vs. Apache 2.0 comparison — for the project owner's own LICENSE decision |
| `docs/TOOLING.md` | Graphify (with a domain warning — `graphify.net` ≠ the real project) and complementary repo-hygiene tooling (Dependabot, gitleaks, detekt/ktlint, CodeQL), researched 2026-08-25, ready for a local/Claude Code session to install |
| `AGENTS.md` | Machine/tooling setup, working preferences, documentation-update ritual |
| `HANDOFF.md` | This file |
| `CLAUDE.md` | Short pointer file read automatically by Claude Code and the GitHub Actions below — points here and to AGENTS.md rather than duplicating them |
| `.github/workflows/ci.yml` | Build + unit test on every push/PR |
| `.github/workflows/claude.yml` | Responds to `@claude` mentions in issues/PRs |
| `.github/workflows/claude-ci-watch.yml` | Daily CI-health check; opens a fix PR if something's broken, never auto-merges |
| `app/src/main/java/com/granify/app/` | All application source, one package per concern (see README's Architecture section). Kotlin package/namespace unchanged by the Oumatjie rename — see DECISIONS.md. |

## What not to assume

- Do not assume any of this project's code (either 2026-08-24 session's work: typography wiring,
  button hierarchy, haptics/motion, session persistence, AI features 1–4, the rename, the design
  system refactor, the splash screen) compiles or runs correctly just because it's described as
  "done" above — everything has been hand-verified only, never built. Run a real build first (see
  "Recommended next steps" #1).
- Do not assume `com.granify.app` and `com.oumatjie.app` are a typo or inconsistency if you see
  both — they're deliberately different (`namespace` vs. `applicationId`). See DECISIONS.md's
  rename entry before "fixing" this.
- Do not assume AI-assistant features 5, 6, 8 (calendar-aware reading, notifications, chat panel)
  exist in code — none do; AI_ASSISTANT.md is still a specification for those. Categorization (7)
  is partially built: Tier 1's local rules exist (`data/categories/`), Tier 2 (AI-assisted) and a
  rename/merge UI do not. The home-screen widget and App Shortcuts, discussed in ROADMAP.md, don't
  exist in code either.
- Do not assume sound was overlooked as a feedback channel — it was researched and deliberately
  not added this pass (2026-08-25); see DECISIONS.md before adding it casually, and check
  DESIGN_SYSTEM.md's Haptics section for the requirements it would have to meet.
- Do not assume nothing is committed to git — as of 2026-08-25 it is: a real, private GitHub
  repo (`https://github.com/ReubenMiddleton/oumatjie`) with CI and the GitHub Action live. Older
  language in this project (and muscle memory from earlier sessions) said otherwise; that's now
  out of date.
- Do not assume every "hand-verified, not compiled" claim in this file or DECISIONS.md is
  probably fine — the very first real CI run found a genuine bug (see above). Treat unverified
  claims as genuinely unverified, not as low-risk by default.
- Do not assume a Google Cloud project or real Gmail credentials exist — they don't; only the demo inbox has been exercised live.
- Do not assume an Anthropic API key exists anywhere in this project — it doesn't; AI features
  are off by default and have never been exercised against a real model response.
- Do not assume `graphify.net` is the real Graphify project if you come across it again — the
  official project's own site explicitly disavows it. Use `graphify.com`/`graphifyy`/
  `Graphify-Labs/graphify` only. See `docs/TOOLING.md`.
- Do not assume `docs/PRIVACY_POLICY.md` is ready to publish as-is — it's an accurate,
  code-verified first draft, not a legally reviewed document. See its own drafting note.
- This machine's local tool paths (SDK/JDK locations, emulator name, known gotchas) are recorded in `AGENTS.md`, not repeated here — check there before rediscovering them from scratch. If working from a different machine, treat that section as a template to redo, not as fact.
