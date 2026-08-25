# AI assistant — design specification

**Status, updated 2026-08-25: features 1–4 of the 8 below are implemented; category 7 is now
partially implemented (Tier 1 only); 5, 6, and 8 remain design only.** Built 2026-08-24:
first-contact sender flagging (1), scam/phishing calm warning (2), read-aloud (3), and
per-message summarization (4). Built 2026-08-25: per-user categorization's Tier 1 (no-AI local
rules — see "Categorization design" below and DECISIONS.md's "Tier 1 (no-AI) mail categorization
implemented" entry for the implementation-level reasoning). **Still not built**: calendar-aware
reading (5), AI-flagged notifications (6), categorization's Tier 2 (AI-assisted suggestion) and
its rename/merge UI, and the chat-style assistant panel (8) — these sections remain exactly what
they were, a specification for a future session to implement from, not a description of anything
that exists. The provider abstraction, provider recommendation, and trigger-model sections below
now describe real, implemented architecture (`ai/AiProvider.kt`, `ai/AnthropicAiProvider.kt`,
`ai/DemoAiProvider.kt`), not just a plan. None of this session's new code has actually been
compiled or run — see DECISIONS.md's "Verification summary (session 2026-08-24 addendum)".

Everything below traces back to one paragraph in
[`PRODUCT_PRINCIPLES.md`](PRODUCT_PRINCIPLES.md), quoted here in full because it is the
load-bearing constraint for the entire document:

> AI features must be optional, explicit, and unable to send mail or follow links without
> confirmation.

Two more existing principles from that same file matter just as much for this feature
specifically, and are treated as equally binding below:

> Request permissions just in time and explain why they are needed.

> Keep email processing on the device unless a feature genuinely requires a server.

Read literally, that last principle means: don't call a cloud AI for anything that a local
lookup or the on-device SDK can already do. Several features below (First Contact flagging,
read-aloud) exist specifically because they satisfy the feature's goal *without* tripping this
principle at all.

---

## Feature priority order

The user's original request listed categorization, calendar awareness, notifications, and a
chat-style assistant, in that rough order, with scam/phishing warnings mentioned but not
emphasized. Research done this session (see ROADMAP.md's "Phishing and scams" finding)
reordered this: seniors file the largest and fastest-growing share of fraud losses of any age
group, phishing/spoofing is their single most-reported fraud type, and there's peer-reviewed
evidence that detection ability specifically declines with age — meanwhile
PRODUCT_PRINCIPLES.md already promises "calm, factual warnings" for this exact case with
nothing built behind the promise yet. That combination — highest real-world stakes, existing
unfulfilled product promise, clearest mission fit — is why the order below leads with scam
detection rather than the features the user listed first.

Build in this order. Each is independently useful — nothing later in the list is a prerequisite
for something earlier.

1. **First-contact sender flagging. ✅ Implemented 2026-08-24.** Not AI. A calm, dismissible note
   on the first email ever received from a given address ("You haven't received mail from this
   address before"), modeled on Outlook/Exchange's "First Contact Safety Tip." Implementation is
   a local lookup against sender addresses already present in the fetched mail — no network call,
   no new permission, no dependency. Ships before anything else on this list, including before
   any AI provider is wired in at all. Real implementation: `data/senders/KnownSendersRepository`
   / `DataStoreKnownSendersRepository`; UI: `MailScreens.kt`'s `FirstContactBanner`.
2. **Scam / phishing calm warning. ✅ Implemented 2026-08-24.** The flagship AI feature, per the
   reasoning above. Triggered per-message, only when the user opens a message (never a background
   scan — see "Trigger model" below), and only when a real AI provider key is configured (never
   the demo heuristic — see DECISIONS.md for why). Sends the message's visible text to the
   configured AI provider with a prompt asking specifically for scam/phishing signals, and renders
   the result as a calm inline banner on the message using PRODUCT_PRINCIPLES.md's existing "calm,
   factual" language, not an alarming interstitial. Never auto-acts — flags for the user's own
   judgment only. Real implementation: `ai/AnthropicAiProvider.checkForScamSignals`; UI:
   `MailScreens.kt`'s `ScamCheckBanner`.
3. **Read-aloud (text-to-speech). ✅ Implemented 2026-08-24.** Not AI. `android.speech.tts.TextToSpeech`
   is part of the Android SDK — no new dependency, no network call, works offline. A named feature
   on Lively/Jitterbug devices for this same audience, so this is a proven fit, not a guess. Ships
   independently and has no relationship to the AI provider work at all. Real implementation:
   `ui/mail/ReadAloudController.kt`.
4. **Per-message summarization. ✅ Implemented 2026-08-24.** Same trigger model as scam detection
   (explicit per-message action — a "Summarize this" button) and shares the same provider
   call/response plumbing. Lower priority than scam detection because it's a convenience feature,
   not a safety one. Unlike scam detection, this one *does* fall back to `DemoAiProvider` when no
   key is configured, honestly labelled "Summary (demo)" in the UI — see DECISIONS.md for why that
   asymmetry with scam detection is deliberate. Real implementation:
   `ai/AnthropicAiProvider.summarize` / `ai/DemoAiProvider.summarize`; UI: `MailScreens.kt`'s
   `SummarySection`.
5. **Calendar-aware reading.** Not implemented. "Tell me if I have something planned on the date this email
   mentions," surfaced while reading the message. Needs the real `READ_CALENDAR` permission
   (see "Permissions" below — this cannot be done via intent handoff, since it's a query, not
   an action). Architecturally, this should reuse whatever date/event mention is already
   extracted for summarization rather than doing a second AI pass — see "Calendar privacy
   design" below for why the calendar's actual contents should never need to leave the device.
6. **AI-flagged important notifications.** Not implemented. Opt-in, and only reachable after the user has
   already turned on at least one of the message-level AI features above — this is presented as
   "notify me about things like this," not a standalone toggle, so the user always understands
   what triggers a notification before enabling it. Needs `POST_NOTIFICATIONS` (Android 13+),
   requested at the moment this specific feature is enabled.
7. **Per-user categorization. 🟡 Tier 1 implemented 2026-08-25; Tier 2 and rename/merge UI not
   implemented.** The most architecturally complex item on this list, matching the "simpler
   compromise" the user explicitly said would be acceptable. See "Categorization design" below
   for the two-tier approach that avoids having to build a fully generative per-user taxonomy on
   day one. Real implementation so far: `data/categories/MailCategory.kt`,
   `data/categories/CategoryAssigner.kt`; UI: a plain text label in `MailScreens.kt`'s
   `MailCard`. Not yet built: the AI-assisted Tier 2 suggestion, and any UI to rename or merge
   categories (both still open — see DECISIONS.md).
8. **Chat-style assistant panel.** Not implemented. Explicitly last. It's the most open-ended surface, the
   hardest to keep within the "unable to send mail or follow links without confirmation"
   principle (a chat interface invites exactly the kind of free-form request that principle
   exists to constrain), and the least differentiated from things a senior could already do
   with any general-purpose chatbot. Everything above it on this list delivers the user's
   real underlying goal — "automate things without the user feeling like they're using AI" —
   better than a chat panel does, since a chat panel is the opposite of that: it's AI the user
   has to actively operate. Build it last, and consider whether it's needed at all once 1–7
   are in place.

---

## Architecture

### Provider abstraction

Define a single interface the rest of the app depends on, so the concrete provider (whichever
is chosen) stays swappable and testable — this mirrors the existing
`GoogleAuthManager`/`DemoAuthManager` split already in the codebase (`auth/` package), which is
exactly this pattern for a different concern. A demo/fake implementation should exist from the
start for the same reason `DemoAuthManager` does: development, screenshots, and tests need to
run without spending real API calls or requiring a configured key.

Shape (documentation, not code to paste in verbatim — the implementing session should design
the real Kotlin types):

- `checkForScamSignals(subject, sender, bodyText) -> ScamAssessment` (a verdict plus a short
  plain-language reason, never a raw model response shown to the user)
- `summarize(subject, bodyText) -> String`
- `extractMentionedDates(bodyText) -> List<LocalDate>` (feeds calendar-aware reading without a
  second round trip)
- `chat(message, recentContext) -> String` (lowest priority — see item 8 above)

Every method takes only message text the user is already looking at — never the inbox as a
whole, never other messages, never calendar contents (see below).

### Provider recommendation

Two real options were priced out this session (see ROADMAP.md's "AI API landscape" finding):

- **Anthropic Claude (Haiku 4.5)** — no free tier, but cheap enough at single-user scale
  (realistic estimate: well under $1/month) that cost is not a real constraint. Anthropic's
  standard API terms do not train on customer prompts by default.
- **Google Gemini (Flash/Flash-Lite)** — genuinely free (1,500 requests/day), but Google's
  terms allow free-tier prompts to be used for model training. The paid tier does not have this
  caveat.

**Recommendation: default to Claude Haiku 4.5, or Gemini's *paid* tier if staying in Google's
ecosystem matters more than cost.** Do not use Gemini's free tier for real email content —
sending a user's actual mail (bank statements, family correspondence) into a pipeline that may
train a model on it conflicts directly with this project's privacy stance, and the whole point
of routing through a "calm, factual" scam-warning feature is to be trustworthy about exactly
this kind of handling. If cost ever becomes a real concern at some future scale, that's a
reason to revisit, not a reason to default to the free tier now.

Whichever provider is chosen, the API key must be user-supplied (entered in Settings, stored
the same way other sensitive local state is — not bundled in source or committed to the repo)
rather than shipped with the app. This is a personal-scale app the user intends to eventually
open-source; a bundled key would leak in the first `git log`.

### Trigger model

Every provider call is initiated by an explicit, visible user action on one specific message —
opening a message (for scam detection, if the feature is enabled), or tapping "Summarize this."
There is no background process that scans the inbox, polls for new mail, or runs on a schedule.
This is stricter than "opt-in" alone: even with every AI feature turned on, a user who never
opens a given message never triggers a call about it. This directly serves the user's own
framing of the goal — automation the user doesn't have to feel or operate — while staying
inside "email processing stays on the device unless a feature genuinely requires a server":
the server call happens only for the one message currently being read, not the inbox at large.

### Calendar privacy design

The calendar's actual contents (event titles, attendees, times) should never be sent to the AI
provider. The split: the AI provider only ever sees the email text it's already processing for
summarization (item 4) and, from that, extracts *mentioned dates* — a small, already-necessary
exposure, not a new one. The question "am I free on that date" is then answered by a purely
local `CalendarContract` query against those extracted dates, with the result composed and
shown to the user entirely on-device. The AI never learns what's actually on the user's
calendar; it only ever sees the date the *email* mentioned.

---

## Permissions and when to request them

| Feature | Permission | Requested when |
|---|---|---|
| First-contact flagging | none | never — local lookup only |
| Scam warning / summarization | none (network only, to the AI provider) | when the user first enables any message-level AI feature in Settings, with plain-language disclosure of what gets sent |
| Read-aloud | none | never — `TextToSpeech` needs no runtime permission |
| Calendar-aware reading | `READ_CALENDAR` | when the user turns this specific feature on, not before; re-checked before every calendar read since it's revocable at any time from system settings |
| AI-flagged notifications | `POST_NOTIFICATIONS` (Android 13+) | when the user turns this specific feature on |

This table is a direct application of "request permissions just in time and explain why they
are needed" — every row is requested at the moment of specific relevance, never at first
launch, and never bundled together as a single "enable AI features" grant.

---

## UI/UX shape

- **The panel is closed by default, always**, on every app launch, regardless of how many AI
  features are enabled. Opening it is always a deliberate action (a tab or icon, not something
  that appears unprompted).
- **Per-message results surface inline on the message itself** — a calm banner for a scam
  signal, a short block of text for a summary — not buried inside a separate chat transcript
  the user has to go find. The side panel is where the user *asks* for something (chat, if
  built at all); the inline banner is where the assistant *tells* the user something it already
  computed for the message they're looking at. Keeping these visually distinct matters: it's
  the difference between "the app noticed something and told me calmly" and "I have to go ask
  a chatbot," and the former is the whole point per the user's own framing.
- **Warning visual treatment**: consistent with PRODUCT_PRINCIPLES.md's "calm, factual"
  language — a neutral-toned banner with a short plain-English reason ("This is the first
  message from this address, and it asks you to click a link. Take a moment before clicking."),
  never red flashing, siren iconography, or a modal interrupt that blocks reading the message.
- **Every actionable suggestion requires a tap to act on** — the app can say "this looks like a
  password reset link," but never follows it, and never marks anything read/archived/deleted on
  the AI's own initiative. This is PRODUCT_PRINCIPLES.md's confirmation rule applied directly.
- **First-use disclosure is plain language, shown once before the first AI call ever fires**,
  stating what gets sent (the text of the message being read) and to which provider. If a free
  tier with a training caveat is ever offered as a user choice, that caveat is stated in the
  same disclosure, in plain words, not linked off to a separate policy page.

---

## Categorization design

Full free-form, generative-per-user categorization (a unique taxonomy invented per user, per
the user's original request) is architecturally the most demanding item on this list, so it's
built as two tiers rather than attempted whole:

- **Tier 1 (no AI, ships first if this feature is built at all). ✅ Implemented 2026-08-25**,
  minus the rename/merge UI. A small fixed starter set (Bills, Family, Receipts, Newsletters —
  `data/categories/StarterCategories`) assigned by simple local keyword rules over subject/preview
  text (`data/categories/CategoryAssigner`). `Family` is deliberately never auto-assigned — no
  reliable text signal exists for it, unlike the other three — see DECISIONS.md. **Still open**:
  the "which the user can rename or merge" half of Tier 1's own definition — no UI exists yet to
  view all categories, rename one, merge two, or manually (re)assign a category to a message.
- **Tier 2 (optional upgrade, AI-assisted). Not implemented.** The AI provider suggests a
  category for a message when Tier 1's rules don't confidently match one (i.e. `CategoryAssigner`
  returned `null`), using the same per-message trigger model as everything else — never a bulk
  reclassification pass over the whole inbox.

This mirrors the general on-device/cloud hybrid pattern the research surfaced repeatedly:
cheap, fast, local handling for the common case, cloud reasoning reserved for what actually
needs it.

---

## Longer-range ideas (not scoped for near-term build)

- **Caregiver/family check-in.** An explicit, user-initiated daily one-tap "I'm okay" rather
  than passive activity monitoring — passive monitoring reads as surveillance, which cuts
  against PRODUCT_PRINCIPLES.md's emphasis on dignity and not treating age as a single
  disability. Flagged here because it's a real, validated pattern in this product space (see
  ROADMAP.md's research findings), not because it's ready to design in detail — it implies a
  second person's identity, a second device or channel, and a consent flow more elaborate than
  anything else in this document, and deserves its own dedicated design pass when it's actually
  prioritized.

---

## Open questions for the implementing session

- ~~Exact storage for the user-supplied API key~~ — resolved 2026-08-24: DataStore Preferences,
  same as text-scale/session prefs, in `settings/SettingsRepository`. See DECISIONS.md for the
  masking-vs-visible-field tradeoff that was decided along with it.
- Whether to support multiple providers behind a Settings toggle from day one, or hardcode one
  and generalize later once the interface (above) has a second real implementation to prove it
  against.
- Cost/rate-limit guardrails if usage ever grows beyond single-user scale (not a concern at
  today's scope, per the pricing research above, but worth a deliberate check before any point
  where this app might get more than one real user).
- Whether scam-warning false positives (flagging legitimate mail) need a feedback mechanism
  ("this wasn't a scam") or whether that's over-engineering for a single-user app — lean toward
  not building this until real usage shows it's needed.
