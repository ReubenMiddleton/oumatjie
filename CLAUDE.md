# CLAUDE.md

This file is read automatically by Claude Code (local) and by the `claude.yml`/`claude-ci-watch.yml`
GitHub Actions in this repo — keep it short; the real depth lives in the files it points to.

## Read first

Before doing anything else in this repo, read [`HANDOFF.md`](HANDOFF.md) in full — it's written
specifically to get a new session (human or Claude, local or in Actions) oriented in one read,
and links out to everything else (`docs/DECISIONS.md`, `docs/ROADMAP.md`, `docs/AI_ASSISTANT.md`,
`docs/DESIGN_SYSTEM.md`). Then read [`AGENTS.md`](AGENTS.md) for the standing working rules below
— they apply here too, not just to the session that wrote them.

## Standing rules (apply to every session, human-directed or autonomous)

- **Never commit to git without being explicitly asked**, except: `claude-ci-watch.yml` and the
  `@claude`-mention workflow are pre-authorized to commit and open pull requests as part of their
  own job — that authorization came from the project owner when this repo was set up (see
  `docs/DECISIONS.md`'s "CI/CD and autonomous GitHub Action set up"). **Neither workflow
  auto-merges** — always leave the PR for the project owner to review and merge themselves, even
  if CI is green.
- Log anything that genuinely needs the project owner's decision, credentials, or account access
  in `docs/NEEDS_YOUR_INPUT.md`, in the same format as the entries already there — don't guess at
  something that's actually their call.
- Record real decisions and hurdles in `docs/DECISIONS.md` as they happen, following that file's
  existing dated-entry format. This project's documentation discipline is what makes it safe for
  different sessions/tools to pick up the same work cold — don't let that lapse.
- Update `HANDOFF.md` (and this file, if something here goes stale) at the end of a session that
  changed enough to matter.
- Give honest technical opinions, including "no" or "this is a bad idea" — this project has
  explicitly asked for that, not agreement.
- Ground non-trivial technical claims in something checked (a doc, a real error, a source file),
  not assumption — several entries in `docs/DECISIONS.md` exist specifically because something
  assumed turned out wrong.
- The repo is meant to be public eventually — keep it clean and professional accordingly.

## What a local/Claude Code session can do that no session so far has been able to

Every session that built this project before 2026-08-25 ran in a cloud sandbox with **no
compiler, no Android SDK toolchain, no shell access to the project owner's machine, and a
network policy that blocks general GitHub access** — see `docs/DECISIONS.md`'s "Verification
summary" sections and "CI/CD and autonomous GitHub Action set up" entry for the full detail. That
means a large fraction of this codebase has only ever been "hand-verified," never actually
compiled — and the first time a real toolchain touched it (this repo's first GitHub Actions run,
2026-08-25), it immediately found a real bug hand-review had missed (`docs/DECISIONS.md`'s
"First real CI run found a real bug hand-review missed"). A local session removes all of those
constraints at once. In rough priority order, first things worth doing specifically because they
were never possible before:

1. **Run the real build and test suite**: `./gradlew testDebugUnitTest assembleDebug
   assembleRelease` and fix whatever a real compiler finds — this has never fully happened.
   Cross-check every "hand-verified, not compiled" claim in `docs/DECISIONS.md` against what
   actually happens.
2. **Run on a real emulator or device** and do a genuine TalkBack accessibility pass — flagged as
   an open gap since 2026-08-17; only ever addressed by static source read-throughs, never a real
   accessibility-service run.
3. **Work through `docs/SETUP.md` §3** with the project owner to register a real Google Cloud
   OAuth client, then exercise the Gmail integration against a real inbox for the first time.
4. Everything else in `HANDOFF.md`'s "Recommended next steps" — that list is still accurate and
   ordered; don't re-derive it from scratch.

## Local dev environment notes

Machine-specific tool paths (JDK location, Android SDK, emulator name, known Gradle/toolchain
gotchas already hit and fixed once) are recorded in `AGENTS.md` — check there before rediscovering
something already solved.
