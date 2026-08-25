# Play Store readiness

A concrete checklist for taking Oumatjie from "runs on an emulator" to "listed on the Play
Store," gathered from Google's own published Play Console/Play Developer policy documentation.
Organized by what's already true of this codebase, what's pure paperwork/account-setup work only
the project owner can do, and what's still a real engineering gap. Cross-references
`docs/NEEDS_YOUR_INPUT.md` for anything blocked on the owner's own accounts or decisions, and
`docs/SETUP.md` for the Google Cloud project checklist this overlaps with.

## Already satisfied

- **Target API level.** Google requires new and updated apps to target Android 15 (API 35) as of
  around August 31, 2025, with the requirement rolling forward to Android 16 (API 36) roughly a
  year later — Play Console typically grants an extension to November 1 of the deadline year for
  developer accounts that request one before the deadline. This project's `targetSdk = 36`
  (`app/build.gradle.kts`) already meets the more current requirement outright; nothing to do
  here. Re-verify the exact current deadline in Play Console before submission, since Google has
  moved this date before and this document can't stay current on its own.
- **No embedded secrets.** No OAuth client secret, API key, or signing key is committed to this
  repository (`README.md`'s own instructions call this out explicitly) — Play Console's app
  content review checks for exactly this class of problem.
- **Permissions are minimal and justified.** The manifest requests only `INTERNET`
  (`AndroidManifest.xml`) — no location, contacts, storage, or other sensitive runtime
  permission is requested by anything currently implemented, which keeps the Data Safety form
  (below) short and keeps the app out of Google's stricter review lanes for sensitive-permission
  apps.
- **No ads, no third-party trackers.** Confirmed by reading `app/build.gradle.kts` and
  `AndroidManifest.xml` directly — no ad SDK, no analytics SDK, no crash-reporting SDK exists in
  this project. This simplifies both the Data Safety form and Google's separate ad-related policy
  surface to "not applicable."

## Paperwork and account setup (the project owner's own accounts — can't be done from this repo)

- **Google Play Developer account.** A one-time $25 registration fee, a real identity/organization
  verification, and (depending on current Play Console requirements) a D-U-N-S number if
  registering as an organization rather than an individual. Start this early — identity
  verification can take from a few days to a few weeks.
- **OAuth consent screen + brand verification.** `docs/SETUP.md` §3 already covers creating the
  Android OAuth client; separately, Google requires the OAuth consent screen's "app information"
  (name, logo, support email, homepage) to pass a brand verification review before the
  `gmail.readonly`/`gmail.modify` scopes can be used with real (non-test) Google accounts at
  scale.
- **Restricted-scope OAuth verification + CASA.** `gmail.readonly` and `gmail.modify` are both
  Google-designated "restricted scopes," which triggers Google's own security assessment on top
  of normal OAuth verification — a completed CASA (Cloud Application Security Assessment)
  questionnaire, potentially followed by a lab-based Tier 2 assessment (a paid third-party
  security review, historically in the low-$1,000s) if Google's automated Tier 1 self-assessment
  flags anything. Budget real calendar time for this — it is routinely the longest step in Play
  Store prep for any app touching Gmail, independent of how ready the app itself is. Start this
  well before a target launch date, not right before it.
- **Privacy policy hosting.** `docs/PRIVACY_POLICY.md` is a drafted, accurate-to-the-code first
  pass, written for oumatjie.com — it needs (a) a real legal review before publishing (see the
  drafting note at the end of that file) and (b) to actually be hosted at a stable, public URL on
  oumatjie.com, since both the Play Console listing and the OAuth consent screen require a live
  privacy policy link, not just a document that exists.
- **Data Safety form.** A Play Console questionnaire about what data the app collects, why, and
  whether it's shared — filled out in Play Console itself, not a repo file, but
  `docs/PRIVACY_POLICY.md`'s "What Oumatjie can see" / "Where your data goes" sections are written
  to map directly onto it: Gmail message content (collected, used for app functionality, not
  shared for advertising), and — only if AI features are enabled — message text sent to
  Anthropic (collected, used for app functionality via a service provider, not shared for
  advertising). A July 2026 Play policy update specifically extended the User Data Policy's
  disclosure requirements to cover third-party AI integrations like this one, so this section is
  worth double-checking against Play Console's current form wording at submission time rather
  than assuming last year's category list still applies unchanged.
- **Closed testing period.** New Play Developer accounts are generally required to run a closed
  test (Google's current requirement: at least 12 testers opted in continuously for at least 14
  days) before an app can go to production availability. Plan for this as real calendar time
  after the app is otherwise ready, not as a formality that happens instantly.
- **Play App Signing.** Enroll the app in Play App Signing (Google holds the production signing
  key, the developer holds an upload key) rather than self-managing the production signing key —
  Google's now-standard recommendation, and required for some newer Play features. The production
  signing key itself (`docs/DECISIONS.md`, "No production signing key") still needs to be
  generated and stored somewhere outside this repository; this is listed here because Play App
  Signing changes *how* that key is managed, not just whether one exists.

## Real engineering gaps (would need code, not just paperwork)

- **Accessibility testing pass.** `docs/SETUP.md` §5 already calls for TalkBack, large-font, and
  Accessibility Scanner testing before release — genuinely important for an accessibility-first
  app's credibility, and not yet done (no compiler/emulator access existed in the sessions that
  wrote most of this code — see `docs/DECISIONS.md`'s sandbox-environment notes). This should
  happen on the real Windows machine with a real emulator or device, not attempted from a
  cloud-sandboxed session with no display.
- **A retention policy for email bodies and attachments**, referenced as a gap in `docs/SETUP.md`
  §5. The current behavior (attachments cached temporarily, cleared automatically — see
  `docs/PRIVACY_POLICY.md`) is reasonable and already documented, but hasn't been written up as
  an explicit stated policy anywhere Google's review would look for one. Likely just needs a
  short explicit paragraph added to the privacy policy once the general legal review happens,
  not new code.
- **Real-account testing**, tracked in `docs/NEEDS_YOUR_INPUT.md` — the app has never been
  exercised against a live Gmail account end-to-end, only the offline demo inbox and unit tests
  against fakes, because no Google Cloud project has been created yet. This blocks meaningfully
  completing several of the paperwork items above (you can't finish an OAuth consent screen
  review for a scope you've never actually exercised against a real account) — see
  `docs/SETUP.md` §3.

## Suggested order

1. Register the Play Developer account and start identity verification (long lead time, do this
   first, in parallel with everything else).
2. Finish `docs/SETUP.md` §3 (Google Cloud project, OAuth client, real-account testing) — this
   unblocks actually exercising the scopes the rest of this checklist assumes work.
3. Get `docs/PRIVACY_POLICY.md` through a real legal review and hosted live at oumatjie.com.
4. Start the OAuth consent screen brand verification and restricted-scope/CASA process — begin
   this as early as possible given how long it can take.
5. Run the accessibility testing pass on a real device.
6. Fill out the Data Safety form and complete the closed testing period.
7. Enroll in Play App Signing, generate and securely store the production key, and submit.
