# Tooling: Graphify and repo-hygiene candidates (researched 2026-08-25)

Written in response to a direct request to look into Graphify and scout adjacent tooling "for
future repo hygiene... that will work well with claude code and graphify," to be handled once a
local/Claude Code session exists. Nothing in this file has been installed or wired into the repo
— this cloud sandbox can't actually reach any package registry (see "Why nothing here was
installed this session" below), so everything below is research and a recommended order, for a
local session to execute.

## Graphify — read the domain warning before doing anything else

**The tool the request pointed at, `graphify.net`, is not the official project.** The real
project's own official site says so directly, on a page it built specifically to address the
confusion:

> "graphify.net is not affiliated with or operated by Graphify Labs... not an official source of
> Graphify software, documentation, or support... Content published there is not written,
> endorsed, or reviewed by Graphify Labs."
> — [graphify.com/graphify-net-vs-graphify-com](https://graphify.com/graphify-net-vs-graphify-com)

That the official project felt it necessary to publish a dedicated disambiguation page is itself
worth noting — it means the confusion is real and apparently common enough to address head-on.
**If Graphify gets set up here, it should be from the official source only**, cross-checked
against all three of:
- Official site: [graphify.com](https://graphify.com/)
- PyPI package: [`graphifyy`](https://pypi.org/project/graphifyy/) — note the double "y"; there is
  no single-y `graphify` package to worry about mixing up
- GitHub: [github.com/Graphify-Labs/graphify](https://github.com/Graphify-Labs/graphify)

Never `graphify.net`, and never anything a search engine surfaces that doesn't match those three.

### What it actually is, and what it needs

An open-source (dual MIT/Apache-2.0) local CLI, not a hosted service. It parses a repo (code via
tree-sitter, docs/PDFs/images via an LLM your AI assistant is already configured with) into a
knowledge graph — `graph.html`, `GRAPH_REPORT.md`, `graph.json` — that Claude Code can then query
via a `/graphify` skill instead of grepping the whole codebase for context. Per its own docs it
runs **entirely on-device, sends only extracted semantic descriptions (never raw source) to
whatever model your assistant already uses, and needs no API key of its own** — it rides on
Claude Code's existing credentials rather than asking for new ones, which fits this project's
credential-handling rule (nothing new to hand it, nothing new for the user to type in).

Install (once there's a local session with real PyPI access):
```
pip install graphifyy && graphify install
```
This drops a skill file at `~/.claude/skills/graphify/SKILL.md`. From there, `/graphify .` inside
Claude Code builds the graph for this repo. Requires Python 3.10+, which AGENTS.md's machine notes
should already cover — check there first.

### Honest assessment, not just a description

This looks like a real, legitimate, actively-developed project — not a scam — based on multiple
independent signals: a named founder (Safi Shamsi) with a public LinkedIn/X presence, a real
[Y Combinator company page](https://www.ycombinator.com/companies/graphify-labs) (batch: Summer
2026, founded 2026, 2 people, London), a proper dual open-source license, and a level of
implementation detail (tree-sitter AST parsing, Leiden community detection) that a pure marketing
site wouldn't bother with.

One number is worth flagging rather than repeating uncritically: both graphify.com and its YC
page claim **105K+ GitHub stars**. This session could not independently verify that figure —
`api.github.com` is blocked by this sandbox's network policy (same restriction already documented
elsewhere in this project for general GitHub access), so the only sources for the number are the
project's own site and its own YC profile, which YC company pages don't independently audit.
A 2-person company, founded this year, with a PyPI package whose latest release is dated
**2026-08-24 — yesterday, relative to this research** claiming that star count is a large enough
claim, from a source that can't check its own homework, that it's worth a five-second sanity check
(`gh repo view Graphify-Labs/graphify` or just opening the repo page) before treating it as fact,
rather than either dismissing it or taking it at face value.

None of that changes the recommendation, though: the tool's actual footprint — local-only,
read-only against this repo, no new credentials, easy to `pip uninstall` — is low-risk regardless
of whether the star count holds up. **Recommended**: try it, from the official source, as a local
Claude Code convenience. Not recommended yet: Graphify Labs' paid tiers, which per graphify.com
review pull requests and want write access to a repo — there's no reason for a solo project this
size to hand a five-week-old company that kind of access, official or not.

### Why nothing here was installed this session

This cloud sandbox's network policy blocks PyPI the same way it already blocks general GitHub
access and `apt` (see `docs/DECISIONS.md`'s CI/CD entry for the precedent) — confirmed directly:
`pip install` inside a throwaway venv here returns `No matching distribution found`, and
`curl -I https://pypi.org/simple/graphifyy/` returns `403 host_not_allowed`. Setup has to happen
in a local/Claude Code session with real network access, same constraint that shaped the GitHub
Actions setup.

## Complementary repo-hygiene tooling

Scanned for things that pair well with Claude Code and don't carry the same "brand new, unverified"
profile as Graphify — all of the below are long-established, widely-used projects, not scouted for
novelty. Ordered by how much value they add relative to effort for a solo project of this size and
age, not alphabetically.

### 1. Dependabot version updates (free, native, no new tool to trust)
GitHub's own dependency-update bot, free on every plan and every repo visibility — nothing to
install, no separate account, no license question. For a Gradle/Kotlin project like this one, a
`.github/dependabot.yml` with a `package-ecosystem: gradle` entry opens a PR whenever a dependency
(Retrofit, OkHttp, Compose, etc.) has an update — exactly the kind of thing that's easy to fall
behind on silently. Dependabot *security alerts* (as opposed to version-update PRs) are also free
regardless of repo visibility and are worth turning on in repo Settings → Security even before the
config file exists.

### 2. gitleaks (free, open source, works on a private repo without a paid plan)
GitHub's own secret scanning is free for public repos but requires a paid GitHub Advanced Security
license for private ones (confirmed via GitHub's billing docs) — and this repo is private for now.
[gitleaks](https://github.com/gitleaks) is the well-established free alternative: a GitHub Action
that scans every push/PR for accidentally-committed API keys, tokens, and credentials before they
land in history. Directly relevant here given this project already handles an Anthropic API key
and Google OAuth client details. Worth adding as a fourth workflow file once there's a local
session (this cloud sandbox's `.github/workflows/` write restriction applies to this too, same as
`ci.yml`/`claude.yml` earlier this session).

### 3. detekt + ktlint (free, open source, Kotlin-specific)
Static analysis and formatting-consistency for Kotlin specifically, not a generic linter bolted
on — catches real issues (unused code, complexity, common Kotlin footguns) `ci.yml`'s current
`testDebugUnitTest`/`assembleDebug` steps don't. Both are Gradle plugins with a well-worn
GitHub Actions pattern (add the plugin, add a `./gradlew detekt ktlintCheck` step to `ci.yml`).
Reasonable to add as an extra step in the existing `ci.yml` rather than a new workflow file.

### 4. CodeQL — free later, not now
GitHub's own semantic code-scanning (finds real security bugs, not just style issues) is free for
public repos but, like secret scanning, requires paid Advanced Security for private ones. Not
worth paying for at this project's current size — but worth remembering as a zero-cost add the
day this repo actually goes public (the LICENSE decision in NEEDS_YOUR_INPUT.md is the blocker for
that, not a technical one).

### Suggested order for the local/Claude Code session

1. Graphify, from the official source only (`graphify.com`/`graphifyy`/`Graphify-Labs/graphify`) —
   flagged as the priority by the project owner, and the lowest-risk of everything here since it
   never touches the repo's git history or CI.
2. Dependabot config — five minutes, free, no new trust decision to make.
3. gitleaks workflow — closes a real gap (no secret scanning at all right now on a private repo).
4. detekt/ktlint — real value, but lower urgency than the above two; fine to batch with other
   `ci.yml` changes.
5. CodeQL — revisit once the repo is public, not before.
