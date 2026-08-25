# Decisions, hurdles, and gaps

A working log for whoever (human or Claude) picks this project up next. README and SETUP.md
describe what the app *is*; this file explains *why* it ended up this way, what got in the
way while building it, and what's still open. Newest entries at the top of each section.

## How to read this file

- **Decision** — a deliberate choice with real alternatives, and why this one won.
- **Hurdle** — something that fought back during implementation, and how it was resolved.
- **Gap** — known-incomplete, deliberately deferred, or unverifiable in the current
  environment. Not a bug list — a "here's what 'done' doesn't cover yet" list.

## Verification summary (session 2026-08-24 addendum)

Everything below this line and above the 2026-08-17 summary was built in a cloud sandbox with
**no network access to Maven Central, Google's Maven repository, or any package registry, and
no way to invoke this machine's actual Gradle/JDK/Android SDK toolchain.** Confirmed directly:
`curl` to `repo1.maven.org` and `dl.google.com` both returned HTTP 403 through the sandbox's
proxy, and there was no way to run `./gradlew` at all. This means none of this session's Kotlin
code has been compiled, and none of the new unit tests have actually been run — a materially
different, weaker claim than the 2026-08-17 summary below, which reflects a real emulator run.

**What was done instead, to keep the risk as low as realistically possible without a compiler:**
every new file was written only after reading every existing, already-verified file it needed to
pattern-match against (button components, DataStore-backed repositories, ViewModel factory
shape, Retrofit service definitions), rather than writing from general Kotlin/Compose knowledge
alone. The one genuine compile-risk API used in new code —
`androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm` / `.VirtualKey` — was explicitly
verified against Google's own `androidx-main` source tree
(`compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/hapticfeedback/PlatformHapticFeedback.android.kt`,
fetched directly) rather than assumed; both are real, current members, alongside `LongPress`,
`Reject`, `ContextClick`, `GestureEnd`, `GestureThresholdActivate`, `KeyboardTap`,
`SegmentFrequentTick`, `SegmentTick`, `TextHandleMove`, `ToggleOff`, `ToggleOn`. Every new/changed
file was also re-read in full at least once after writing, checking imports, parameter threading
across call sites, and smart-casts by hand.

**What this doesn't cover, and should be the very first thing the next session with a working
toolchain does**: run `./gradlew testDebugUnitTest assembleDebug` and actually fix whatever a
real compiler finds. Treat every claim below about this session's own work as "carefully
hand-verified," not "compiled and run" — that distinction matters and shouldn't get lost the next
time this file is skimmed rather than read.

## Verification summary (second session, same date — 2026-08-24)

A second session, same day, in the same kind of sandbox (no package registry access, no real
toolchain — reconfirmed, not just assumed) did the Granify→Oumatjie rename, the design-system
refactor, the splash screen, and the two new production-readiness docs described throughout this
file. Same standard as the addendum above: every changed file was re-read in full after editing,
and every genuine compile-risk API this session introduced was checked against real documentation
or source rather than assumed, specifically: Material3's `Shapes` constructor (all parameters
optional, confirmed via the Kotlin API reference) and `CardDefaults.cardColors()`'s `CardColors`
return type (confirmed the class is public and importable); `androidx.core:core-splashscreen`'s
`installSplashScreen()` — confirmed as an `Activity` extension function in
`SplashScreen.Companion` via the library's actual source on `androidx-main`, its required
before-`super.onCreate()` ordering, and that leaving `windowSplashScreenAnimatedIcon` unset
correctly falls back to the app's own adaptive launcher icon (confirmed against Android's own
splash-screen documentation, not assumed from general platform-API familiarity). One additional,
non-code verification: the stale-cache data-integrity bug this session hit and recovered from
(see "Stale device-bridge upload cache" below) was caught and fixed entirely by direct
byte-for-byte comparison against freshly re-staged device content — 68 files compared, not
sampled — before any of it reached the real device.

Same caveat as above, unchanged: nothing this session wrote has been compiled or run. Run a real
build before trusting any of it further.

## Verification summary (as of 2026-08-17)

What "verified" actually means for this snapshot, so it doesn't need to be re-derived:

- `./gradlew testDebugUnitTest assembleDebug assembleRelease` all pass together — 27 unit
  tests (0 failures), a debug APK, and an R8-minified + resource-shrunk release APK (unsigned;
  see "No production signing key" below). Verified with `compileSdk` temporarily lowered to 36
  (platform 37 isn't published to the SDK repo yet — see the hurdle below); the project's real,
  committed `compileSdk` is 37.
- The debug APK was installed and driven on a real booted emulator (API 36, `google_apis`
  x86_64, WHPX-accelerated) — not just compiled. That pass is what found and fixed the two
  crashes and the raw-exception-message bug documented below, and confirmed: the demo inbox
  end to end, attachment download and the PDF viewer end to end, Trash with its confirmation
  dialog and snackbar, mark-as-read's color change, all three text sizes on both the inbox and
  Settings screens without clipping, rotation state survival, the real Google account picker
  (reachable, shows the app's own icon), and a cancelled sign-in's graceful error path.
- Not run: anything requiring a real Google account (none is configured on the emulator or
  anywhere else — see "No production signing key" and SETUP.md §3/§4), and a physical device
  (everything above is emulator-only).

---

## Decisions

### `claude.yml` gated to the repo owner only, ahead of a possible switch to a public repo (2026-08-25)
The project owner asked whether there's any downside to making the repo public now, specifically
to get CodeQL/native secret scanning for free (see `docs/TOOLING.md`'s "CodeQL — free later, not
now" note — private repos need paid GitHub Advanced Security, public repos get it free). Answering
that honestly meant re-reading `claude.yml` with a public repo's threat model in mind, not the
private one it was written for.

**Real gap found**: `claude.yml` triggers on `issue_comment`, `pull_request_review_comment`,
`pull_request_review`, and `issues: opened`, gated only on the comment/issue body containing
`@claude` — with no check on *who* left it. On a private repo that's moot (only the owner has
access at all). On a public repo, it isn't: any GitHub account could open an issue or leave a
comment mentioning `@claude` and the workflow would run with `contents: write` /
`pull-requests: write`, authenticated as the repo owner's own `CLAUDE_CODE_OAUTH_TOKEN`. The
no-auto-merge rule already in place (see the CI/CD entry below) stops a stranger's prompt from
ever landing changes on `main` unreviewed, but it wouldn't stop them from burning the owner's own
Claude subscription usage or generating spammy/abusive PRs and issue comments in their name.

**Fixed**: added `github.actor == github.repository_owner` to the front of the job's `if:`
condition, so the workflow now only ever runs for events the repo owner themselves triggered,
regardless of repo visibility. `claude-ci-watch.yml` didn't need the same fix — it only runs on a
schedule and `workflow_dispatch`, and GitHub restricts manual dispatch to accounts with write
access by default. `ci.yml` (build+test on push/PR) also didn't need a code change: GitHub's own
default behavior — pull requests from first-time outside contributors require a maintainer to
manually approve the workflow run before it executes — already covers the equivalent risk there
(a stranger's fork PR running arbitrary code in Actions), without this project needing to
configure anything for it.

**On the underlying question — no other downside found to going public right now.** A grep across
the codebase for API keys, tokens, and other credential patterns turned up nothing hardcoded (only
variable/parameter names like `apiKey`, consistent with `docs/NEEDS_YOUR_INPUT.md`'s existing note
that no Anthropic key or Google Cloud project exists yet) — this only checked the current working
tree, not full git history, but that history is short (two commits) and this session's own
`git log` review found nothing to contradict it. The one nuance worth naming rather than treating
as a downside: an unlicensed public repo is visible to anyone but not actually reusable by anyone
(default copyright applies with no LICENSE file) — that's a separate, already-tracked decision
(`docs/NEEDS_YOUR_INPUT.md`'s "LICENSE choice"), not something going public changes or requires.

### Graphify and repo-hygiene tooling researched; nothing installed yet, use the official source only (2026-08-25)
The project owner pointed at `graphify.net` — "an open-source tool that turns your codebase,
documents, PDFs, and images into a queryable knowledge graph for AI coding assistants" — asking
for it to be set up cleanly for this repo, plus a scan for complementary tooling, both to be
handed off for a local/Claude Code session to act on. Full research, honest assessment, and a
recommended tool list live in the new `docs/TOOLING.md` — this entry is the short version.

**The domain the request pointed at is not the official project.** The real project lives at
`graphify.com` (PyPI package `graphifyy`, GitHub `Graphify-Labs/graphify`) — its own site
publishes a dedicated page stating `graphify.net` is "not affiliated with or operated by Graphify
Labs... not an official source." Whatever gets set up here should come from `graphify.com` /
`graphifyy` / `Graphify-Labs/graphify` only. This is the one finding worth remembering even
without reading the full doc.

**Assessed as a real, actively-developed project, not a scam** — named founder with a public
LinkedIn/X presence, a genuine Y Combinator company page (S26 batch), dual MIT/Apache-2.0
license, and enough real implementation detail (tree-sitter AST parsing, Leiden community
detection) that a pure marketing site wouldn't bother including it. One number is flagged rather
than repeated as fact: both the official site and its YC page claim 105K+ GitHub stars, which this
session couldn't independently verify (`api.github.com` is blocked by this sandbox's network
policy, the same restriction already documented for general GitHub access) — the only sources for
that number are the project's own site and its own YC profile. Worth a five-second sanity check
from a local session before leaning on it, though it doesn't change the recommendation either way:
the tool's actual footprint (local-only CLI, no new credentials, reads the repo, writes nothing to
it) is low-risk regardless of whether the star count holds up.

**Nothing was installed this session** — confirmed directly, not assumed: `pip install` inside a
throwaway venv here returns "No matching distribution found," and `curl -I
https://pypi.org/simple/graphifyy/` returns `403 host_not_allowed` — this sandbox's network policy
blocks PyPI the same way it already blocks general GitHub access and `apt` (see this file's CI/CD
entry below for the precedent). Setup needs a local/Claude Code session with real network access.

**Also scouted, for the same local session**: Dependabot version updates (free, native, no new
tool to trust), `gitleaks` (free secret scanning that works on a private repo without paying for
GitHub Advanced Security, which this repo would otherwise need), detekt + ktlint (Kotlin-specific
static analysis and formatting), and CodeQL (free, but only once this repo goes public — private
repos need paid Advanced Security, confirmed against GitHub's own billing docs). Full detail,
setup commands, and a suggested order are in `docs/TOOLING.md`.

### CI/CD and autonomous GitHub Action set up; direct git push from the cloud sandbox ruled out (2026-08-25)
The project owner asked for the repo to be tracked on their personal GitHub, kept cleanly
separate from their work GitHub/Codex setup, and — going further — for commits, PR fixes, and
CI-failure diagnosis to be able to happen without their input during long stretches away. This
entry records what was actually possible to build given real constraints hit along the way, and
why the final shape looks like it does.

**Ruled out: this cloud sandbox pushing directly to GitHub.** Tested directly rather than
assumed: this sandbox's network reaches `github.com` for plain file downloads and read-only git
operations (`git ls-remote` against a public repo succeeded), but GitHub's login/OAuth pages and
general REST API are blocked by the sandbox's own network policy (confirmed via direct requests
— `https://github.com/login/device` and `https://api.github.com/zen` both returned a proxy
error naming "Claude Code GitHub Actions" as the only configured path). Raw TCP to port 22
(SSH) and to GitHub's port-443 SSH fallback both timed out — also confirmed directly, not
assumed. Net effect: no OAuth device flow, no SSH key, nothing that would let this sandbox
authenticate as the project owner's GitHub account is reachable from here at all. Separately,
and independent of the network question: this session has no tool to run shell commands on the
project owner's own machine either (no `device_bash`-equivalent is available here — only file
transfer and mouse/keyboard computer-use), so even a fully working credential wouldn't have
given this session a way to actually invoke `git push` anywhere.

**Ruled out: handling a raw GitHub token/PAT directly.** Even where a Personal Access Token
would have been technically usable (git's HTTPS protocol paths aren't blocked, unlike the login
pages), entering a credential like that into a config file or command is the same category as
entering a password into a form — not something this session does on the project owner's
behalf, regardless of who supplies the value or how explicitly they authorize it. The one
credential-adjacent thing this session did do — generating an SSH keypair — was fine specifically
because a public key isn't a secret; it turned out moot once SSH itself was confirmed blocked at
the network level.

**What this leaves, and what was actually built:** the project owner runs a short one-time setup
themselves (git init/commit/push under their own login, installing the official Claude GitHub
App, and generating a subscription-backed token via `claude setup-token` — see
`docs/NEEDS_YOUR_INPUT.md`), and this session prepared everything that doesn't require touching
a credential: three GitHub Actions workflow files, ready to commit.

- **`ci.yml`** — the project's first real CI (closes the "No CI/CD" gap below): unit tests plus
  a debug build, on every push and PR. Deliberately doesn't sign or publish anything — no
  signing key is stored as a secret (see "No production signing key" below, unchanged). Flags a
  known possible first-run gap directly in its own comments: `compileSdk = 37` may still not be
  published to Google's SDK repository (last checked 2026-08-17 — see that hurdle below), which
  would fail the very first run at the SDK-download step, not because of anything in the code.
- **`claude.yml`** — the interactive `@claude`-mention responder (issues, PR comments, PR
  reviews), matching Anthropic's own documented example workflow rather than a hand-rolled
  variant, specifically to stay easy to compare against upstream docs later.
- **`claude-ci-watch.yml`** — a new, not-upstream-provided workflow: runs once daily (plus
  on-demand via `workflow_dispatch`), checks the latest CI run on the default branch, and does
  nothing if it's green. If it's red, Claude diagnoses the failure from the logs, fixes it, and
  opens a pull request describing what broke and why the fix addresses it — **it does not
  auto-merge**. This is the piece that directly answers "diagnose build/e2e failures while I'm
  away." Deliberately scoped to CI-failure diagnosis only, not open-ended feature work, to keep
  an unattended, scheduled agent's blast radius predictable. A first draft, not yet exercised
  against a real failure — expect to tune the prompt or tool list once it's actually run.

**Why PRs, not auto-merge.** A solo project with no other reviewer means an auto-merge policy
has no human backstop at all if an unattended run gets something wrong. Opening a PR keeps a
human decision point in the loop for anything that changes shipped code, while still doing all
the actual diagnosis-and-fix work without anyone needing to be present for it. This can be
revisited once the workflow has a track record — flagged as an easy future toggle, not a
permanent constraint.

**Why a subscription token (`claude setup-token`), not a metered API key.** The project owner
asked specifically for a setup that avoids billable GitHub services; GitHub Actions minutes are
free up to 2,000/month on a private repo (confirmed against GitHub's own billing docs), which
personal-scale CI runs are very unlikely to approach. The remaining cost axis — Claude API
usage — is avoided the same way by authenticating with a subscription-backed long-lived token
instead of a pay-per-token API key, so a run of either workflow above draws on the project
owner's existing plan rather than adding a separate metered bill.

**Why the scheduled workflow explicitly avoids `github_token: ${{ secrets.GITHUB_TOKEN }}`.**
GitHub does not trigger downstream workflows (like `ci.yml`) on pushes made with the default,
automatically-provided `GITHUB_TOKEN` — a deliberate anti-recursion guard. A CI-diagnosis
workflow that pushed a fix using that token would open a PR whose CI status never actually runs,
defeating the point. Leaving the action's `github_token` input unset (its default) makes it
authenticate as the Claude GitHub App instead, which doesn't carry that restriction — documented
directly in `claude-ci-watch.yml`'s own comments so this isn't accidentally "simplified" away
later.

### Sound as a feedback channel — evaluated, not built (2026-08-25)
ROADMAP.md's retrospective flagged "sound never evaluated one way or the other" as its own gap,
distinct from and smaller than the haptics work already done — "a considered 'no' is fine, but
'never considered' isn't the same thing." This entry closes that gap with an actual decision,
researched rather than assumed.

**Decision: do not add sound as a UI feedback channel this pass.** Haptics
(`docs/DECISIONS.md`'s "Three-tier button hierarchy" entry) already give every tap a
non-visual, non-auditory confirmation, and every state change that matters (Trash, mark-done,
errors) already has a visible text confirmation — a snackbar or inline banner. Sound would be a
third channel layered on top of two that already work, not a gap being filled.

**Research behind this:**
- The existing hearing-loss finding (ROADMAP.md's "Haptics, sound, and hearing loss" research,
  2026-08-17 — roughly 1 in 3 adults 65–74 have some hearing loss, rising with age) already
  settles that sound can never be the *only* channel for anything. That was never in question;
  what this pass had to newly evaluate was whether sound was worth adding *in addition* to
  haptics + visible confirmations, not instead of them.
- General searches for senior-specific sound-feedback preference research (queries covering
  "sound design for older adults," "elderly app sound effects preference") did not surface any
  study or established guideline recommending *adding* audio feedback for this audience
  specifically — the accessibility literature that does exist (Wayfindr's mobile sound-design
  guidelines, checked directly) is about audio *navigation* for vision-impaired users, a
  materially different use case from a short confirmation chime in a sighted-first app like
  Oumatjie.
- A concrete implementation risk surfaced during this research and is worth recording even
  though it didn't end up mattering for the decision: Android's conventional UI-sound-effect
  stream types (`STREAM_SYSTEM`, `STREAM_NOTIFICATION`, and the modern
  `AudioAttributes.USAGE_ASSISTANCE_SONIFICATION` equivalent) do **not** automatically respect
  the device's silent/vibrate ringer mode the way a phone's own system sounds do — an app has to
  explicitly call `AudioManager.getRingerMode()` and gate playback itself, or it can ring out
  loud on a phone its owner deliberately silenced. Confirmed directly against Android's own
  `AudioManager` reference documentation. This is exactly the kind of "looks simple, isn't"
  detail that argues for not adding sound casually.
- No new sound asset exists in the repo (unlike the font, which was sourced, OFL-licensed, and
  verified before use) — building this properly would also mean sourcing and licensing an actual
  sound file, the same diligence the Atkinson Hyperlegible font got, which is real effort with no
  identified need behind it yet.

**What would change this decision**: real usage feedback (from the project owner, or eventually
real users) specifically asking for an audible confirmation, or evidence that the current
haptic-plus-text pattern is missed by some users. Absent that signal, adding a feedback channel
looking for a problem to solve isn't the right instinct — see this file's own repeated theme of
not building speculative UI (the categorization rename/merge screen, the AI provider picker) on
a hunch rather than a demonstrated need. If sound is ever added, `docs/DESIGN_SYSTEM.md`'s
"Haptics" section now documents the three requirements it would have to meet: never the sole
channel, off by default, and gated on `getRingerMode()`.

**This closes ROADMAP.md's flagged gap** — updated there and in the "Implementation status"
section to read "evaluated, decision recorded" rather than "still never evaluated."

### Tier 1 (no-AI) mail categorization implemented; rename/merge UI deliberately deferred (2026-08-25)
New `data/categories/MailCategory.kt` (a `MailCategory(id, label)` data class plus a
`StarterCategories` object holding the fixed starter set — Bills, Receipts, Newsletters,
Family) and `data/categories/CategoryAssigner.kt` (the actual rule engine), matching
AI_ASSISTANT.md's "Categorization design" Tier 1 spec: simple local keyword rules over a
message's subject/preview text, no AI, no network call. `MailViewModel.loadInbox()` now computes
`categoryByMessageId: Map<String, MailCategory>` alongside the existing `firstContactMessageIds`
(same shape, same pattern), and `MailScreens.kt`'s `MailCard` renders the assigned category as a
plain text label when one exists — additive, no existing layout changed. Covered by
`CategoryAssignerTest.kt` (7 tests) and two new `MailViewModelTest.kt` cases.

**`MailCategory` deliberately separates a stable `id` from a user-editable `label`, even though
nothing yet lets the user rename or merge categories.** This is the one piece of forward design
in an otherwise minimal implementation: modeling identity and display text as two fields now is
what makes a future "rename this category" or "merge these two" screen a UI-only addition later
— reading/writing `label` against a stored `id` — rather than a data-model migration. The
alternative (a bare `String` category, `"Bills"` doubling as both identity and label) would work
identically today but would make that future feature meaningfully harder to add without breaking
already-assigned categories.

**Rule order is Bills, then Receipts, then Newsletters, deliberately.** A message matching more
than one keyword list (e.g. a subject mentioning both "statement" and "order") resolves to
exactly one category rather than an ambiguous result, and Bills is checked first specifically
because mischaracterizing a bill as something less consequential (a receipt, a newsletter) is a
worse mistake for this audience than the reverse.

**`Family` is never auto-assigned by a keyword rule, and this is deliberate, not an oversight.**
Unlike Bills/Receipts/Newsletters, there is no reliable text signal that a message is "from
family" — no consistent vocabulary the way "invoice" or "unsubscribe" are. A heuristic here (e.g.
matching the sender's name against a list of common first names) would be unreliable and
occasionally embarrassing rather than genuinely useful. This mirrors the same "don't fake
confidence" reasoning already applied to `DemoAiProvider` never standing in for the real scam
check (see that decision above) — an honest "not categorized" beats a guess that's sometimes
wrong in a way the user can't easily tell. `Family` stays populated only by a future manual
assignment or Tier 2's AI-assisted suggestion, neither built yet.

**Verified against the demo inbox's actual sample data, not just unit tests.** `MockMailRepository`'s
first message ("Your monthly statement is ready" / "Your statement for this month is attached.")
matches Bills; its second ("Lunch on Sunday" / "We are looking forward to seeing you on Sunday.")
correctly matches nothing and stays uncategorized — a personal message, exactly the kind of
message Family would cover if it were auto-assignable. This gives the demo inbox a sensible,
honest demonstration of the feature (one message categorized, one correctly left alone) rather
than either every message matching or none of them.

**What this session deliberately did not build**: any UI to view all categories at once, rename
one, merge two, or manually assign/reassign a category to a specific message. AI_ASSISTANT.md's
Tier 1 spec explicitly includes "the user can rename or merge" as part of Tier 1's own
definition — that part is still a real gap, not finished. Scoped out for the same reason this
session's other UI-heavy items were: a proper rename/merge screen deserves its own focused pass
(a settings-style CRUD surface, list state, name-collision handling) rather than being added
provisionally in an environment with no compiler to verify it against. The stable-id/label split
above is what keeps that future pass cheap when it happens. Tier 2 (AI-assisted suggestion for
unmatched messages) also remains entirely unbuilt, unchanged from before this session.

### Atkinson Hyperlegible wired into every Typography style, not just the 7 with size overrides (2026-08-24)
`ui/theme/Theme.kt`'s `OumatjieTypography` now routes all ~15 Material3 `Typography` styles
through the Atkinson Hyperlegible font (`Font(R.font.atkinson_hyperlegible_regular, ...)` etc.,
declared as a `FontFamily` and applied via a `TextStyle.withOumatjieFont()` extension), not only
the 7 styles that previously had a custom size override. `styles.xml`'s `Theme.Oumatjie` also now
sets `android:fontFamily` to the same font-family XML (`res/font/atkinson_hyperlegible.xml`, new
this session), so the classic View-based PDF viewer screen picks it up too, not just Compose.

**Why:** this was the exact half-done state ROADMAP.md's gap entry (below) warned the next
session not to repeat — building a full `Typography(...)` mapping every token, rather than
extending the old `Typography().run { copy(...) }` block that only touched 7 styles. The font
binaries and OFL license file were already present in the repo from a prior session; this session
only added the font-family XML and the `Theme.kt`/`styles.xml` wiring.

### Three-tier button hierarchy implemented as `OumatjieHeroButton` / `OumatjieButton` (standard) / `OumatjieSecondaryButton` (standard, outlined) / `OumatjieTertiaryButton` (2026-08-24)
`ui/components/GranifyComponents.kt` now has four button composables instead of two: Hero (80dp,
`headlineSmall`+Bold — one primary forward action per screen), the existing 64dp filled/outlined
pair (secondary but real actions), and a new Tertiary (56dp, `TextButton`, `titleLarge` not bold —
low-stakes always-available navigation like "Back to your mail," "Get help," a dialog's
"Cancel"). Every tier also now fires a light haptic tick on tap
(`LocalHapticFeedback.performHapticFeedback`) — `HapticFeedbackType.Confirm` for Hero buttons and
dialog confirm actions, `HapticFeedbackType.VirtualKey` for the other two tiers.

**Why:** this is ROADMAP.md's "Design direction: final plan" — Button hierarchy and Motion/haptics
sections, implemented as specified there (Hero/Standard/Tertiary sizing, haptics as the
foundational feedback channel given how common hearing loss is in this audience past 65). See the
"Verification summary" addendum above for how `HapticFeedbackType.Confirm`/`.VirtualKey` were
confirmed to be real APIs without a working compiler.

### List entrance/exit animation on inbox cards, via `Modifier.animateItem` (2026-08-24)
`MailCard` in `ui/mail/MailScreens.kt` now carries
`Modifier.animateItem(fadeInSpec = tween<Float>(200), fadeOutSpec = tween<Float>(150))` inside the
inbox's `LazyColumn`. A plain opacity tween, not a spring or a slide, to stay consistent with the
existing reduced-motion crossfade used for navigation.

**Why:** ROADMAP.md's retrospective flagged "zero motion beyond screen transitions" as a gap.
This is a small, deliberately unambitious first pass — one list, one property (opacity) — rather
than a broader motion pass, since Compose animation code carries real compile risk in a session
with no working compiler (see the Verification summary addendum) and a narrow, well-understood API
surface was preferred over a more expressive one.

### Session persistence implemented as a remembered boolean plus a silent re-authorize attempt (2026-08-24)
New `session/SessionRepository` interface (`hasSignedInBefore`, `recordSignedIn`, `clear`) backed
by `session/DataStoreSessionRepository`, storing exactly one boolean, never a token or email.
`SessionViewModel` now calls a private `attemptSilentSignIn()` from `init {}`: if the flag is set,
it calls `AuthManager.authorize()` once before the sign-in screen is ever shown. A `Granted`
outcome signs the user in with no UI; a `Failed` outcome (e.g. access was revoked) clears the flag
and silently falls back to the ordinary sign-in screen with no error shown; a `ResolutionRequired`
outcome also falls back silently but *keeps* the flag, since it isn't proof the account is gone
for good. Nothing is ever launched (account picker, browser tab) without an explicit prior user
action.

**Why:** ROADMAP.md's retrospective specifically called this out as "deferred too readily" —
`GoogleAuthManager.authorize()` already silently re-grants previously-approved scopes with no UI,
so the actual persistence work needed was small. `SessionRepository` is an interface (not just a
concrete DataStore class) for the same testability reason `AuthManager`/`MailRepository` already
are — see `session/SessionViewModel.kt`'s class doc and `SessionViewModelTest.kt`.

### First-contact sender flagging treats "first contact" as first-ever, not first-in-this-fetch (2026-08-24)
New `data/senders/KnownSendersRepository` interface (`isFirstContact`, `recordSeen`), backed by
`data/senders/DataStoreKnownSendersRepository` using a `stringSetPreferencesKey` of lowercased
addresses. `MailViewModel.loadInbox()` computes the first-contact set by checking every fetched
sender against this store *before* recording them all as seen, so the flag persists across app
restarts and isn't reset by pagination or a later refetch.

**Why:** AI_ASSISTANT.md's feature spec didn't fully pin down whether "first contact" meant
"never seen in this session" or "never seen, ever" — it only cited Exchange's "First Contact
Safety Tip" as the model to follow. Chose the persisted, cross-session interpretation because
that's the only one that actually matches "first contact" as a phrase, and matches how Exchange's
real feature behaves; a same-session-only version would re-flag the same sender's second email
after every cold start, which reads as broken rather than calm.

### The AI scam-check heuristic (`DemoAiProvider`) is never used for the live, automatic scam-check trigger (2026-08-24)
`ai/DemoAiProvider.checkForScamSignals()` fully implements `AiProvider` (so it's usable in tests
and previews) but `ui/mail/MailScreens.kt`'s `LaunchedEffect` that auto-triggers a scam check on
opening a message only ever passes a *real* `AnthropicAiProvider` — `realProvider`, never
`demoAiProvider` — and only when one is configured. When AI features are on but no API key has
been entered, `ScamCheckBanner` shows "Add an AI provider key in Settings to turn on scam checks"
rather than running the demo heuristic and rendering its result as if it were real.
`DemoAiProvider` *is* used for `summarize()` as an honest, clearly-labelled ("Summary (demo)")
fallback when no key is configured.

**Why:** this is the one place in this session's work where "always give the user something
rather than nothing" was deliberately overridden. `DemoAiProvider`'s scam check is a keyword
list, not a real model — for summarization, a mediocre demo summary is a low-stakes convenience
gap. For a scam check, a keyword list silently standing in for a real safety check on a message
that actually is a phishing attempt, and confidently returning "no concerns found," is actively
harmful for the exact audience this app is built for (see ROADMAP.md's phishing research). Better
to visibly say "this isn't checked yet" than to imply a check happened when it didn't.

### AI provider API key stored as a plain DataStore string, shown in a visible (not password-masked) settings field, held in-memory only while being typed (2026-08-24)
`data/settings/SettingsRepository` gained `anthropicApiKey: Flow<String?>` /
`setAnthropicApiKey()`, stored the same way every other local setting is (DataStore Preferences,
never logged — see `data/gmail/NetworkModule.kt`'s logging-level comment). The Settings screen's
`OutlinedTextField` for the key uses `remember`, not `rememberSaveable`, specifically so a typed-
but-not-yet-saved key can never end up written into a saved-instance-state `Bundle` that could
outlive the screen (e.g. surviving a process death into a bundle stored on disk).

**Why not password-masking the field:** an Anthropic API key is not a password the user re-uses
elsewhere or that grants account access on its own beyond this one integration — masking it adds
friction (no way to visually confirm what was pasted) without a corresponding security benefit
for this specific credential type, especially for an audience already flagged as sometimes finding
input-heavy UI stressful. Revisit if this app is ever handed a credential type where masking's
tradeoff looks different.

### Anthropic Claude Haiku 4.5 is the only wired-in AI provider; Gemini is not implemented (2026-08-24)
`ai/AnthropicAiProvider` is the sole real (non-demo) `AiProvider` implementation, calling
`https://api.anthropic.com/v1/messages` directly via Retrofit with model alias `claude-haiku-4-5`
(an alias, not a dated snapshot, so it keeps resolving to a current build without an app update).

**Why:** AI_ASSISTANT.md's own "Provider recommendation" section, written by a prior session's
research pass, already concluded Claude Haiku 4.5 over Gemini specifically because Gemini's free
tier may train on prompts and this app's entire premise is trustworthy handling of a vulnerable
user's real email content. Implementing only the recommended provider (rather than both, behind a
picker) keeps the `AiProvider` interface honestly exercised by exactly one real implementation
rather than speculatively built against a second one nobody has used yet — matches this project's
general "don't add an abstraction until there's a second real thing to prove it against"
instinct (see AI_ASSISTANT.md's own "Open questions" on this exact point).

### This session scoped down to AI_ASSISTANT.md features 1–4 of 8, not the full list (2026-08-24)
Implemented: first-contact flagging (1), scam/phishing calm warning (2), read-aloud (3), and
per-message summarization (4). Not implemented this session: calendar-aware reading (5),
AI-flagged notifications (6), per-user categorization — including its "Tier 1, no AI" fallback
(7), and the chat-style assistant panel (8).

**Why:** AI_ASSISTANT.md's own feature ordering already ranks these by real-world stakes and
implementation cost, and features 5–8 each carry a cost this session's environment couldn't
responsibly absorb: calendar-aware reading needs a new sensitive permission (`READ_CALENDAR`)
that can't be requested-and-tested without a real device/emulator, which this sandbox doesn't
have; notifications need `POST_NOTIFICATIONS` and a real notification-triggering flow, same
constraint; categorization (even Tier 1) is a genuinely separate feature surface with its own UI
(a way to view/rename/merge categories) that deserved its own focused pass rather than being
squeezed in; and the chat panel is explicitly last in AI_ASSISTANT.md's own ordering with "consider
whether it's needed at all" attached. This is a scope cut in the same spirit as this project's
existing ones (Archive, reply/compose) — documented rather than silently left ambiguous. See
ROADMAP.md and AI_ASSISTANT.md for the updated status markers.

### Jargon copy-editing pass: reviewed, no changes made (2026-08-24)
ROADMAP.md's research flagged "attachment," "scope," and "Trash" as candidates worth a second
look for hidden jargon (NN/g's finding that words like "page" trip up this audience). Reviewed all
existing and newly-added user-facing strings against this specifically:

- **"Attachment"** never actually appears in user-facing copy — the UI already says "Document" /
  "Contains 1 document" / "Open document" throughout (`MailScreens.kt`'s `AttachmentCard`,
  `MailCard`). "Attachment" only appears in code identifiers and doc comments. No change needed —
  this was already handled correctly by a prior session.
- **"Scope"** (an OAuth term) never appears in user-facing copy either — it's confined to code
  (`GmailMailRepository.REQUIRED_SCOPES`) and internal comments. No change needed.
- **"Trash"** was kept as-is, deliberately. It's Gmail's own real, permanent label for where a
  deleted message actually goes and can be recovered from for 30 days — the in-app copy already
  explains this in plain language every time it's used ("Moved to Trash. You can get it back from
  Trash for 30 days."). Renaming it to a softer synonym ("Deleted," "Removed") would create a
  mismatch with the real Gmail Trash folder the user might independently encounter (a browser, a
  different device), which seems like a worse outcome than the small jargon cost of a word most
  people already associate with "deleted but recoverable" from general computer literacy, not
  Gmail specifically.
- New strings added this session (AI disclosure text, `ScamCheckBanner`/`FirstContactBanner`
  copy, "New sender" label, `SummarySection` copy) were written in plain language from the start,
  reviewed against the same standard, and no jargon was found worth flagging.

**Why documented as a decision rather than left silent:** ROADMAP.md explicitly called this pass
out as a to-do; recording that it happened, and what it concluded, keeps a future session from
re-doing the same review from scratch or wondering whether it was skipped.

### Static accessibility audit — heading navigation + a color-only signal fixed (2026-08-24)
`docs/SETUP.md` and `HANDOFF.md` have flagged "TalkBack never actually tested" as an open gap
since 2026-08-17. This session still couldn't run a real TalkBack pass (no emulator/device
access — same sandbox limitation as everything else), but did a static read-through of every
screen's Compose source specifically looking for structural accessibility issues a real test
would catch, and fixed what it found. This doesn't replace a real pass — it narrows what one
would find.

**Found and fixed:**
- **Unread mail was signalled by card background color alone** (`MailCard` in
  `ui/mail/MailScreens.kt`) — invisible to TalkBack (color isn't announced) and unreliable for
  low-vision or colorblind readers, a real WCAG 1.4.1 ("Use of Color") gap. Fixed by adding an
  explicit "Unread" text label, matching the existing "New sender" label's pattern exactly (same
  card, same styling convention) — a small, additive, low-risk change.
- **No heading semantics anywhere**, meaning a TalkBack user had no way to jump between sections
  (`InboxScreen`'s "Your mail", each `MessageScreen`'s subject line, `SettingsScreen`'s title and
  each of its four sections — "Your account," "Text size," "AI features," "Privacy" — and
  `SignInScreen`'s "Just exploring?" break) without swiping through every element first. Added
  `Modifier.semantics { heading() }` to each — confirmed via `androidx-main`'s actual
  `SemanticsProperties.kt` source rather than assumed, since it's a genuine (if simple)
  compile-risk API. Purely additive; changes no visual behavior.

**Checked and confirmed already correct, not changed:**
- A clickable `Card` (`Card(onClick = ...)`, used by `MailCard`) automatically merges its
  descendants' semantics into one logical TalkBack stop — confirmed against Android's own
  Compose accessibility documentation. A card with a sender name, timestamp, subject, and preview
  as separate `Text` children was a real candidate for reading as several disjointed stops
  instead of one coherent announcement; it isn't, because `clickable` merges by default.
- The app has no bare icon-only controls anywhere (`grep`-confirmed zero uses of `Icon`/`Image`/
  `IconButton` in `ui/`) — every interactive element is a labelled text button by design (see
  `docs/PRODUCT_PRINCIPLES.md`), so the classic "icon missing a `contentDescription`" bug class
  doesn't have anywhere to occur in this codebase.
- The text-size picker's selected state (`TextScaleOption`, `ui/settings/SettingsScreen.kt`)
  already signals selection two ways, not one — filled vs. outlined button styling *and* a "✓"
  appended to the visible label text (which a screen reader reads as "check mark") — so it
  wasn't a color-only-signal case despite looking similar to the unread-card issue above.
- `Card(onClick = ...)` does not set a `Role.Button` semantics role by default (confirmed against
  Material3's actual `Surface`/`Card` source, which explicitly documents that no role is applied
  unless the caller adds one). Deliberately left as-is rather than added — a tappable list-style
  card reading as "double tap to activate" without also announcing "button" matches how list
  items are commonly treated elsewhere on the platform, and forcing a `Role.Button` onto
  `MailCard` felt like a speculative change more than a verified bug.

**Still genuinely open, not addressed by a static read-through**: real TalkBack navigation order
and gesture behavior, actual touch-target hit testing on a real screen density, and anything
that only shows up when the on-device accessibility service is actually running. A real device
or emulator pass is still the right next step — see HANDOFF.md.

### Rename: Granify → Oumatjie (2026-08-24)
The project owner bought `oumatjie.com` (Afrikaans for "grandmother") because "Granify" was
already taken as a domain/app name, and asked for the rename to be carried through the whole
project ahead of a real Play Store listing.

**`applicationId` changed, `namespace` deliberately did not.** `app/build.gradle.kts`'s
`defaultConfig.applicationId` is now `com.oumatjie.app` — this is the public, Play-Store-facing
identity, and it's what a fresh Android OAuth client (`docs/SETUP.md` §3) needs to be registered
against. `android.namespace` stays `com.granify.app`, and so does the on-disk Kotlin package
(`app/src/main/java/com/granify/app/...`) and every file's `package`/`import` line. The two
Gradle properties are independent by design; changing `namespace` would mean moving every source
file into a new package directory tree, which this session had no tooling to do safely (no shell
access to the actual Windows machine — see AGENTS.md's "Working from a cloud/sandboxed Claude
session"). A real IDE "Rename package" refactor (Android Studio, right-click the `com.granify.app`
package → Refactor → Rename) is the clean way to finish this later if it still feels worth doing;
it's cosmetic at that point, not a public-facing gap. `${applicationId}.fileprovider` (manifest)
and `context.packageName` (`AttachmentCache.kt`) both already follow `applicationId` automatically
and needed no changes.

**Everything else — identifiers, strings, resource names, doc prose — was renamed.** Every class,
composable, DataStore file name, XML resource name (`res/values/colors.xml`'s `oumatjie_*`
colors, `styles.xml`'s `Theme.Oumatjie`), and doc mention of "Granify" became "Oumatjie" (case
preserved: `Granify`→`Oumatjie`, `granify`→`oumatjie`, `GRANIFY`→`OUMATJIE`), via a script that
protects the literal `com.granify` package substring from the rename, does the text
substitution, then restores it. `.kt` file *basenames* deliberately did not move (Kotlin doesn't
require them to match the class they contain, and neither the compiler nor Android Studio's
inspections enforce it) — so `GranifyApplication.kt` now contains `class OumatjieApplication`,
`GranifyComponents.kt` now contains `OumatjieHeroButton`/`OumatjieButton`/etc., and so on. This
was a deliberate choice, not an oversight: renaming file basenames too would have meant deleting
and recreating every file rather than editing in place, with no way in this environment to
confirm a rename-vs-recreate round-trip didn't lose anything. A future session with real
Android Studio access could do a proper "Rename file to match class" pass if that inconsistency
is worth cleaning up; it has zero effect on how the app builds or runs.

**One test fixture needed a manual fix the blanket rename couldn't safely make.**
`MimePayloadParsingTest.kt` asserted that the base64 string `SGVsbG8sIEdyYW5pZnkh` decodes to
`"Hello, Granify!"` — a blind text substitution would have renamed the assertion string to
`"Hello, Oumatjie!"` while leaving the base64 constant (which actually decodes to the old text)
untouched, silently breaking the test. Recomputed the correct value by hand
(`SGVsbG8sIE91bWF0amllIQ`, verified via `base64.urlsafe_b64decode`) and updated both the comment
and the assertion together.

`rootProject.name` (`settings.gradle.kts`) is now `"Oumatjie"`; the two remaining Granify-prose
mentions in `app/build.gradle.kts` (a comment explaining the applicationId/namespace split, and
a mention of "Granify" in a dependency comment) were edited by hand rather than by the script,
since that file wasn't a rename target (its `namespace`/`com.granify.app` value needs to *stay*
literal, which a blanket substitution can't distinguish from prose).

### Stale device-bridge upload cache silently reverted several already-renamed files mid-session (2026-08-24)
While rebuilding this session's local sandbox mirror of the repo (to run the rename script
against it), a blanket `cp -r <staged-upload-dir>/. <mirror-dir>/` pulled in stale content for
several files that a *previous* session had already edited and written back to the real device.
The upload directory `device_stage_files` writes into is deterministic (derived from the common
parent of the requested paths) and — this is the part that wasn't obvious going in — appears to
persist and accumulate across multiple `device_stage_files` calls within one session rather than
being freshly and exclusively populated per call. Files staged early in this session (before this
session made its own edits, let alone before a prior session's edits existed) were still sitting
in that cache; a blanket recursive copy doesn't distinguish "freshly staged this call" from
"cached from three calls ago," so files not included in the most recent staging call came back
old.

**Caught by**: manually diffing the 24 files the rename script reported as changed against the
pre-rename backup mirror, and noticing `SignInScreen.kt` referenced `GranifyButton` — a component
name that stopped existing once the three-tier button hierarchy shipped earlier this session (see
the "Three-tier button hierarchy" decision above); it should have read `GranifyHeroButton`
(pre-rename). That's a content regression having nothing to do with renaming, which is what made
it stand out during review.

**Fixed by**: re-staging every `.kt`/`.xml` file in the app plus `AndroidManifest.xml`,
`app/build.gradle.kts`, `settings.gradle.kts`, and `proguard-rules.pro` fresh from the real
device (68 files total, three `device_stage_files` calls), then comparing each one byte-for-byte
against the corresponding file already in the sandbox mirror. 30 of the 68 were stale and got
overwritten with the freshly staged, ground-truth version before the rename ran again on top of
them. The rename script's original output was discarded entirely rather than patched, since there
was no way to know which of its 24 "changed" files were changed correctly and which were changed
starting from already-stale content. After the fix, a repeat of the same byte-for-byte comparison
against fresh staging showed zero remaining differences, and the rename was re-run cleanly on
this now-verified baseline.

**No data was lost and nothing wrong was ever written to the real device** — this was caught and
fixed entirely within the sandbox mirror, before any `device_commit_files` call this session.
**Lesson for future sessions working this way**: never trust a bulk copy out of the device-bridge
upload cache without first re-staging (or otherwise confirming the mtime of) every file the copy
will touch; the safer pattern is to stage exactly the files needed immediately before copying
them, every time, rather than reusing whatever happens to already be in that directory.

### Design system formalized: two-radius shape tokens + a shared `OumatjieInfoCard` (2026-08-24)
Added `docs/DESIGN_SYSTEM.md` as the single documented source of truth for color, type, shape,
spacing, motion, and haptics, and refactored the code to actually match it rather than just
describing the status quo.

**Shape tokens.** `ui/theme/Theme.kt` now defines `OumatjieShapes` (a Material3 `Shapes` with
`medium = 16dp`, `large`/`extraLarge = 24dp`) and wires it into `MaterialTheme(shapes = ...)`.
Before this, `16.dp` and `20.dp` corner radii were hardcoded as `RoundedCornerShape(...)` calls
independently in `MailScreens.kt` (three places) and `SignInScreen.kt` (one place), with no
shared definition — a real risk of silent drift (someone tweaks one, not the others) and one of
the specific "looks unstyled/AI-scaffolded" tells this session researched (see
DESIGN_SYSTEM.md's "Avoiding a generic/AI-generated look"). `MailCard` now uses
`MaterialTheme.shapes.large` explicitly; every informational card gets `shapes.medium` via the
component below.

**`OumatjieInfoCard`** (`ui/components/GranifyComponents.kt`) is a new shared, documented
component — a `Card` that owns shape/color/padding and takes a `tone` (`Neutral` /
`Highlight` / `Problem`, mapping to `surfaceVariant` / `secondaryContainer` / `errorContainer`)
plus a content-projection lambda for the caller's specific text/layout, the same shape as an
Angular shared component taking typed `@Input`s and projecting `<ng-content>`. It replaces six
near-identical hand-rolled `Card` blocks that had already started to drift from each other:
`MailScreens.kt`'s `FirstContactBanner`, the scam-check "Worth a closer look" card, the message
summary result card, `AttachmentCard`, and the inline error card in `MessageScreen`, plus
`SignInScreen.kt`'s inline sign-in-failure card. One of the six (`AttachmentCard`) had no
explicit shape at all and was silently falling back to Material3's un-themed default rather than
matching the others.

**Known, deliberate behavior change**: `OumatjieInfoCard` always applies `Modifier.fillMaxWidth()`
internally. Several of the six original call sites didn't set `fillMaxWidth` on their `Card`
explicitly (unlike `MailCard`/`AttachmentCard`, which did), meaning a `LazyColumn` item without
enough content to fill the row could in principle have rendered narrower than the screen. This
was judged to be drift, not intent — nothing in `docs/PRODUCT_PRINCIPLES.md` or
`docs/DESIGN_SYSTEM.md` calls for narrower-than-full-width informational cards, and a shared
component that requires every caller to remember to add `fillMaxWidth` defeats the point of
sharing it. Flagging explicitly in case it turns out some narrower card was actually wanted
somewhere — nothing observed in a hand-review of the six call sites suggested that.

### Splash screen implemented via androidx.core:core-splashscreen (2026-08-24)
Added a real cold-start splash screen instead of relying on Android's own default (a blank white
flash, or — on API 31 without any splash configuration — the app icon on a plain white
background with no theming). `app/build.gradle.kts` adds
`androidx.core:core-splashscreen:1.2.0` (the current stable release, confirmed against
`developer.android.com/jetpack/androidx/releases/core`), which backports the Android 12+
platform `SplashScreen` API down to this app's `minSdk = 28`, so the same themed splash shows
consistently on every supported Android version rather than only on newer ones.

**Applied to `MainActivity` only, not the `<application>` tag.** `styles.xml`'s new
`Theme.Oumatjie.Starting` (`parent="Theme.SplashScreen"`) is set as `MainActivity`'s own
`android:theme` in `AndroidManifest.xml`, not at the `<application>` level the way the migration
guide's simplest example shows. That's deliberate: `PdfViewerActivity` has no `android:theme` of
its own and relies entirely on inheriting `<application>`'s `Theme.Oumatjie` (a real Material3
theme, required for androidx.pdf's `PdfViewerFragment` to inflate — see `styles.xml`'s own
comment). Applying the splash theme at the application level would have made `PdfViewerActivity`
inherit `Theme.SplashScreen` instead and broken fragment inflation; scoping it to just the
launcher activity avoids that entirely while still covering the only place a splash screen is
meaningful (a cold start).

**No custom splash icon asset was created.** `windowSplashScreenAnimatedIcon` is left unset in
`Theme.Oumatjie.Starting`, which both the platform API and the compat library document as
defaulting to the app's own adaptive launcher icon (`android:icon` in the manifest) —
confirmed directly against Android's splash-screen documentation rather than assumed. This reuses
the existing hand-authored launcher icon (`docs/DECISIONS.md`, "App icon is hand-written vector
XML") with zero new assets and zero sizing/masking risk. `windowSplashScreenBackground` is set to
`@color/oumatjie_background` — the same warm off-white the app's own background uses — so the
splash-to-first-frame transition has no visible color flash. `MainActivity.kt` calls
`installSplashScreen()` as the very first line of `onCreate()`, before `super.onCreate()`,
matching the library's documented required ordering.

### Color palette and contrast (2026-08-17)
Every color role Compose actually resolves (`primary`, `primaryContainer`,
`secondaryContainer`, `background`, `surface`, `surfaceVariant`, `error`, `errorContainer`,
`outline`, plus their `on*` pairs) is now explicitly set in `ui/theme/Theme.kt`, computed and
checked against WCAG contrast math rather than left to Material3's baseline defaults.

**Why:** `lightColorScheme()` doesn't derive unset roles from the ones you do set — it fills
them from M3's baseline neutral-gray palette. Two roles the app actually uses
(`surfaceVariant` for read message cards, `errorContainer` for inline error cards) were never
set, so they were silently rendering as generic Material gray/pink that didn't match Oumatjie's
warm palette and had never been contrast-checked, despite this being an accessibility-first
app. Found by writing a small standalone contrast checker
(`ContrastCheck.java`, WCAG relative-luminance formula) rather than eyeballing hex values.
Every pair now clears 7:1 (AAA); `outline` only needs 3:1 (non-text) and clears it at 5.09:1.

### Reduced motion in navigation (2026-08-17)
`OumatjieNavHost` overrides Navigation Compose's default enter/exit transitions (a slide+fade)
with a plain 150ms crossfade, applied globally via `NavHost`'s own transition parameters
rather than per-`composable`.

**Why:** docs/PRODUCT_PRINCIPLES.md calls out reduced motion as a requirement, and this was
the one place it hadn't been addressed. Went with "always minimal motion" rather than reading
`Settings.Global.TRANSITION_ANIMATION_SCALE` and branching on it — simpler, and a fast
opacity-only crossfade isn't the kind of motion that triggers vestibular issues in the first
place, so there's little upside to making it conditional. If a future screen wants a more
expressive transition, override it per-`composable` rather than changing the default.

### PDF viewer error state matches ErrorScreen's pattern, not a red-tinted card (2026-08-17)
`PdfViewerActivity`'s failure state uses a neutral background with a bold headline
("This document did not open") + body text + action button — the same shape as Compose's
`ErrorScreen` — rather than tinting the screen with `errorContainer`.

**Why:** Compose already has two different error treatments in this app: a full-screen
neutral `ErrorScreen` for "there's nothing else to show" (e.g. inbox failed to load), and a
small inline `errorContainer`-tinted card for "something failed but you still have context on
screen" (e.g. Trash failed, message still open). The PDF viewer's failure is the first kind —
full-screen, only escape is "back" — so it should match `ErrorScreen`'s visual language, not
introduce a third pattern. `oumatjie_error_container`/`oumatjie_on_error_container` are still
defined in `colors.xml` (now correct, matching Theme.kt) for the day an XML screen genuinely
needs an inline error card.

### App icon is hand-written vector XML, not generated PNGs (2026-08-17)
Adaptive icon (`background` + `foreground` + `monochrome` layers) built entirely as Android
vector drawables — a dark-green background plus a line-art envelope glyph — with no PNG
assets and no legacy pre-adaptive-icon fallback.

**Why:** minSdk is 28, which is already above API 26 (when adaptive icons shipped), so
per-density PNG exports aren't needed at all — one set of vector XML covers every device this
app can run on. The envelope is stroke-only (not fill+stroke in two colors) specifically so
the *same* path data works unmodified as the API 33+ monochrome/themed-icon layer, where the
system applies its own single tint color and a two-tone design would lose the second color
entirely. This is a first pass at a real icon, not a placeholder, but it's also not a
designer's icon — if Oumatjie gets real branding/a logo later, this is the file to replace
(`res/drawable/ic_launcher_*.xml`), not redesign around.

### R8 keep rules added explicitly, even though most are probably redundant (2026-08-17)
`proguard-rules.pro` has explicit keep rules for Retrofit's dynamic proxy, kotlinx.serialization's
generated serializers, and all `Fragment` subclasses, plus a blanket keep on `androidx.pdf.**`.

**Why:** Retrofit, OkHttp, kotlinx.serialization, and Play services all ship consumer rules
inside their own AARs that most likely already cover the first three — but "most likely" and
"already verified" aren't the same thing, and confirming exactly which rules are bundled
where is more effort than just writing the standard, well-documented versions of these rules
by hand. androidx.pdf is the real risk: it's `1.0.0-alpha19` with no long track record against
R8, so it's kept whole rather than trusted to shrink safely. See "Verification summary" above
for how the resulting release build was actually checked.

### Attachment cache is cleared on PDF viewer close, not per-file (2026-08-17)
`PdfViewerActivity.onDestroy()` (guarded by `isFinishing`, so it doesn't fire on rotation)
calls `AttachmentCache.clear()` — wiping the *entire* attachment cache directory — rather than
deleting just the one file that screen opened.

**Why:** Reversing a FileProvider `content://` URI back to its underlying `File` isn't a
supported/stable operation (`FileProvider` is deliberately one-directional), so per-file
cleanup would mean threading the original `File` (or at least its name) through the Intent
separately from the URI. Since the app only ever has one attachment open at a time in
practice, wiping the whole cache on viewer-close is behaviorally equivalent and far simpler.
The real backstop is still `OumatjieApplication.onCreate()` sweeping the cache on every cold
start, which covers the crash/force-stop case this per-screen cleanup can't.

### `AttachmentDownloader` returns `String`, not `android.net.Uri` (2026-08-17)
The interface downloader implementations (mock and real) return the content URI as a plain
`String`; `MailViewModel` and its `openDocumentEvents` channel are typed `String` too. The one
`Uri.parse()` call happens at the last possible moment, in the Compose layer right before
`startActivity`.

**Why:** Found while writing `MailViewModelTest`'s success-path test for `openAttachment()` —
constructing a real `android.net.Uri` in a plain JVM unit test isn't possible without either a
mocking library (not otherwise used in this project) or Robolectric (not a dependency). Rather
than add either, pushing the one unavoidable Android-framework touchpoint down to the UI layer
keeps the ViewModel fully platform-independent and directly testable with plain fakes — which
is a better outcome on its own merits, not just a workaround.

### Manual DI over Hilt (carried over from the first pass, reaffirmed here)
Still a hand-written `AppContainer`, no DI framework. Revisited this assumption during the
production pass and it still holds: the object graph is small (a dozen-ish singletons, no
scoped/multibinding needs), and Hilt's KSP annotation processing is one more thing that could
interact badly with the alpha PDF library or kotlinx.serialization during R8 — not a large
risk, but not a risk worth taking for what Hilt would save here.

---

## Hurdles

### First real CI run found a real bug hand-review missed: `authorize()` called twice per sign-in (2026-08-25)
The very first GitHub Actions `ci.yml` run against the new `oumatjie` repo (see "CI/CD and
autonomous GitHub Action set up" under Decisions) failed on `testDebugUnitTest`:
`SessionViewModelTest.init_withPreviousSignIn_andGrantedOutcome_signsInSilentlyWithNoLoadingLeftOver`
asserted `authManager.authorize()` was called exactly once during a silent re-auth, and it was
actually called twice.

**Root cause**: `SessionViewModel.handleOutcome()`'s `Granted` branch called
`gmailMailRepository.fetchAccountEmail()` with no arguments, and that function derived its own
auth header via `authHeader()` — which itself calls `authManager.authorize()` again. Every
sign-in path (silent re-auth on cold start, `signInWithGoogle()`, `onAuthorizationResolved()`)
was calling `authorize()` twice: once to learn the outcome, once more, redundantly, just to
fetch the account email. Harmless functionally against a real Google account (a second call to
an already-granted `authorize()` just returns immediately with no UI — the same silent-refresh
behavior `GmailMailRepository`'s own class doc describes), but a real, unnecessary extra network
round-trip on every sign-in, and exactly the kind of thing a real test run catches that reading
the code carefully does not.

**Fixed** by threading the token straight through instead of re-deriving it: `fetchAccountEmail`
now takes an `accessToken: String` parameter (`GmailMailRepository.kt`), and
`SessionViewModel.handleOutcome()` passes `outcome.accessToken` — the token it already has from
the very `Granted` outcome it's handling — rather than triggering a second `authorize()` call.
No other call site existed for the old zero-arg signature.

**Why this entry matters beyond the fix itself**: every "Verification summary" note in this file
since 2026-08-24 has flagged the same caveat — this project's later sessions ran in a sandbox
with no compiler, so their claims were "hand-verified," never "compiled and run," and that
distinction "matters more with each session that adds more unverified surface area." This is the
first time that gap actually mattered: a real toolchain (GitHub Actions, once the repo existed)
found a real bug on its very first run that two full sessions of careful hand-review missed. Not
a reason to distrust everything hand-verified in this project — most of it will likely turn out
fine — but a concrete, non-hypothetical confirmation that "carefully reviewed" and "actually run"
are genuinely different claims, worth remembering before treating any still-unverified section of
this file as more solid than it's actually been shown to be.

### PDF viewer crashed again after the first fix: `isToolboxVisible` needs the fragment's view, not just its attachment (2026-08-17)
Fixing the crash below (switching to `commitNow` + setting `documentUri` after) made the
viewer load correctly — but it showed a floating purple edit/annotate FAB from the library's
built-in "toolbox" that has no place in a read-only viewer for this audience. Setting
`fragment.isToolboxVisible = false` right after `documentUri` (same spot, same file,
`PdfViewerActivity.onCreate()`) crashed with a *different* error:
`UninitializedPropertyAccessException: lateinit property _toolboxView has not been
initialized`. `commitNow` guarantees the fragment is *attached* (enough for `documentUri`,
which only needs a `ViewModelStore`) but does not guarantee its *view hierarchy* exists yet —
that depends on the host Activity's own lifecycle position at the moment of commit, and
`isToolboxVisible`'s setter reaches into a view (`_toolboxView`) that only exists once the
fragment's own `onViewCreated()` has run. Fixed by moving the `isToolboxVisible = false` call
into `OumatjiePdfViewerFragment.onViewCreated()` itself — the fragment setting its own
view-dependent config in its own lifecycle callback, rather than the host Activity guessing
whether enough of that lifecycle has elapsed yet. General lesson for anything else this alpha
library's API surface might need later: `documentUri` (attachment-level) was safe to set
right after `commitNow`; anything that touches the fragment's *view* is not, and belongs in an
override on the fragment subclass instead.

### PDF viewer crashed on first real tap: "Can't access ViewModels from detached fragment" (2026-08-17)
Found by actually running the app on an emulator and tapping "Open document" — this could not
have been caught by the earlier compile-only verification. `PdfViewerActivity` was building
the fragment and setting `documentUri` in the same expression
(`OumatjiePdfViewerFragment().apply { this.documentUri = documentUri }`) before ever attaching
it to the `FragmentManager`. `PdfViewerFragment`'s `documentUri` setter internally reaches for
its own `ViewModelStore` (it hoists document-loaded state into a `PdfDocumentViewModel`, which
is exactly why that state survives rotation) — and a fragment that isn't attached yet has no
`ViewModelStore`, so this threw immediately and crashed the whole app on every single "Open
document" tap. Fixed by switching `commit { }` to `commitNow { }` (forces the transaction to
actually attach the fragment synchronously, rather than merely scheduling it) and moving
`documentUri = documentUri` to *after* that call returns. This exact ordering — attach fully
first, set the document second, as two separate statements rather than one chained expression
— is also what Google's own sample code does, which wasn't obvious until this crashed; the
sample reads as a style choice until you understand why the two calls can't be merged.

### `checkDebugAarMetadata` failed: androidx.pdf needs `compileSdkExtension 19` (2026-08-17)
`androidx.pdf:pdf-core` (and its siblings) declare in their AAR metadata that consumers must
compile against SDK extension level 19 or higher. This is a *different* axis from `compileSdk`
itself (Android's mainline-module extension SDK versioning) and isn't visible from reading the
library's bytecode — only from actually attempting a build. Fixed with one line:
`compileSdkExtension = 19` in `app/build.gradle.kts`. AGP auto-downloaded the matching
"Android SDK Platform 36-ext19" extension stub the moment the config was in place, no manual
`sdkmanager` step needed.

### `PdfViewerFragment.onLoadDocumentSuccess()` doesn't override anything (2026-08-17)
Google's own official guide (fetched via WebFetch) shows `override fun
onLoadDocumentSuccess()` with no parameters. The real `1.0.0-alpha19` artifact's actual
signature is `onLoadDocumentSuccess(document: androidx.pdf.PdfDocument)`. The compiler caught
this immediately; the fix was a one-line signature change plus an import. Lesson: for an
alpha library, "the official doc says X" and "the artifact you actually resolved says X" can
disagree, and only a real build against the real resolved jar settles it — bytecode
inspection (see below) or a compiler error, not documentation.

### `androidx.compose.foundation.layout.weight` — "it is internal in file" (2026-08-17)
`Modifier.weight(1f)` used inside a `Row`, imported the conventional way
(`import androidx.compose.foundation.layout.weight`), failed to compile: the resolved compose-bom
version has moved `RowScope.weight`/`ColumnScope.weight` to be members of those scope
interfaces rather than top-level extension functions, so that import path now only resolves to
an unrelated *internal* `RowColumnParentData.weight` property that happens to share the name.
Fix: delete the import entirely — inside a `Row { }`/`Column { }` lambda, `weight` is already
in scope as a member function and needs no import at all. Worth remembering for any future
Compose layout code: if `weight`/`align`-style scoped modifiers ever throw a similar
"internal" error, try removing the import before assuming something is actually broken.

### Retrofit 3.0.0's own POM still pins OkHttp 4.12.0, not 5.x (2026-08-17)
Assumed "latest Retrofit" would want "latest OkHttp." Checking Retrofit 3.0.0's actual POM
(not just its own Maven metadata) showed it depends on `okhttp:4.12.0`. Declaring OkHttp 5.5.0
explicitly would have let Gradle's highest-version-wins resolution silently put an untested
OkHttp major version underneath Retrofit. Pinned `okhttp`/`logging-interceptor` to 4.12.0 to
match what Retrofit itself was actually built and tested against.

### This machine's default JRE has no `javac` (2026-08-17)
`C:\Program Files\Eclipse Adoptium\jre-21.0.11.10-hotspot` (also `JAVA_HOME`) is JRE-only.
Android Gradle Plugin wires up a Java compile task (`compileDebugUnitTestJavaWithJavac`) even
for a pure-Kotlin module, so the very first build attempt failed before touching a single line
of Oumatjie's own code. A full JDK 17 was already present via unrelated bubblewrap tooling
(`C:\Users\reube\.bubblewrap\jdk17-x64\jdk-17.0.20+8`) and was used via
`-Dorg.gradle.java.home=...` rather than touching any committed project file. Full detail
saved to this session's Claude memory (`dev_tooling_paths`) so a future session doesn't
re-diagnose this from scratch.

### Android SDK Platform 37 isn't published to Google's SDK repository yet (2026-08-17)
`compileSdk = 37` is the project's real target, but neither the stable nor preview
(`--channel=3`) `sdkmanager` listing offers `platforms;android-37` as of this date — only
`build-tools;37.0.0` exists so far. Verified against a temporary local `compileSdk = 36`
(reverted immediately after) rather than assuming the gap meant something was broken in the
code. This is a pure environment/timing gap, not a Oumatjie issue, and should resolve itself
once Google publishes the platform — worth a quick `sdkmanager --list` recheck before
concluding it's still true.

### `sdkmanager` with `echo y |` silently produced zero output for ~20 minutes (2026-08-17)
Piped through `tail -20` to keep the log short; `tail` (not `tail -f`) buffers *all* input and
only prints once the underlying command reaches EOF, so nothing appeared until the whole
install finished — which looked identical to a hung process waiting on a second license
prompt `echo y`'s single answer couldn't satisfy (a real risk when installing multiple
packages that carry different license IDs in one command). Killed it and re-ran without the
`tail` and with `yes |` instead of `echo y |`, which is safe against any number of prompts.
Lesson for next time: never pipe a live sdkmanager/gradle install through `tail` — pipe
through nothing, or `tee` to a file if a short log is wanted.

### Sign-in error text was showing the raw exception message, not the friendly fallback (2026-08-17)
Found by actually cancelling the Google account picker on the emulator (no account is
configured on it, so `Identity.getAuthorizationClient` genuinely returns a real
`ApiException` — this path could not have been exercised by unit tests against fakes).
`GoogleAuthManager.describe()` was written as `e.message ?: <friendly template>`, on the
assumption that `ApiException.message` would usually be null and the friendly template would
be what most users see. In practice `ApiException.message` is very often *non-null* — for this
cancellation it was `"16: [16] Cancelled by user."` — so the `?:` fallback almost never
triggered, and the raw, developer-oriented string is what actually reached the screen.
Rewrote `describe()` to never use `e.message` at all: it special-cases
`CommonStatusCodes.CANCELED` (confirmed to be exactly code 16 by this same test) with
"You closed the Google sign-in screen before finishing…", and otherwise always shows
"Please try again, or use the demo inbox below." Logcat already has the full exception for
anyone debugging; the UI no longer needs to double as that channel.

---

## Known limitation

### A floating edit/annotate FAB from androidx.pdf's toolbox is still visible (2026-08-17)
`OumatjiePdfViewerFragment.isToolboxVisible = false` is set both in `onViewCreated()` and (an
attempt that was reverted — see below) again in `onLoadDocumentSuccess()`, and a purple
pencil-icon FAB still appears in the bottom-right corner once a document loads. Setting the
documented `isToolboxVisible` property clearly isn't what controls this specific element —
most likely it toggles a different piece of the toolbox (a top search/page bar, going by the
`PdfSearchViewManager`/`ToolBoxView` symbols visible in the library's bytecode), and this FAB
is something else, gated by internal state this alpha library doesn't expose a supported way
to control. Tried and reverted: re-asserting `isToolboxVisible = false` again after the
document finishes loading, on the theory that a reactive state collector inside the library
was reasserting visibility after the initial `onViewCreated` setting — the FAB was still there
either way, so that line was dead weight and removed rather than left in.

Left as a known, investigated cosmetic gap rather than pursued further: it doesn't crash or
block reading/searching/zooming, and chasing an undocumented internal state machine inside a
1.0.0-alpha19 library has real diminishing returns against everything else still open. Options
for whoever picks this up: check whether a newer alpha release exposes a real toggle for it,
or intercept the touch region and no-op it as a last resort if it turns out to be genuinely
confusing in front of real users.

## Gaps

### No LICENSE file (2026-08-17)
The repository has no `LICENSE` at all yet — not even a placeholder.

**Why left this way:** the user has said they want to make this repo public eventually, which
makes a license a real requirement before that happens, not a nice-to-have. But which license
(MIT, Apache 2.0, GPL, something else) is a decision with real consequences for how others can
use the code, and — like the production signing key (below) — it's the user's call to make
deliberately, not something to pick quietly as a side effect of a documentation pass. Flagged
here so it isn't forgotten; see HANDOFF.md's next steps.

### Atkinson Hyperlegible font downloaded but not wired in (2026-08-17) — RESOLVED 2026-08-24
Four font files (`app/src/main/res/font/atkinson_hyperlegible_{regular,bold,italic,bolditalic}.ttf`)
and its license (`app/src/main/assets/licenses/atkinson_hyperlegible_OFL.txt`) are committed to
the repo, but nothing in `ui/theme/Theme.kt` or anywhere else references them yet — the app
still renders in Compose's default Material3 typeface.

**Resolved 2026-08-24**: see "Atkinson Hyperlegible wired into every Typography style" under
Decisions above — `Theme.kt`'s full `Typography` now routes through this font, and
`styles.xml`/a new `res/font/atkinson_hyperlegible.xml` font-family cover the classic-View PDF
viewer screen too. Left in place as history rather than deleted, per this file's own convention.

**Why left this way:** downloaded mid-session while planning the typography change described in
docs/ROADMAP.md, but the user then explicitly narrowed this session's scope to documentation
only ("I just want you to do the documentation work please") before the `Theme.kt` edit was
made. The files themselves are inert (unreferenced font/license assets, no risk sitting in the
repo) and were kept rather than deleted, since downloading them again costs real effort for no
benefit. Whoever implements docs/ROADMAP.md's typography plan next should build a full
`Typography(...)` mapping every token through the font, not extend `Theme.kt`'s current
`Typography().run { copy(...) }` block, which only overrides 7 of the ~15 available styles.

### Never tested against a real Gmail account
Everything in `data/gmail/` was built and unit-tested against hand-written fakes
(`GmailMailRepositoryTest`, `MimePayloadParsingTest`), because there is no Google Cloud OAuth
client registered yet (docs/SETUP.md §3). The Gmail REST API shapes are stable and
well-documented so confidence is reasonably high, but "compiles and passes unit tests against
fakes" is not the same claim as "works against a real inbox." First things worth specifically
checking once real credentials exist: token behavior over a longer session, how an HTML-only
(no plain-text part) email actually looks once stripped, and whether Gmail ever returns a MIME
shape `MimePayloadParsing.kt`'s fixtures didn't anticipate.

### No production signing key
Release builds have no `signingConfig`, deliberately — generating a production Play Store
signing key is a decision with real, hard-to-reverse consequences (lose it and you can never
publish an update to the same app again), and where it's stored securely is the user's call to
make, not something to generate quietly as a side effect of a build-verification pass. A
throwaway local-only keystore was used for release-build runtime verification (see
"Verification summary" above); it is *not* meant to sign anything that ships.

### No CI/CD — RESOLVED 2026-08-25
Not built, and not started unprompted — wiring up GitHub Actions (or similar) means decisions
about where a signing key/secrets would live, which is the same "not mine to decide quietly"
reasoning as the signing key itself.

**Resolved 2026-08-25**: the project owner explicitly asked for this (see "CI/CD and autonomous
GitHub Action set up" under Decisions above). `.github/workflows/ci.yml` builds and unit-tests
on every push/PR; no signing key or secret beyond a subscription auth token is involved — the
signing-key gap itself (below) is unchanged and still open. Not yet run for real — the workflow
exists but the repo isn't pushed to GitHub yet as of this entry; see
`docs/NEEDS_YOUR_INPUT.md`.

### Session doesn't persist across app restarts (2026-08-17) — RESOLVED 2026-08-24
Closing and reopening the app always returns to the sign-in screen, even right after signing
in. This was a deliberate scope cut in the first pass (see original architecture notes) rather
than an oversight — building real persistence (silently re-checking a stored grant on cold
start, handling the case where it's since been revoked) is realistically its own small feature,
better done deliberately once there's a real account to test it against than spliced in now.

**Resolved 2026-08-24**: see "Session persistence implemented" under Decisions above. Not yet
verified against a real account (no Google Cloud project exists — see NEEDS_YOUR_INPUT.md), only
hand-reviewed and covered by `SessionViewModelTest.kt`'s fakes; genuinely exercising the silent
re-authorize path is one of the first things worth doing once real credentials exist.

### Archive and reply/compose don't exist
Matches docs/PRODUCT_PRINCIPLES.md and the README's MVP feature list as written — Archive was
never in scope, and reply needs a send scope that docs/SETUP.md explicitly says to add "only
when reply functionality exists," not ahead of it.

### minSdk 28 (Android 9) itself is unverified
All real-device/emulator testing so far (see "Verification summary" above) ran
on an API 36 emulator image, since that's what best represents "a modern device" and matches
`targetSdk`. Nothing in the codebase knowingly relies on an API newer than 28 without a guard
(`PdfViewerActivity`'s `parcelableExtra` helper branches correctly on API 33; `java.time` is
natively available since API 26), but that's a code-reading claim, not a claim of having run
the app on an actual API 28 device or emulator.
