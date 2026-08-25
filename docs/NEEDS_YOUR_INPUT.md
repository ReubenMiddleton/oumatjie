# Needs your input

A running queue of points where autonomous work hit something that genuinely needs the
project owner's decision, credentials, account access, or subjective judgment call — not
something to guess at. When work is paused for one of these, it's logged here with enough
context to resume immediately once it's resolved, and work continues elsewhere rather than
stalling. Newest first within each section.

## How to read this file

- **Blocked** — real work is ready to proceed the moment this is resolved; nothing else is
  waiting behind it structurally.
- **Resolved** — kept for history with the date and what was decided, not deleted, so the
  reasoning trail survives.

## Blocked

### The public repo currently has NO license — the drafted `LICENSE` file was never committed (found 2026-08-25)
Worth acting on sooner than the general "which license" question below, because the repo is
already public. `https://api.github.com/repos/ReubenMiddleton/oumatjie` reports
`"visibility": "public"` and `"license": null`, and the `LICENSE` file drafted in the previous
session exists on the dev machine only as an **untracked** file — it was never committed or
pushed. The practical effect: the code is publicly readable but, under default copyright, nobody
may legally reuse, modify, or redistribute it. That's the *opposite* of what an Apache 2.0 choice
was meant to achieve, and it's silently true right now.

This is a one-command fix once the licence choice below is confirmed — it is listed separately
only because "no licence at all on a public repo" is a different and more urgent state than
"deciding between two licences." **To resolve**: confirm Apache 2.0 (or name another), and the
existing `LICENSE` file gets committed and pushed. Committing it wasn't done unprompted, per this
project's standing "never commit without being asked" rule.

### Android `cmdline-tools` install, to build `compileSdk = 37` locally (new 2026-08-25)
Local builds currently need `compileSdk` temporarily lowered to 36, because this machine's Android
SDK only has the deprecated `tools/bin/sdkmanager`, which cannot see modern packages — full
diagnosis in `docs/DECISIONS.md`'s "Android SDK Platform 37 *is* published" entry and in
`AGENTS.md`. Everything still builds and all 52 tests pass with the workaround, so this is
friction rather than a blocker, but it will keep costing every local session a manual edit-and-
revert, and it blocks any emulator/TalkBack work against the real committed configuration.

**To resolve**: download Google's current `commandlinetools-win` zip and unpack it to
`C:\Users\reube\.bubblewrap\android_sdk\cmdline-tools\latest\`, then
`sdkmanager --install "platforms;android-37.0"`. Not done unprompted because it means downloading
and unpacking a toolchain onto your machine — say the word and it takes a few minutes. One thing
to check while doing it: the build actually asks for target `android-37.0-ext19`, and Google's
manifest doesn't obviously list an `-ext` variant for 37, so confirm that target resolves rather
than assuming `platforms;android-37.0` alone is enough.

### 10 open Dependabot PRs awaiting review (new 2026-08-25)
Dependabot went live this session (you added `.github/dependabot.yml` directly on GitHub) and has
already opened **10 PRs** — 6 GitHub Actions bumps and 4 Gradle dependency bumps, including
Kotlin `2.3.21 → 2.4.10` (both the compose and serialization plugins), OkHttp `4.12.0 → 5.5.0`,
and Compose BOM `2026.06.00 → 2026.08.00`. None have been reviewed or merged.

These aren't all equal risk and shouldn't be batch-merged: the Actions bumps are low-risk, but
the **Kotlin 2.4** and **OkHttp 5** jumps are majors that can break compilation, and OkHttp 5 in
particular touches the real Gmail networking path. Now that a local toolchain works, each can
actually be tested rather than merged on faith. **To resolve**: decide whether you want them
worked through — a reasonable order is the 6 Actions bumps first (low risk, immediate), then
Compose BOM, then Kotlin, then OkHttp last with the most attention.

### AI provider API key (updated 2026-08-24 — plumbing now built, just needs your key)
Scam/phishing detection and summarization (see [`docs/AI_ASSISTANT.md`](AI_ASSISTANT.md)) are
now fully implemented and wired up to call Anthropic's Claude Haiku 4.5 — the only thing missing
is your own API key. Both features stay completely off until you add one: with AI features off
(the default), the app behaves exactly like it did before this work existed. **To resolve**: get
an API key from [console.anthropic.com](https://console.anthropic.com) (Anthropic Claude was
chosen over Gemini specifically because Gemini's free tier may train on prompts and this is real
email content — see AI_ASSISTANT.md's "Provider recommendation" for the full reasoning), then
open the app's Settings screen, turn on "AI features" (you'll see a one-time plain-language
disclosure of what gets sent and to whom), and paste the key into the "Anthropic API key" field.
Realistic personal-scale cost is well under $1/month (see ROADMAP.md's pricing research). Without
a key, summarization still works using a clearly-labelled offline demo; the scam checker stays
off entirely rather than pretending to check with a fake heuristic (see DECISIONS.md for why that
one specifically doesn't get a demo fallback).

Calendar-aware reading, AI-flagged notifications, categorization, and the chat panel (features
5–8 in AI_ASSISTANT.md) are separate, larger pieces of work not touched this session — still
fully blocked/unbuilt, not just waiting on a key.

### Google Cloud project for real Gmail access
No Google Cloud project, OAuth consent screen, or registered Android OAuth client exists yet
(`docs/SETUP.md` §3). Nothing Gmail-shaped can be exercised against a real inbox until this
exists — it's been verified only against the offline demo inbox and hand-written fakes so far.
This requires the project owner's own Google account and Cloud console access; it can't be done
on their behalf. **To resolve**: work through `docs/SETUP.md` §3's checklist — note the OAuth
client should now be registered for package `com.oumatjie.app` (the app's `applicationId` as of
the 2026-08-24 Granify→Oumatjie rename), not `com.granify.app`; see SETUP.md §3 step 5 and
`docs/DECISIONS.md`'s rename entry for why those are two different, both-correct values.

### Play Store submission (updated 2026-08-24 — checklist ready, several steps need your accounts)
`docs/PLAY_STORE_READINESS.md` (new this session) lays out the full path to a public listing.
Two items on it specifically need the project owner and have long, unpredictable lead times worth
starting early rather than saving for last: **registering a Google Play Developer account**
(identity/organization verification can take days to weeks) and **the OAuth restricted-scope
verification + CASA security assessment** that `gmail.readonly`/`gmail.modify` both trigger
(routinely the longest step in Play Store prep for any Gmail-touching app, independent of the
app's own readiness). Separately, `docs/PRIVACY_POLICY.md` is a drafted, code-accurate first
pass written for oumatjie.com, but has not had a real legal review — that review, and then
actually hosting it live at a stable oumatjie.com URL, is real work only the project owner can
do or commission. **To resolve**: no single action closes this out — work through
`docs/PLAY_STORE_READINESS.md`'s "Suggested order" section, starting with Play Developer account
registration in parallel with finishing `docs/SETUP.md` §3.

### LICENSE choice (updated 2026-08-25 — recommendation made and LICENSE file drafted, still needs your confirmation)
The repo went public 2026-08-25, which makes this live rather than hypothetical. Asked directly
for a recommendation (not just a neutral comparison), given the project's actual shape — a real
Android app, built on Google/AndroidX conventions where Apache 2.0 is already the ecosystem norm,
with no stated need to force forks to stay open (which is what GPL would be for): **Apache 2.0**,
over MIT mainly for its explicit patent grant/termination clause (costs nothing, adds real
protection), over GPL because copyleft's main benefit — preventing a closed-source fork — doesn't
match a solo project that may want maximum flexibility later (dual-licensing, monetization,
whatever). A complete `LICENSE` file (Apache 2.0, copyright 2026 Reuben Middleton) has been
drafted and delivered — see `docs/LICENSE_COMPARISON.md` for the full side-by-side if you want to
weigh the copyleft trade-off yourself before confirming. **To resolve**: say the word if
Apache 2.0 is right, or which one you'd rather use instead and the file gets swapped.

## Resolved

### GitHub repo + Actions setup (resolved 2026-08-25)
The project owner ran all four account-side steps: `git init`/commit, `gh auth login` as their
personal account, `gh repo create oumatjie --private --source=. --remote=origin --push` (repo now
live at `https://github.com/ReubenMiddleton/oumatjie`), installed the Claude GitHub App scoped to
just this repo, and added `CLAUDE_CODE_OAUTH_TOKEN` as a repository secret. The three workflow
files (`ci.yml`, `claude.yml`, `claude-ci-watch.yml`) are live. The repo's very first CI run
immediately found a real bug — see `docs/DECISIONS.md`'s "First real CI run found a real bug
hand-review missed" — which is itself the clearest possible confirmation this was worth setting
up. See `docs/DECISIONS.md`'s "CI/CD and autonomous GitHub Action set up" entry for the full
setup reasoning.
