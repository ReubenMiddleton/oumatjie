# Roadmap

Where DECISIONS.md is the tactical log ("why this line of code is this way"), this document
is strategic: a retrospective on the project so far, the design for where it's headed next
(the AI assistant, in particular), and the research behind both. Read HANDOFF.md first if
you're picking this project up cold — it points here for the parts that matter most.

## Implementation status (2026-08-25 update)

The 2026-08-17 sessions below produced this document as a *design plan* — at that point nothing
in it was built. A first 2026-08-24 session implemented most of "Design direction: final plan"
and AI_ASSISTANT.md's features 1–4. A second 2026-08-24 session renamed the project to Oumatjie,
formalized the design system, and did a first production-readiness pass. A 2026-08-25 session
(working autonomously on self-selected, no-input-needed follow-ups while the project owner was
away) did a static accessibility audit and built categorization's Tier 1. This box is the current
status; the sections below are kept as-written (the reasoning trail), not edited to read as if
they always described finished work.

- **Typography** — done. Atkinson Hyperlegible wired into every `Typography` style and the
  classic-View theme. See DECISIONS.md.
- **Button hierarchy** — done. Hero/Standard/Tertiary tiers, as specified below.
- **Motion, haptics** — first pass done: haptic tick on every button tap, a fade-in/out on inbox
  list items. **Sound as a feedback channel** — evaluated 2026-08-25 (see DECISIONS.md); decision
  recorded, not built this pass.
- **Visual identity — shape** — done (second 2026-08-24 session): a deliberate two-radius shape
  system (`docs/DESIGN_SYSTEM.md`, "Shape"; `docs/DECISIONS.md`'s design-system entry) replaces
  what had drifted into four independently hardcoded corner-radius values. **Color-as-signal
  beyond what already existed** is still open — `InfoCardTone`'s three tones (Neutral/
  Highlight/Problem) formalize the color roles that already existed, but no new signaling use of
  color was added this pass.
- **Jargon copy-editing pass** — done (reviewed, no changes needed — see DECISIONS.md).
- **TalkBack** — real device/emulator pass still never run (unchanged gap; see below). A static
  read-through audit (2026-08-25) substituted as far as a sandbox without device access can: found
  and fixed a color-only unread signal and added heading navigation across every screen. See
  DECISIONS.md's "Static accessibility audit" entry.
- **AI assistant features 1–4** (first-contact flagging, scam warning, read-aloud,
  summarization) — done. **Feature 7 (categorization)** — Tier 1 (no-AI local rules) done
  2026-08-25; Tier 2 (AI-assisted) and the rename/merge UI still not implemented. **Features 5,
  6, 8** (calendar-aware reading, AI-flagged notifications, chat panel) — **not implemented**,
  deliberately scoped out; see DECISIONS.md's "This session scoped down to AI_ASSISTANT.md
  features 1–4" and "Tier 1 (no-AI) mail categorization implemented" entries for why, and
  AI_ASSISTANT.md's own status header for feature-by-feature detail.
- **Session persistence** — done (see the retrospective entry immediately below, and
  DECISIONS.md).
- **Rebrand to Oumatjie** — done (second 2026-08-24 session). See DECISIONS.md's rename entry.
- **Design system documentation** — done (second 2026-08-24 session): `docs/DESIGN_SYSTEM.md`.
- **Splash screen** — done (second 2026-08-24 session). See DECISIONS.md.
- **Home-screen widget (Glance), static App Shortcuts** — considered, deliberately not built this
  pass; see this document's own sections below and "Considered and deliberately not built" above.
- **Privacy policy, Play Store readiness checklist** — first drafts done (second 2026-08-24
  session): `docs/PRIVACY_POLICY.md`, `docs/PLAY_STORE_READINESS.md`. Both need real-world
  follow-through (legal review, account registration, verification processes) that only the
  project owner can do — see `docs/NEEDS_YOUR_INPUT.md`.

None of either 2026-08-24 session's new code has been run through an actual compiler or
emulator — see DECISIONS.md's "Verification summary (session 2026-08-24 addendum)" for what that
means and what to check first.

## Retrospective (2026-08-17)

A critical pass over the first two build sessions, looking for places discretion calls should
have gone differently, before making any new ones. Not a list of bugs — those are fixed and in
DECISIONS.md. This is about judgment calls that were defensible at the time but are worth
revisiting now that more of the app actually works and has been run for real.

### Worth reconsidering now

**Session persistence was deferred too readily.** The original reasoning (docs/DECISIONS.md
carries the earlier note) was that persisting a signed-in session was "realistically its own
small feature." In hindsight, that underestimated how cheap it actually is given how the auth
layer turned out: `GoogleAuthManager.authorize()` already silently re-grants already-approved
scopes with no UI, so "persistence" mostly means remembering *that* the user signed in before
(a boolean + email, not a token) and letting `SessionViewModel` attempt a silent re-authorize
on cold start before falling back to the sign-in screen. That's a much smaller feature than it
looked like when the auth layer was still theoretical. For this specific audience — people who
may already find login flows stressful — being asked to sign in again every single time the
app is closed and reopened is a real, repeated cost, not a minor rough edge. This is now
addressed; see "Session persistence" under Decisions below.

**Button sizing had no hierarchy.** Every primary action button was a uniform 64dp regardless
of how important or frequent it was — "Continue with Google" and "Get help" read as equally
prominent. A flat minimum is the right accessibility *floor*, but visual hierarchy through size
is itself an accessibility tool: it tells a user where to look first without them having to
read and compare every option. Addressed by introducing an explicit size tier system — see
"Button hierarchy" below.

**The visual design leaned on Material3 defaults more than it should have.** Color and type
*scale* were customized and contrast-verified, but shape language (one corner radius used
everywhere), typography (the system default font, never reconsidered), and decorative/illustrative
moments (empty states and success states are plain text, nothing else) were all left at
framework defaults. That reads as "a competent default Material app," not as a considered,
opinionated product — which is a fair characterization of how it looked before this pass.
Addressed under "Visual identity" below.

**Zero motion beyond screen transitions, zero haptics, sound never considered.** The reduced-motion
crossfade added in the production-quality pass was the *only* motion in the entire app —
no feedback on button press, no entrance animation for list items, no acknowledgment moment
for a successful action beyond a snackbar. Haptic feedback (`LocalHapticFeedback` in Compose)
was never used at all, despite being a strong candidate for this audience specifically — see
the research section below for why. Sound was never evaluated one way or the other, which is
its own gap: a considered "no" is fine, but "never considered" isn't the same thing.

**TalkBack itself was never actually tested.** The accessibility work verified contrast ratios
and text-scaling layout robustness rigorously, but docs/PRODUCT_PRINCIPLES.md explicitly names
"TalkBack semantics" as a requirement, and no pass of this project has turned on TalkBack and
listened to how the app actually reads. Compose's default semantics are usually reasonable, but
"usually reasonable" and "verified" are different claims, and this project has otherwise held
itself to verifying claims rather than assuming defaults are fine. Flagged as an open gap below
rather than closed in this pass — it needs a real device or a specific emulator accessibility
workflow, not just more code reading.

**docs/PRODUCT_PRINCIPLES.md already anticipated AI features, and nothing surfaced that.** It
was written before any of this session's work and already says: "AI features must be optional,
explicit, and unable to send mail or follow links without confirmation." Nobody involved in
architecture work up to this point (including the very first scaffolding pass, before this
session existed) called that out as a live constraint waiting for a feature. Lesson for next
time: read every existing doc fully for forward-looking hooks, not only the parts relevant to
whatever's being built right now — a principles doc is often a roadmap in disguise.

### Held up fine on reflection

**Manual DI over Hilt.** Still the right call for the current object graph size. Worth a
concrete revisit trigger rather than an open-ended "maybe someday": if the AI assistant work
below adds more than two or three more long-lived singletons with real scoping needs, revisit.
Not there yet even with the assistant scaffolding added in this pass.

**Deferring Archive, reply/compose, and search.** Archive and reply are still correctly out of
scope (matches the documented MVP feature list and the scope-add rule for `gmail.send`).
Search deserves a specific note: it was never built and was never flagged as a gap either,
which in hindsight was the right instinct even if it wasn't deliberate — typing a search query
on a phone keyboard is exactly the kind of interaction this app's whole premise is designed to
avoid needing. If search ever gets built, it should probably not be a text field at all (see
the AI assistant section — "ask the assistant" is a more plausible interaction than typing
keywords for this audience).

**The PDF viewer toolbox FAB left unresolved.** Still a reasonable place to have stopped;
nothing in this pass's research changed that assessment (see "AndroidX PDF library" in the
research section).

---

## Design direction: initial instincts, before research

Captured deliberately *before* the research pass below, so the reasoning that led here is
visible rather than silently folded into whatever the research turned up. The "Design
direction: final plan" section further down is what actually reconciles this against research
and is what a future session should build from — treat this section as "why," that one as
"what," if they ever seem to disagree.

**Typography.** The app has never reconsidered the system default font. A specific, deliberate
choice: **Atkinson Hyperlegible**, designed by the Braille Institute of America specifically
for low-vision and aging readers (distinct letterforms for commonly-confused characters —
`I`/`l`/`1`, `0`/`O`), OFL-licensed (free to bundle). This is a rare case where "more
accessible" and "more considered/less generic" point at the exact same choice rather than
trading off against each other — a generic warm/rounded font (Nunito, Quicksand) would help
the "looks AI-generated" complaint without the accessibility research behind it; Atkinson
Hyperlegible does both. **Already downloaded** (OFL-licensed, verified) to
`app/src/main/res/font/atkinson_hyperlegible_{regular,bold,italic,bolditalic}.ttf` with the
license text at `app/src/main/assets/licenses/atkinson_hyperlegible_OFL.txt` — ready to wire
into `ui/theme/Theme.kt`'s `Typography`, not yet done. This should be double-checked against
research (below) rather than assumed correct.

**Button hierarchy.** Every primary button is currently a uniform 64dp regardless of
importance — flat, safe, but no hierarchy. Instinct: a three-tier system —
*Hero* (~80dp, boldest, the one primary forward-moving action per screen — "Continue with
Google," "Done reading," "Open document"), *Standard* (64dp, current default — secondary but
real actions, e.g. "Try the demo inbox," "Move to Trash" deliberately *not* hero since it's
destructive and shouldn't invite fast taps), *Tertiary* (~56dp, text-style — low-stakes/always-available
navigation like "Back to your mail," "Get help," dialog "Cancel"). The existing deliberate
choice to keep "Move to Trash" visually lighter than "Done reading" already matches this
system's spirit; formalizing it as named tiers just makes that pattern consistent and
intentional everywhere instead of accidental in one place.

**Motion, haptics, sound.** Currently: one crossfade (navigation) and nothing else — no button
press feedback, no list entrance animation, no haptics anywhere, sound never evaluated. Instinct
going in: haptic feedback (`LocalHapticFeedback` in Compose) is probably a *better* first
investment than sound for this specific audience, since age-related hearing loss is common and
a phone's tap-back vibration doesn't ask anything of hearing — sound should probably be a
secondary/optional layer on top, off by default or very gentle, never the only channel a
confirmation is communicated through (the app already doesn't do this — Trash's confirmation is
a visible snackbar, not implied by a sound — and that pattern should hold for whatever gets
added). This needs the research pass, not just this instinct, before committing to specifics.

**AI assistant.** The one part of this pass that has a real external unknown before any design
decision is final: what "free" actually means for an LLM API suitable for a personal-scale app
like this. Not resolved by instinct — see the research section for the actual landscape check
this needed. Architecturally, though, one thing doesn't depend on that answer:
docs/PRODUCT_PRINCIPLES.md's existing constraint ("AI features must be optional, explicit, and
unable to send mail or follow links without confirmation") should be the *load-bearing* design
constraint for all of it, not a footnote — every capability below should be designed so a user
who never opens the assistant panel gets an app that behaves identically to one that was never
built. That means: no AI call ever fires without the panel being explicitly opened or a
notification explicitly enabled; nothing the assistant "notices" changes what's shown in the
inbox unless the user turned that specific behavior on; and any categorization or summarization
is described to the user in plain terms before it's turned on, not silently applied.

---

## Research findings (2026-08-17)

Real web research, not assumption — done specifically to check the instincts above rather than
to browse generally. Organized by topic; every claim below came from a search, not memory.

### Senior UX design — the fundamentals check out, with one new lesson

General guidance (Toptal, NN/g, Eleken, Cyces, and others) converges on what this project
already does: large text (16px+ minimum — Oumatjie's smallest body text is already 20sp, well
above the floor), high contrast (this project's contrast work already targets WCAG AAA, above
the 4.5:1/3:1 AA floor these sources cite), simple/shallow navigation, one primary decision per
screen, plain-language confirmations after every important action, and gestures kept to simple
taps (no multi-finger or fast-swipe requirements — Oumatjie has never required either). Apple's
documented minimum touch target is 44×44pt (~9.6mm); Oumatjie's 48dp absolute floor / 64dp
default already clears this with real margin. None of this changed anything — it confirmed the
existing direction was already right, which is itself a useful, if less exciting, finding.

**The one new lesson**: Nielsen Norman Group's guidelines specifically flag *unexplained
jargon* — even words like "page" or "website" tripped up study participants. Worth a
copy-editing pass over Oumatjie's own strings for hidden assumptions (a few candidates worth
checking, not yet confirmed problems: "sync," "cache" never appears in user-facing text
already — good — but "attachment," "scope," and "Trash" as a Gmail-specific concept vs. a
real-world metaphor are worth a second look during the next copy pass).

### Material 3 Expressive — the right instinct, wrong dependency

Google's own research (46 rounds, ~18,000 participants, announced Google I/O 2025) was aimed
at exactly the "why do all our apps look the same" problem this pass is trying to solve for
Oumatjie specifically — richer/varied shapes, deliberate "springy" motion, and color used as a
communication tool rather than uniform branding. That's a strong external validation of the
direction in "Button hierarchy" and "Visual identity" above.

**But**: as of this research (2026-08-17), Expressive's actual components live behind
`@ExperimentalMaterial3ExpressiveApi` in `material3:1.5.0-alpha`; the stable line is
`1.4.0` without them. Given this project already carries real alpha-library risk in exactly one
place (`androidx.pdf`, and that one *bit* — see DECISIONS.md's two PDF crash entries) and that
was scoped to a single screen, taking a *second* alpha dependency on something that touches
literally every screen (the whole theme system) is a materially bigger risk for a much less
essential reason. **Recommendation: adopt the design philosophy on the existing stable
Material3 (shape variety, deliberate motion, color-as-signal, done by hand with stable APIs),
do not take the `1.5.0-alpha` dependency.** Revisit once Expressive graduates to the stable BOM
line — it's real, funded, Google-priority work, so this is a "when," not "if."

### AI API landscape — "free" is real, but not unconditionally

- **Google Gemini** (`gemini-2.5-flash` / `flash-lite`) has a genuine free tier with no credit
  card required: **1,500 requests/day, 15 RPM, 1M TPM**, and it doesn't expire. For scale: even
  a heavy day of 40 emails, each triggering one AI call (a scam-check *and* a summary), is 80
  requests — 5% of the daily ceiling, for one user. **The catch that matters for an email app
  specifically: Google's terms allow free-tier prompts to be used for model training.** Sending
  someone's grandmother's actual email content — a bank statement, a family email — into a
  pipeline that trains a model on it is a real conflict with this project's privacy stance, not
  a hypothetical one. The paid tier and Vertex AI explicitly do not train on prompts; the free
  tier explicitly may. This needs to be surfaced to the user in plain language before any
  Gemini free-tier option is enabled, not buried in a settings toggle's fine print — see the
  design principle in AI_ASSISTANT.md.
- **Anthropic Claude** has no free API tier, but Haiku 4.5 is cheap enough that realistic
  personal-scale use (per the same 80-requests/day estimate, mostly short prompts) would run
  well under a dollar a month. No free-tier-training caveat applies the same way (Anthropic's
  standard API terms don't train on customer prompts by default).
- **On-device vs. cloud is a real, well-documented spectrum, not a binary choice** — the
  consistent recommendation across sources is a *hybrid*: cheap/fast/private on-device
  processing (rule-based or a small local classifier) for simple, high-volume tasks, reserving
  cloud LLM calls for the cases that actually need real reasoning. This directly validates
  treating "smarter AI categorization" as an optional *upgrade* layered on top of a working,
  free, on-device baseline — not the only way categorization can work at all. See
  AI_ASSISTANT.md for how this becomes the two-tier categorization design.

### Haptics, sound, and hearing loss — confirms the instinct, with real numbers behind it

Roughly **1 in 3 adults aged 65–74 have some hearing loss** (and the proportion rises with age
past that), so any design that communicates something *only* through sound is, for a
meaningful fraction of this exact audience, communicating nothing. The consistently repeated
best practice across sources: every audio cue needs a visual or haptic equivalent, never the
reverse-only. This confirms treating haptic feedback as the more foundational channel and sound
as a secondary, optional, user-controllable layer on top — never the sole confirmation that an
action happened. It also reinforces something Oumatjie already does by accident rather than
design: Trash's confirmation today is a visible snackbar, not a sound — that pattern should be
the template going forward, not the exception.

### Phishing and scams — the single highest-value thing an "AI assistant" could do

This is the most consequential finding of the whole research pass, and it reframes the AI
assistant's priority order. Adults 60+ filed **over 201,000 fraud complaints in 2025 with
losses topping $7.7 billion** (a 59% year-over-year increase in losses); **phishing/spoofing
was the single most common fraud type reported by seniors** (roughly a quarter of all senior
fraud complaints). This isn't just "seniors are targeted more" — there's peer-reviewed research
specifically showing **older age correlates with measurably worse ability to distinguish a
genuine email from a phishing one**, i.e., this isn't a volume problem alone, it's a detection
problem this exact audience has more than most. docs/PRODUCT_PRINCIPLES.md already commits to
"calm, factual warnings" for unusual requests — that principle currently has no implementation
behind it anywhere in the app. Given everything else an "AI assistant" could plausibly do
(categorize mail, summarize, chat), **this is the one with a real, quantified, mission-critical
case for going first** — see AI_ASSISTANT.md's proposed feature ordering, which was reordered
specifically because of this finding (it was not the first idea originally).

One directly-relevant, non-AI-dependent pattern found in the same research: Microsoft Exchange's
"First Contact Safety Tip" flags the *first* email ever received from a given sender with a
brief, calm note. This is a proven, enterprise-grade pattern for exactly the "unknown sender"
half of the existing product principle, and — critically — **it needs no AI or API call at
all**: "have I seen this address before" is a simple local lookup against the messages already
fetched. This is the "simpler compromise" version of categorization the user explicitly said
would be an acceptable fallback if true AI categorization proved too complex — except here it's
not a fallback, it's arguably the better *first* feature regardless of whether AI ever gets
added at all.

### Calendar, notifications, read-aloud — the platform APIs involved

- **Calendar**: Android's own guidance is usually "don't request `READ_CALENDAR`, hand off to
  the Calendar app via an intent instead" — but that guidance is written for apps that want to
  *add* an event (a one-off handoff works fine for that). Oumatjie's actual ask — "tell me if
  I'm free on the date this email mentions, without leaving the message" — is a *query*, which
  intents can't do; there's no "ask the Calendar app whether I'm busy on X and hand the answer
  back" intent. Reading calendar data for this feature genuinely does need the real
  `READ_CALENDAR` permission and `CalendarContract` queries, which is a meaningfully more
  sensitive permission than anything Oumatjie currently requests (today: only `INTERNET`).
  Two hard implementation details worth remembering: the permission **must be checked before
  every single calendar read**, not just once at grant time, since the user can revoke it from
  system settings at any moment; and it should be requested only at the moment the user
  explicitly turns the feature on, matching the existing "just in time" principle exactly.
- **Notifications**: Android 13+ requires the runtime `POST_NOTIFICATIONS` permission, and new
  installs default to notifications *off* until it's granted — there is no scenario where
  Oumatjie could show a notification without an explicit permission prompt first, which
  conveniently makes "notifications are opt-in" the *only* possible behavior on current Android
  versions, not just a design choice. Best practice is to request it contextually (when the
  user turns on the specific feature that needs it), which is, again, exactly the existing
  "just in time" principle already written down.
- **Read-aloud**: `android.speech.tts.TextToSpeech` ships in the Android SDK itself — no new
  dependency, no network call, works offline. This isn't a novel idea for this audience either:
  it's the exact pattern Lively/Jitterbug devices already ship as a named feature ("read-out"),
  and dedicated apps (Voice Aloud Reader and others) already do this for email specifically.
  Given zero new dependencies and a proven audience fit, this is one of the cheapest
  high-value features on the whole list — see AI_ASSISTANT.md's feature ordering (it's not
  actually an "AI" feature at all, and doesn't need to wait for any AI work to land first).

### A caregiver/family visibility feature is a real, established pattern — approach with care

Multiple shipping apps (Parents Are OK, StillAlive, AllsOK, and others) exist purely to give an
adult child peace of mind about an elderly parent, via either passive inactivity monitoring or
an explicit daily one-tap check-in. The explicit-tap pattern is the better fit for Oumatjie's
existing ethos (docs/PRODUCT_PRINCIPLES.md's emphasis on dignity and not treating age as a
single disability) — passive monitoring reads as surveillance, an explicit daily action a user
chooses to take does not. This is flagged as a real, validated idea worth having in the
long-term idea list (see "Longer-range ideas" in AI_ASSISTANT.md), but it's the single feature
on this whole list with the least design precedent *inside* Oumatjie's own current architecture
(it implies a second person's identity, a second device or channel, and a consent flow more
elaborate than anything else in the app) — not a near-term build.

### Other concrete, low-effort ideas the research surfaced

- **A home-screen widget showing unread count / latest sender**, built with Jetpack Glance
  (a real, Compose-like, actively maintained library for exactly this). Directly serves the
  stated goal of the user needing to interact with the app as little as possible — a glance at
  the home screen can answer "do I have new mail" without opening anything.
- Cloud AI email-summarization products broadly get criticized in privacy writeups for vague
  data-handling disclosures and continuous background access to a user's full mail corpus. The
  clear lesson for Oumatjie's design, beyond the Gemini-training point above: **AI processing
  should be triggered per-message, by an explicit user action on that specific message** (open
  the assistant panel, tap "check this email" or "summarize this"), never a standing background
  process scanning the whole inbox. This is now a hard requirement in AI_ASSISTANT.md, not a
  suggestion.

---

## Design direction: final plan (2026-08-17)

This section reconciles the pre-research instincts above against the research findings above
into one authoritative specification. Where research confirmed an instinct, it's restated
briefly with the evidence attached. Where research changed something, the change and the reason
are both spelled out. Future sessions should treat *this* section as the current source of
truth for design direction, and the two sections above it as the reasoning trail that produced
it — not as competing or outdated instructions.

**Typography.** Unchanged: Atkinson Hyperlegible, already downloaded to
`app/src/main/res/font/`, ready to wire into `Theme.kt`. Research added one new to-do rather
than changing the plan: a copy-editing pass for hidden jargon (NN/g's finding, above) should
happen alongside the font work, since both touch the same surface area — every user-facing
string. Candidates already flagged for a second look: "attachment," "scope," "Trash" as a
Gmail-ism rather than a real-world metaphor.

**Visual identity.** Confirmed and sharpened: lean on Material 3 Expressive's *philosophy*
(deliberate shape variety, springy motion, color used to communicate rather than just brand)
using stable, non-experimental Material3 APIs — not the `1.5.0-alpha` dependency itself. This
project already paid a real cost for one alpha dependency (`androidx.pdf`, see DECISIONS.md);
Expressive's alpha status makes it the same trade again, but bigger, since it touches every
screen instead of one. Revisit when Expressive ships in a stable BOM line.

**Button hierarchy.** Unchanged from the initial-instincts section (Hero ~80dp / Standard 64dp
/ Tertiary ~56dp) — general senior-UX research confirmed the underlying reasoning (44×44pt
Apple's documented floor, Oumatjie's 48dp floor already clears it) without suggesting a
different structure. No change.

**Motion, haptics, sound.** Confirmed and given a harder edge: roughly a third of adults 65–74
have some hearing loss, rising with age past that, so sound can never be the *only* channel for
a piece of feedback — haptics or a visual change must always carry the same information sound
does. This was already the instinct (haptics-first, sound secondary); research turned it from
"seems right" into "measurably necessary for a meaningful fraction of this exact audience."
Trash's existing visible snackbar-without-sound confirmation is the template to repeat, not an
accident to fix.

**AI assistant priority order — changed by research, this is the one real pivot.** The original
framing (from the user's request) led with categorization and a chat-style assistant, with
scam/notification warnings mentioned alongside. Elder-fraud research reorders this: phishing is
the single most-reported fraud type for seniors, seniors are measurably worse at detecting it
than other age groups, and docs/PRODUCT_PRINCIPLES.md already promises "calm, factual warnings"
for exactly this case with nothing yet built behind that promise. **Scam/phishing detection
moves to the front of the queue, ahead of categorization and ahead of any chat interface.**
Full reasoning, feature ordering, and architecture now live in the new
[`docs/AI_ASSISTANT.md`](AI_ASSISTANT.md) — this file only records that the reordering
happened and why.

**New, near-term, non-AI features surfaced by research** (none of these were in the original
request; all are cheap, proven, and fit the mission directly):
- **"First contact" sender flagging** — a calm note on the first email ever received from a
  given address. Needs no AI, no network call, no new permission: just a local lookup against
  addresses already seen. This is the "simpler compromise" version of categorization the user
  said would be acceptable if true AI categorization proved too complex — except it's arguably
  the better *first* build regardless, and it can ship before any AI work lands.
- **Read-aloud (text-to-speech)** — `android.speech.tts.TextToSpeech` ships in the Android SDK,
  no new dependency, works offline. Matches a feature Lively/Jitterbug devices already ship by
  name for this exact audience.
- **Home-screen widget** (unread count / latest sender) via Jetpack Glance — directly serves
  the "as little interaction with the app as possible" goal from the user's own request; a
  glance at the home screen answers "do I have new mail" without opening anything.

**Considered and deliberately not built: static App Shortcuts (long-press the launcher icon).**
This is a genuinely low-risk platform feature (a static XML resource, no runtime permission), but
it doesn't have an honest payoff for this app's current shape. A useful shortcut needs to jump
straight to a specific in-app screen — Oumatjie is a single Activity with all navigation handled
internally by `OumatjieNavHost` (inbox ↔ message ↔ settings), gated by `SessionViewModel`'s
sign-in/demo/signed-out state. Making a shortcut land anywhere other than the default cold start
would mean `MainActivity` parsing intent extras and threading a "requested destination" signal
through `OumatjieApp` and into navigation setup, working correctly against every session state
(what should a "Settings" shortcut do if the user isn't signed in yet?) — real, easy-to-get-wrong
plumbing for a payoff of skipping one or two taps, in an environment with no compiler to verify
any of it against. Worth building once there's compiler/emulator access to verify it end-to-end,
and once the app has a destination that's actually a chore to reach normally (the home-screen
widget above is a better first answer to "less friction to check mail" than a shortcut menu is).

**Permissions this adds, and the rule for requesting them.** Calendar-aware reading needs the
real `READ_CALENDAR` permission (intents can't do a read-only "am I free" query); any
AI-triggered notification needs `POST_NOTIFICATIONS` on Android 13+. Both follow the pattern
this project already committed to for the AI features generally: requested only at the moment
the user turns on the specific feature that needs them, never at first launch, and — for
calendar specifically — re-checked before every read rather than trusted from grant time,
since it's revocable at any moment from system settings.

**Longer-range idea, not scoped for near-term build.** A caregiver/family visibility feature
(an explicit one-tap daily check-in, not passive monitoring — passive reads as surveillance,
which cuts against docs/PRODUCT_PRINCIPLES.md's emphasis on dignity) is a real, validated
pattern in this space. It's recorded here and in AI_ASSISTANT.md's longer-range ideas because
it has the least precedent in Oumatjie's current architecture of anything on this list — it
implies a second person's identity and a consent flow more elaborate than anything else in the
app — not because it's a bad idea.
