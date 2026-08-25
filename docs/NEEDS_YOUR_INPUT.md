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

### LICENSE choice (updated 2026-08-25 — comparison doc ready, still your call)
No license file exists. The project owner has said they want this repo public eventually, which
makes this a real requirement, not a nice-to-have — but which license is a decision with real
consequences for how others can use the code (see `docs/DECISIONS.md`'s "No LICENSE file" gap).
A factual, non-recommending comparison of the two common permissive defaults for a project like
this is now ready: [`docs/LICENSE_COMPARISON.md`](LICENSE_COMPARISON.md) — MIT vs. Apache 2.0,
side by side, plus a brief note on GPL as a materially different (copyleft) third option. **To
resolve**: read the comparison and say which one (or something else entirely).

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
