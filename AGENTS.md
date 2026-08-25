# Agent notes

This file is for whoever (human or AI) is doing hands-on work in this repository. It's
different from the other documentation in kind, not just location: [`README.md`](README.md)
and [`docs/`](docs/) describe the *project*; [`HANDOFF.md`](HANDOFF.md) describes its *current
state*; this file describes how the person or agent doing the work should operate — machine
setup, standing preferences, and the update ritual that keeps all of the above trustworthy.

**Always read this file, [`HANDOFF.md`](HANDOFF.md), and whatever `docs/` files are relevant
before starting work in this repo.** Treat the documentation as load-bearing, not optional
background reading — decisions recorded in `docs/DECISIONS.md` and direction recorded in
`docs/ROADMAP.md` exist specifically so they don't have to be rediscovered or re-decided. If
something you're about to do contradicts what's written, that's worth noticing and resolving
deliberately (update the doc, or don't take the action), not overriding silently.

## Session ritual

This project is worked on across many separate sessions, often with no shared memory between
them except this repository. Two habits keep that working:

- **At the start of a session**, before making changes: check for relevant persisted context
  (a Claude session's own memory system, if available, plus this file and HANDOFF.md) rather
  than assuming the project's state from scratch or from what a prior conversation summary
  implied. Repo state is ground truth; anything remembered outside the repo should be verified
  against it, not trusted blindly.
- **At the end of a session — always when the user asks for a hand-off summary, and whenever
  else it's warranted** — update `HANDOFF.md` and this file to reflect what actually changed:
  new state, new decisions, new preferences learned, anything that would otherwise force the
  next session to reconstruct context the hard way. A hand-off summary given only in chat and
  never written down does not count as done.
- Keep `HANDOFF.md` and this file itself accurate "every so often," not only at explicit
  hand-off requests — if a session runs long and learns something future sessions will need,
  write it down before it's forgotten rather than waiting for the session to end.
- **When working autonomously and something needs the project owner's decision, credentials,
  or account access** (not just a judgment call you're empowered to make), don't stall on it:
  log it in [`docs/NEEDS_YOUR_INPUT.md`](docs/NEEDS_YOUR_INPUT.md) with enough context to
  resume immediately once it's resolved, and move on to other work. Do as much of the
  surrounding, non-blocked work as possible rather than waiting idle — see that file's "AI
  provider account and API key" entry for the pattern (build everything up to the blocked
  point behind a swappable interface, using a fake/demo implementation so it's still testable).

## This machine's environment

Specific to the primary Windows development machine this project has been built on so far
(username-bearing paths below) — useful to whoever is actually driving on that machine, not
necessarily portable to a different one. Worth genericizing or removing this section if the
repo is made public and machine-specific paths stop being appropriate to keep committed.

- Default JRE (also `JAVA_HOME`) is `C:\Program Files\Eclipse Adoptium\jre-21.0.11.10-hotspot`
  and has **no `javac`** — it's JRE-only, so any Gradle task needing real compilation fails with
  a `JAVA_COMPILER` capability error until a full JDK is pointed to explicitly. A working JDK 17
  is already installed at `C:\Users\reube\.bubblewrap\jdk17-x64\jdk-17.0.20+8`; pass it with
  `-Dorg.gradle.java.home="C:\Users\reube\.bubblewrap\jdk17-x64\jdk-17.0.20+8"` rather than
  changing any committed project file.
- An Android SDK already exists at `C:\Users\reube\.bubblewrap\android_sdk` (platform-tools,
  build-tools 34/35/36, platform 36, `sdkmanager` at `tools\bin\sdkmanager.bat`). Point a
  gitignored `local.properties` at it with `sdk.dir=C:\\Users\\reube\\.bubblewrap\\android_sdk`.
  `sdkmanager` can fetch missing platforms/build-tools/extensions on demand.
- **Corrected 2026-08-25 — the old "Platform 37 isn't published yet" note was wrong.** Google
  publishes `platforms;android-37.0` and `android-37.1` (confirmed against
  `https://dl.google.com/android/repository/repository2-3.xml`). The real problem is local: this
  SDK only has the **deprecated** `tools/bin/sdkmanager`, which speaks an old repository schema
  and cannot see modern packages — so `sdkmanager --list` reports them as missing and
  `--install "platforms;android-37.0"` fails with `Failed to find package`. **Do not trust that
  tool's `--list` output as evidence about what Google publishes.** It also requires an explicit
  `--sdk_root=C:\Users\reube\.bubblewrap\android_sdk`, or it fails with a bare
  `IllegalArgumentException: Could not create settings`.
  - Real local failure with the committed `compileSdk = 37`:
    `Failed to find target with hash string 'android-37.0-ext19'`. Note it wants the
    **`-ext19`** variant (from `compileSdkExtension = 19`), which Google's manifest does not
    appear to list for 37 — confirm that before assuming installing `android-37.0` fixes it.
  - **Until modern `cmdline-tools` is installed** (logged in `docs/NEEDS_YOUR_INPUT.md`), build
    locally with `compileSdk` temporarily lowered to 36 against the installed `android-36-ext19`,
    and revert before committing. This works — the full build and all 52 tests pass that way.
  - CI is unaffected and builds `compileSdk = 37` green, because `android-actions/setup-android`
    uses current cmdline-tools. A green CI run says nothing about this machine, and vice versa.
  - `detekt`/`ktlintCheck` run fine with `compileSdk = 37` locally — AGP only fails at
    task-dependency resolution for compile tasks. Don't read a green lint run as an SDK check.

- **Building on this machine**: the default `JAVA_HOME` is JRE-only (no `javac`), so always pass
  the JDK explicitly. The full working invocation:
  ```
  ./gradlew testDebugUnitTest assembleDebug \
    "-Dorg.gradle.java.home=C:\Users\reube\.bubblewrap\jdk17-x64\jdk-17.0.20+8"
  ```
  A cold `--no-daemon` build takes roughly 3–4 minutes.

- **Python, `uv`, and Graphify (added 2026-08-25)**: there is **no real Python** on this machine —
  `python`/`python3` resolve to the Microsoft Store stub and `pip` doesn't exist, so TOOLING.md's
  `pip install graphifyy` cannot work as written. Graphify is installed via `uv` instead and lives
  at `C:\Users\reube\.local\bin\graphify.exe` (on PATH after `uv tool update-shell`; restart the
  shell). Two Windows quirks if it ever needs reinstalling: `uv` fails with
  `Missing expected target directory for Python minor version link` unless you pass an explicit
  interpreter (`--python <path-to-python.exe>`), and its managed interpreters live under
  `C:\Users\reube\AppData\Roaming\uv\python\`.
  - Build the graph with `graphify . --code-only` — **the plain `graphify .` will fail** asking
    for an LLM API key, which this project deliberately does not have. Refresh after code changes
    with `graphify update .` (no API cost). Output lands in the gitignored `graphify-out/`.
  - `gh` is **not** installed system-wide; GitHub API queries in this project have been done with
    PowerShell's `Invoke-RestMethod` against `api.github.com`, which needs no auth for this
    now-public repo.
- A working AVD named `granify_test` already exists (API 36, `google_apis`, x86_64, `pixel_3a`
  profile) at `C:\Users\reube\.android\avd\granify_test.avd` — reuse it
  (`emulator -avd granify_test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect`)
  rather than creating a new one. Its name is a leftover from before the Oumatjie rename (see
  docs/DECISIONS.md) — renaming an AVD requires editing files on disk outside this repo, which
  no session so far has had the tooling to do; it's cosmetic only; a fresh `oumatjie_test` AVD
  can be created later if desired. WHPX hardware acceleration is confirmed working on this
  machine, so emulators run at normal speed.
- **If a new AVD ever needs creating**: `avdmanager create avd` on this machine writes a
  *doubled* relative path into the new AVD's `config.ini`
  (`image.sysdir.1=android_sdk\system-images\...` instead of `system-images\...`), which makes
  the emulator fail fast with `Cannot find AVD system path`. Fix by hand-editing that AVD's
  `config.ini` to strip the leading `android_sdk\` segment. Also, `avdmanager`'s `--sdk_root`
  flag must come *after* the verb (`avdmanager list device --sdk_root=...`), and this SDK's
  device-profile list only goes up to `pixel_xl`/`pixel_c` — use `pixel_3a` rather than guessing
  a newer profile name.
- **Driving an emulator reliably**: don't guess tap coordinates from a scaled screenshot.
  Instead: `adb shell uiautomator dump //sdcard/dump.xml && adb pull //sdcard/dump.xml <local>`
  (the doubled leading slash matters — Git Bash silently mangles a single-leading-slash device
  path into a Windows path), then read the dumped XML for the target `text="..."` attribute's
  `bounds="[x1,y1][x2,y2]"` and tap the midpoint. After a fresh install, wait for the UI dump to
  actually show the expected screen before tapping — a fixed short sleep right after cold start
  is not reliable.

## Working from a cloud/sandboxed Claude session (learned 2026-08-24)

If whoever is reading this is a Claude session running in a cloud sandbox (as opposed to one
with direct access to this machine's shell), two things are worth knowing before starting:

- **The sandbox itself typically has no network access to Maven Central, Google's Maven
  repository, or any package registry, and no way to invoke this machine's real
  Gradle/JDK/Android SDK toolchain.** Confirm this early (a quick `curl` to `repo1.maven.org`
  is enough) rather than assuming it and rather than repeatedly retrying a build that can't
  possibly succeed. If confirmed, no code written in that session can be compiled or run that
  session — say so explicitly in DECISIONS.md/HANDOFF.md rather than letting "hand-reviewed"
  and "verified" blur together (see DECISIONS.md's 2026-08-24 verification-summary addendum for
  the pattern to follow). Compensate by reading every existing, already-verified file a new one
  needs to pattern-match against before writing it, re-reading everything in full afterward, and
  — for any specific API whose exact surface is genuinely uncertain — fetching the real upstream
  source directly (e.g. `android.googlesource.com`'s raw file view, or a project's GitHub source
  tree) rather than trusting a Google search snippet or an AI-generated summary of one; API
  reference pages are frequently JS-rendered and return nothing useful to a fetch tool, but a
  `.kt`/`.java` source file's raw text usually isn't.
- **The device-bridge file-staging tool's upload cache can silently serve stale content.**
  Files staged into the session's uploads directory via the device-bridge file tool appear to
  land in a deterministic path derived from the common parent of the requested files, and that
  directory appears to persist and accumulate across multiple staging calls within one session
  rather than being freshly and exclusively populated each call. A blanket recursive copy out of
  that directory (e.g. to rebuild a local working mirror) can silently pull in content cached
  from much earlier in the session — even from before edits a *previous* session already made and
  committed to the real device — rather than the current state. This caused a real, fully-recovered
  data-integrity bug on 2026-08-24 (see `docs/DECISIONS.md`, "Stale device-bridge upload cache
  silently reverted several already-renamed files mid-session"). **Never trust a bulk copy out of
  that cache without first re-staging (or otherwise confirming the freshness of) every file the
  copy will touch** — the safe pattern is to stage exactly the files a step needs immediately
  before copying them, every time, rather than reusing whatever already happens to be there.
- **When the folders this machine has connected to the session aren't deep enough**, a
  cloud-session file tool may hit a folder-depth limit reaching into a nested path (this
  project's `app/src/main/java/com/granify/app/...` (the Kotlin package/namespace, unchanged by
  the Oumatjie rename — see docs/DECISIONS.md) is 8–10 folders below the repo root — deep
  enough to hit this in practice). Asking to re-grant access to the same or a deeper path did
  not help in one confirmed case; what worked was asking the person at the keyboard to use the
  Claude desktop app's own "Add folder" button to connect the deeper path directly as its own
  root — that establishes a new, shorter effective root, unlike re-granting.

## Working preferences

Learned across this project's build sessions; apply generally, not just to the task that
happened to reveal each one.

- **Read a multi-part request fully before acting on any part of it.** When asked to hold off
  and read something end-to-end before starting, that instruction is literal — don't act on the
  first actionable-looking sentence.
- **Precise scope adherence, especially on correction.** When scope is narrowed mid-task (e.g.
  "just do the documentation, not the implementation"), stop the now-out-of-scope work
  immediately rather than finishing "just this one piece" first. Keep harmless
  already-in-progress side effects (like a downloaded-but-unwired font file) rather than
  reverting them, but don't extend them further.
- **Ground design and technical claims in real research, not assumption or memory** — especially
  when the user's own framing includes a guess ("I think X has this feature"). Verify or correct
  it explicitly rather than quietly working around it. Cite what was actually checked.
- **Give an honest, direct technical opinion when asked for one**, including when it means
  gently disagreeing with an idea the user floated themselves (e.g. a framework/technology
  suggestion). The user explicitly invites this ("let me know what you think") rather than
  wanting reflexive agreement.
- **Document decisions, hurdles, and gaps as a standing practice, not a one-off task** — every
  non-obvious choice, workaround, or deliberately-deferred piece of work belongs in
  `docs/DECISIONS.md` (tactical/code-level) or `docs/ROADMAP.md` (strategic/design-level) as it
  happens, written for a future session with zero conversational context.
- **Keep the repository clean and professional at all times** — the user intends to make it
  public eventually, so documentation tone, structure, and repo hygiene (no stray files, no
  placeholder text, no exposed secrets) should already assume an outside reader, not just the
  user.
- **Don't take hard-to-reverse or destructive actions without being asked** — this project has
  gone multiple sessions without a single git commit; that's a deliberate reflection of "commit"
  being the user's call to make, not something to do proactively as a side effect of other work.

## Information to proactively provide

- When research informs a design or technical decision, say what was actually searched/checked,
  not just the conclusion — the user has asked for this explicitly and reads it as a sign the
  claim is trustworthy rather than assumed.
- When a plan changes because of new information (research, a correction, a discovered
  constraint), say what changed and why, not just the new state — see how `docs/ROADMAP.md`'s
  "final plan" section is written for the expected shape of this.
- Flag scope assumptions before acting on them if there's real ambiguity, but don't stall on
  small, reversible judgment calls — the user has explicitly granted discretion for those and
  wants to see the reasoning recorded, not be asked to approve every step.
- At any natural stopping point, a short, concrete "what's next" list is more useful to this
  user than a summary of what was just done — they consistently ask for next steps to be spelled
  out explicitly (see `HANDOFF.md`'s "Recommended next steps").
