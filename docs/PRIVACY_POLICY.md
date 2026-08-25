# Oumatjie Privacy Policy

**Last updated: August 24, 2026**

This is the privacy policy for Oumatjie, an Android email app for reading Gmail. It's written in
the same plain language the app itself uses — if a sentence here wouldn't make sense read aloud
to the person the app is built for, it needs rewriting. This document is a draft prepared for
publication at oumatjie.com and has not yet had a lawyer's review; see the note at the end before
publishing it or submitting it as part of a Google OAuth verification or Play Console listing.

## The short version

Oumatjie reads your Gmail so you can read it back, in a simpler screen. It does not have a
server of its own — there is no Oumatjie company database your mail passes through. Your device
talks directly to Google's Gmail service, and, only if you turn the feature on yourself and add
your own API key, directly to Anthropic (the company behind Claude) to check a message you've
opened for scam signals or to summarize it. Oumatjie does not show ads, does not use analytics or
tracking software, and does not sell or share your data with anyone for advertising or any other
purpose.

## What Oumatjie can see

When you choose "Continue with Google," Oumatjie asks Google for permission to read your Gmail
(the `gmail.readonly` scope) and to make two specific changes to it (the `gmail.modify` scope):
marking a message as read, and moving a message to Trash. It never asks for permission to send
mail, and it never sees or asks for your Google account password — sign-in happens entirely
through Google's own account picker, using Android's built-in Google Play services
authorization, the same mechanism many other Android apps use to connect a Google account
without ever handling a password directly.

Once you're signed in, Oumatjie can read your inbox, the text and any attached documents of
messages you open, and can mark messages read or move them to Trash on your instruction. It does
not read Sent mail, Drafts, or any other Gmail label beyond what's needed to show your inbox and
the messages in it.

## Where your data goes

**Google.** Every request Oumatjie makes for your mail goes directly from your device to
Google's Gmail API, using the sign-in described above. Oumatjie doesn't have a server that sees
this traffic first — there's nothing to intercept, because there's nothing in between.

**Anthropic — only if you turn this on.** Oumatjie can optionally check an open message for scam
signals, or summarize it, using Anthropic's Claude AI. This is off by default and stays off
until you explicitly turn it on in Settings and add your own Anthropic API key. Turning it on
shows a one-time plain-language explanation of exactly what happens: when you open a message (for
the scam check) or tap "Summarize this," the text of that one message is sent to Anthropic.
Oumatjie never sends your whole inbox, never sends a message you haven't opened, and never sends
anything at all unless you've turned this feature on. If you haven't added an API key,
summaries use a clearly-labelled offline demo instead, and the scam check stays off entirely
(it never uses a fake substitute — see the "how we handle AI" note below). Anthropic's own
handling of API traffic is governed by Anthropic's own commercial terms, not by a free-tier
consumer product's terms; that's a deliberate choice explained further down.

**Nobody else.** Oumatjie has no analytics software, no crash-reporting service, no advertising
network, and no third-party tracking of any kind built into it. There is no list of "third
parties we share data with" beyond the two named above, because there isn't a third one.

## What Oumatjie stores, and where

Everything Oumatjie stores lives on your device, not on a server:

- Your text size preference, whether AI features are turned on, and (if you've added one) your
  Anthropic API key — stored locally using Android's DataStore, never transmitted anywhere except
  that the API key is used to authenticate your own requests directly to Anthropic when you use
  an AI feature.
- A list of sender addresses Oumatjie has seen mail from before, used only to show a "You haven't
  received mail from this address before" note the first time a new sender appears. This list
  never leaves your device.
- Documents you open from an email are downloaded to a temporary cache on your device so they can
  be viewed, and are deleted automatically the next time you open the app and when you close the
  document viewer.
- A remembered "you've signed in before" flag, so re-opening the app can silently reconnect to
  your Google account without asking you to sign in again — this flag is not your Google
  credential or an access token, just a note that one was previously granted.

Oumatjie itself never sends any of this to a server, because Oumatjie doesn't have one.

## How we handle AI features responsibly

Anthropic was chosen deliberately over some other providers specifically because its API terms
do not use customer prompts to train models by default, unlike some providers' free consumer
tiers — and real email content, including messages from people you know, is not something this
app is willing to risk that with. If you turn AI features on, you're sending message text to
Anthropic under Anthropic's own API terms (not a free consumer product's terms); you can review
those at [anthropic.com](https://www.anthropic.com) before deciding whether to enable this
feature. The scam-check feature specifically never uses a fake or heuristic substitute when no
real AI provider is configured — it simply stays off, rather than giving you a false sense that
messages are being checked when they aren't.

## Your choices

- **Turn AI features off** (or never turn them on) in Settings at any time — the app works
  fully without them, using only Google's Gmail service.
- **Revoke Oumatjie's access to your Google account** at any time from your Google Account's
  [connected apps & sites](https://myaccount.google.com/connections) settings. This immediately
  stops Oumatjie from being able to read your mail.
- **Delete everything Oumatjie has stored locally** by clearing the app's storage or uninstalling
  it from Android's Settings → Apps. Because nothing is stored on a server, this removes
  everything — there is no separate "delete my account" step to take anywhere else.

## Children's privacy

Oumatjie is not directed at children and is not knowingly used to collect information from
anyone under 13. It's built for older adults who find typical email apps hard to use, but nothing
about sign-in or data handling is age-gated beyond what Google's own account and OAuth
requirements already enforce.

## Changes to this policy

If this policy changes in a way that meaningfully affects what data is collected or how it's
used, the "Last updated" date above will change and, where practical, the app will surface that
change the next time AI features or sign-in are touched — the same plain-language-disclosure
approach already used for turning AI features on in the first place.

## Contact

Questions about this policy or about your data can be sent to the contact address listed on
oumatjie.com.

---

*Drafting note (remove before publishing): this policy was drafted from the app's actual,
verified behavior as of 2026-08-24 — the OAuth scopes requested (`auth/GmailScopes.kt`), the
absence of any analytics/crash-reporting SDK (checked against `app/build.gradle.kts` and
`AndroidManifest.xml` directly), what's stored locally (`data/settings/`, `data/senders/`,
`session/`), and the AI disclosure text already shown in-app (`ui/settings/SettingsScreen.kt`).
It has not been reviewed by a lawyer. Before this is published at oumatjie.com or submitted as
part of a Google OAuth restricted-scope verification or a Play Console Data Safety form, it's
worth a real legal review — this document is a solid, accurate starting point, not a substitute
for one. See `docs/NEEDS_YOUR_INPUT.md` for how this fits into the rest of Play Store prep.*
