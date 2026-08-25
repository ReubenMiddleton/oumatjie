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

## Current state, as of 2026-08-25 (five sessions total — see below)

**The code has now actually been compiled and tested — for the first time in this project's
history.** This is the single most important change since the repo went live, and it retires the
caveat that every previous version of this file led with. A local Claude Code session on the
owner's Windows machine ran the real toolchain:

- `./gradlew testDebugUnitTest assembleDebug` — **BUILD SUCCESSFUL**, 44.3 MB debug APK.
- **52 unit tests, 0 failures, 0 skipped.**
- `./gradlew assembleRelease` — **BUILD SUCCESSFUL**, R8 shrinks it to 6.1 MB, lintVital clean.

**Nothing was broken.** Four sessions of hand-written, never-compiled code — the Oumatjie rename,
the splash screen, the design-system refactor, Tier 1 categorization, the AI provider abstraction
— compiled on the first attempt. The "hand-verified, not compiled" caveat below is therefore
**discharged for compile-correctness**, though *not* for runtime behavior: nothing has been run on
a device or emulator since 2026-08-17, and TalkBack still has never been tested. Full detail in
DECISIONS.md's new "Verification summary (2026-08-25, first local session with a real toolchain)".

Also this session:

- **A long-standing documented blocker turned out to be wrong.** "Android SDK Platform 37 isn't
  published yet" — repeated since 2026-08-17 in AGENTS.md, `ci.yml`, and DECISIONS.md — is false.
  Google publishes `platforms;android-37.0`/`37.1`; the real problem is that this machine has the
  *deprecated* `sdkmanager`, which can't see modern packages. CI had been quietly proving this all
  along by building `compileSdk = 37` green. Corrected in all three places. Local builds still
  need `compileSdk` temporarily set to 36 until `cmdline-tools` is installed
  (NEEDS_YOUR_INPUT.md).
- **Graphify installed** (`C:\Users\reube\.local\bin\graphify.exe`), from the verified official
  source — provenance cross-checked across PyPI/GitHub before installing, and TOOLING.md's
  unverifiable "105K+ stars" claim now independently confirmed at **110,290** via GitHub's API.
  The graph is built (**451 nodes, 1019 edges, no import cycles**; `AppContainer` is the biggest
  architectural hub and the riskiest file to change). Two corrections to TOOLING.md: its
  `pip install` instruction can't work (no real Python on this machine — `uv` was used), and its
  claim that Graphify needs no API key is **wrong** — `graphify .` demands one, so the graph is
  built with `--code-only`, which excludes this repo's 19 doc files.
- **detekt + ktlint added and wired into `ci.yml`**, both baselined so CI fails on *new* issues
  only. detekt found 45 issues; ktlint found 542 under its default style, cut to 174 by setting
  `ktlint_code_style = intellij_idea` in a new `.editorconfig` rather than reformatting 60 files.
  Three detekt findings are worth a human look and were deliberately not silenced: **swallowed
  exceptions** in `AnthropicAiProvider.kt` (×2) and `GmailMailRepository.kt`.
- **gitleaks workflow added** (`.github/workflows/gitleaks.yml`), scanning full history on every
  push/PR.
- **Dependabot is live and has opened 10 PRs** — you added `dependabot.yml` directly on GitHub.
  None reviewed. Includes majors (Kotlin 2.4, OkHttp 5) that shouldn't be batch-merged; see
  NEEDS_YOUR_INPUT.md.
- **The public repo has no license.** The `LICENSE` file drafted last session was never committed,
  so GitHub reports `"license": null` on a public repo — meaning nobody may legally reuse the
  code. Logged in NEEDS_YOUR_INPUT.md; not committed here, per the never-commit-unasked rule.

**Nothing in this session was committed.** The working tree carries all of the above plus the
previous session's uncommitted doc changes — see "Uncommitted work" at the end of this file.

## Earlier state, as of 2026-08-25 (four sessions)

**The repo is now live on GitHub, with CI and an autonomous GitHub Action, both working** —
`https://github.com/ReubenMiddleton/oumatjie` (**public as of 2026-08-25** — CodeQL, Secret
Protection, and a branch ruleset on `main` are set up; see DECISIONS.md's "LICENSE recommended...
repo made public" entry). This is the single biggest state change
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

1. ~~**Run the real build and test suite locally.**~~ **Done 2026-08-25** — debug build, release
   build, and all 52 tests pass. See the top of this file. What's left from the original item:
   **an emulator smoke pass**, which is *not* done and is now the oldest unverified thing in the
   project (last real device run: 2026-08-17, before three sessions of changes). Fold this into
   step 2 below rather than treating it as separate.
2. **Run on a real emulator and do a genuine TalkBack pass.** Now the single highest-value thing
   nobody has ever done. A working AVD already exists (`granify_test`, see AGENTS.md for how to
   drive it reliably — don't guess tap coordinates from screenshots). Two things to verify beyond
   accessibility, both of which compiled clean but have never *run*: the splash screen, and the
   2026-08-25 static-audit fixes (heading semantics and the "Unread" text label) actually being
   announced by TalkBack. Note you'll need either `compileSdk` temporarily at 36, or
   `cmdline-tools` installed first (NEEDS_YOUR_INPUT.md).
3. ~~**Set up Graphify and the repo-hygiene tools.**~~ **Done 2026-08-25** — Graphify, gitleaks,
   detekt, and ktlint are all installed/wired; Dependabot was set up by the project owner. Only
   CodeQL remains from TOOLING.md, and the repo is public now, so it's free — worth adding.
   TOOLING.md itself now contains two claims this session proved wrong (its `pip install`
   instruction, and "no API key needed"); read the corrections in DECISIONS.md alongside it.
4. **Deal with the licence gap, then the 10 Dependabot PRs.** Both are in NEEDS_YOUR_INPUT.md.
   The licence one is genuinely urgent-ish: the repo is public with no licence at all, so the
   drafted Apache 2.0 file needs confirming and committing. The Dependabot PRs now *can* be
   tested locally rather than merged on faith — do the 6 Actions bumps first, Kotlin 2.4 and
   OkHttp 5 last and carefully.
5. **Look at the three swallowed exceptions detekt found** — `AnthropicAiProvider.kt:31` and
   `:43`, `GmailMailRepository.kt:42`. Deliberately not "fixed" this session because changing
   error handling is a behavior change, not a lint fix, and these sit in auth/network paths where
   a silent failure is exactly what hides a real bug. Worth a decision either way, then either
   fix them or annotate why they're correct.
6. **Rename the real Windows-machine artifacts that this session's text-only rename couldn't
   reach**: the `granify_test` AVD (cosmetic only — see AGENTS.md) and, if desired, a real IDE
   "Rename package" refactor of `com.granify.app` → `com.oumatjie.app` now that Android Studio and
   a real compiler are available to verify it (this session deliberately left the Kotlin
   namespace unchanged — see DECISIONS.md's rename entry for why).
7. **Get an Anthropic API key from the user and try the AI features for real** (NEEDS_YOUR_INPUT.md
   has the exact steps) — the scam-check and summarization features have never been exercised
   against a real model response, only against hand-written fakes. A key would also let Graphify
   index this repo's 19 doc files, which `--code-only` currently excludes from the graph.
8. **Start the Play Store readiness clock.** `docs/PLAY_STORE_READINESS.md` is new this session
   and lays out the order — the OAuth restricted-scope/CASA verification and the Play Developer
   account identity verification both have long, unpredictable lead times and are worth starting
   in parallel with everything else on this list, not saved for last. `docs/PRIVACY_POLICY.md`
   needs a real legal review before it's published at oumatjie.com.
9. **Implement AI_ASSISTANT.md's remaining features, in the order specified there**:
   calendar-aware reading (5) needs the real `READ_CALENDAR` permission and a device/emulator to
   test against; AI-flagged notifications (6) need `POST_NOTIFICATIONS`; categorization's Tier 1
   is now built (2026-08-25) — what's left is Tier 2 (AI-assisted suggestion) and a rename/merge
   UI for the starter categories; the chat panel (8) is explicitly last, with AI_ASSISTANT.md
   itself suggesting reconsidering whether it's needed once the rest exist. The home-screen widget
   (Jetpack Glance) and static App Shortcuts, both considered in ROADMAP.md, are reasonable next
   candidates once there's compiler access to verify them.
10. **Real Google Cloud project setup** (SETUP.md §3) whenever testing against an actual Gmail
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
| `docs/LICENSE_COMPARISON.md` | Factual MIT vs. Apache 2.0 comparison, plus a note on GPL |
| `LICENSE` | Apache 2.0 — recommended and drafted 2026-08-25, still needs the project owner's confirmation (see NEEDS_YOUR_INPUT.md) |
| `docs/TOOLING.md` | Graphify (with a domain warning — `graphify.net` ≠ the real project) and complementary repo-hygiene tooling (Dependabot, gitleaks, detekt/ktlint, CodeQL), researched 2026-08-25, ready for a local/Claude Code session to install |
| `AGENTS.md` | Machine/tooling setup, working preferences, documentation-update ritual |
| `HANDOFF.md` | This file |
| `CLAUDE.md` | Short pointer file read automatically by Claude Code and the GitHub Actions below — points here and to AGENTS.md rather than duplicating them |
| `.github/workflows/ci.yml` | detekt + ktlint, then unit tests, then debug build, on every push/PR |
| `.github/workflows/claude.yml` | Responds to `@claude` mentions in issues/PRs |
| `.github/workflows/claude-ci-watch.yml` | Daily CI-health check; opens a fix PR if something's broken, never auto-merges |
| `.github/workflows/gitleaks.yml` | Secret scanning over full git history on every push/PR (added 2026-08-25) |
| `.github/dependabot.yml` | Weekly Gradle + GitHub Actions dependency PRs (added by the project owner, 2026-08-25) |
| `.editorconfig` | Shared formatting, and the `ktlint_code_style` decision — see its own header comment |
| `config/detekt/detekt.yml` | detekt rule deviations, each with a documented reason |
| `config/detekt/baseline.xml` | The 45 pre-existing detekt issues; CI fails on new ones only |
| `config/ktlint/baseline.xml` | The 174 pre-existing ktlint issues; delete it if you ever run `ktlintFormat` |
| `graphify-out/` | Generated Graphify knowledge graph — gitignored, rebuild with `graphify . --code-only` |
| `app/src/main/java/com/granify/app/` | All application source, one package per concern (see README's Architecture section). Kotlin package/namespace unchanged by the Oumatjie rename — see DECISIONS.md. |

## What not to assume

- **This one changed on 2026-08-25 — read it before acting on old muscle memory.** Every earlier
  version of this file said "do not assume any of this code compiles; it has been hand-verified
  only, never built." That is no longer true: it compiles, and all 52 tests pass. What you still
  must not assume is that it *runs correctly* — compiling is not running, and nothing has been on
  a device or emulator since 2026-08-17, which predates three sessions of changes. Treat runtime
  behavior, not compilation, as the open question now.
- Do not assume `compileSdk = 37` builds on the original dev machine — it doesn't, and that is a
  local `sdkmanager` limitation, not a code defect. Equally, do not assume Platform 37 is
  unpublished; that long-standing note was wrong. See AGENTS.md.
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
- Do not assume nothing is committed to git — as of 2026-08-25 it is: a real GitHub repo
  (`https://github.com/ReubenMiddleton/oumatjie`) with CI and the GitHub Action live. Older
  language in this project (and muscle memory from earlier sessions) said otherwise; that's now
  out of date. Note it is **public**, not private — some surrounding text still says private, and
  that is stale. It also currently has **no licence file committed**, despite being public.
- Do not assume the working tree is clean or that this session's work is committed — it isn't.
  See "Uncommitted work" below before starting anything.
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

## Uncommitted work (as of the end of the 2026-08-25 local session)

Nothing below has been committed or pushed, per this project's standing "never commit without
being explicitly asked" rule. If you're picking this up cold, `git status` should show:

| File | State | What it is |
|---|---|---|
| `LICENSE` | untracked | Apache 2.0, drafted the previous session. **Never committed — so the public repo has no licence.** Awaiting confirmation (NEEDS_YOUR_INPUT.md). |
| `.editorconfig` | untracked | New. Formatting + the `ktlint_code_style` decision. |
| `config/` | untracked | New. detekt config + both lint baselines. |
| `.github/workflows/gitleaks.yml` | untracked | New. Secret-scanning workflow. |
| `.github/workflows/ci.yml` | modified | Added the detekt/ktlint step; corrected the stale Platform 37 header note. |
| `build.gradle.kts`, `app/build.gradle.kts` | modified | detekt + ktlint plugins and config. `compileSdk` is back at the committed `37`. |
| `.gitignore` | modified | Added `graphify-out/` and `.kotlin/`. |
| `HANDOFF.md`, `AGENTS.md`, `docs/DECISIONS.md`, `docs/NEEDS_YOUR_INPUT.md` | modified | This session's documentation. DECISIONS.md and NEEDS_YOUR_INPUT.md also still carry the *previous* session's uncommitted doc edits. |

Two things worth knowing before committing any of it:

1. **The doc changes span two sessions.** DECISIONS.md, NEEDS_YOUR_INPUT.md and HANDOFF.md
   already had uncommitted edits from the previous session when this one started; those are mixed
   in with this session's. If you want clean history, that's worth separating deliberately rather
   than in one blanket `git add -A`.
2. **Committing the build-file changes will immediately exercise the new CI step**, since
   `ci.yml` now runs `detekt ktlintCheck` before the tests. Both pass locally against the
   committed baselines, so it should be green — but the baselines and `.editorconfig` must go in
   the *same* commit as the plugin changes, or CI will fail on 542 ktlint violations.
