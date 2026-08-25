# Development setup

## 1. Workstation

Install the latest stable Android Studio and accept its standard Android SDK setup. This project requires:

- Android Studio with its bundled JDK (JDK 17 or newer)
- Android SDK Platform 37 for compilation
- Android SDK Platform 36 for the current target
- Android SDK Build Tools 36
- Android SDK Platform Tools (`adb`)
- An API 28 or newer emulator, or a physical Android device with USB debugging

The Gradle wrapper will be committed to the repository, so a system-wide Gradle installation is not required.

## 2. First local build

Open the repository in Android Studio, allow Gradle sync to finish, then run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The first build downloads dependencies from Google's Maven repository and Maven Central.

## 3. Google Cloud project (needed for real Gmail)

The demo inbox does not need credentials, and the app always offers it as a fallback. Before
connecting a real Gmail account:

1. Create separate Google Cloud projects for development and production.
2. Enable the Gmail API in the development project.
3. Configure the OAuth consent screen for an external application.
4. Add developer accounts as test users during development.
5. Create an Android OAuth client for package `com.oumatjie.app` (the app's `applicationId` —
   see `app/build.gradle.kts`; this is deliberately different from the `com.granify.app` Kotlin
   package/namespace used internally, see docs/DECISIONS.md).
6. Register both debug and release signing-certificate SHA-1 fingerprints.

That's it — nothing to paste into the app. `auth/GoogleAuthManager` calls Google Play
services' Authorization API (`Identity.getAuthorizationClient`), which is matched to your
Cloud project by package name and signing certificate alone, so there is no client ID or
secret to store anywhere in this repository.

Oumatjie requests `gmail.readonly` and `gmail.modify` together at sign-in — both are already
used (readonly for reading mail, modify for mark-as-read and Trash), so there is nothing
gained by splitting the request. If a future feature needs a *new* scope, request it there,
not here.

Never request a user's Gmail password. Never put an OAuth client secret in the APK; an Android OAuth client is identified using the package and signing certificate.

To print the debug certificate fingerprint after Gradle is available:

```powershell
.\gradlew.bat signingReport
```

## 4. Integration status

Implemented against the real Gmail API and **run end-to-end on an emulator** (not just
compiled — see docs/DECISIONS.md for exactly what that testing found, including two crashes
it's how they were caught), ready to try for real as soon as section 3 above is done:

- Sign-in through `AuthorizationClient` (`auth/GoogleAuthManager.kt`) — verified as far as
  reaching the real Google account picker and handling a cancelled/no-account result
  gracefully; not yet verified with a real granted account, since that needs section 3.
- Inbox and message reading (`data/gmail/GmailMailRepository.kt`) — verified against the demo
  repository and unit tests against fakes; **not** yet run against a real Gmail account (see
  below).
- Mark as read and move to Trash, both with a confirmation dialog — verified end-to-end in the
  demo inbox, including the Trash snackbar and the read-state color change.
- Attachment download into the app cache, deleted automatically on next launch and when the
  viewer closes (`data/attachments/`) — verified end-to-end in the demo inbox.
- In-app PDF viewing with the library's own password prompt (`pdf/PdfViewerActivity.kt`) —
  verified opening a real PDF end-to-end; the library's password prompt itself has not been
  exercised (the bundled demo PDF isn't encrypted), and a floating edit/annotate button from
  the library's toolbox could not be hidden — see docs/DECISIONS.md, "Known limitation".
- An in-app text size setting, independent of Gmail (`data/settings/`) — verified all three
  sizes render without clipping on both the inbox and Settings screens.

Deliberately not built yet, because no feature needs them:

- Archive (README and the product principles only call for mark-read and Trash)
- Reply/compose (needs a send scope, which docs/PRODUCT_PRINCIPLES.md says to add only once
  reply functionality exists)
- Session persistence across app restarts (today, closing the app returns to sign-in; each
  session re-authorizes, which is silent once scopes are already granted)
- Inbox pagination past the first page (`GmailMailRepository` requests the most recent 25
  messages)

**Still not exercised against a real inbox at all** — everything above involving
`data/gmail/GmailMailRepository` was verified either against the demo repository or unit tests
against hand-written fakes (`GmailMailRepositoryTest`, `MimePayloadParsingTest`), because there
is still no configured Google Cloud project. Once section 3 is done, expect a first pass of
real-account testing: confirm token refresh behaves over a longer session, confirm HTML-only
emails render acceptably once stripped to plain text, and confirm the Trash/mark-read calls
behave the same way against real Gmail data as they did against the fakes.

## 5. Play Store preparation (later)

Before a public release, plan for:

- Google OAuth brand and restricted-scope verification
- A public privacy policy and data-deletion instructions
- Play Console Data safety declarations
- Accessibility testing with TalkBack, large fonts, display scaling, and Accessibility Scanner
- Closed testing with older adults using different devices and Android versions
- A documented retention policy for email bodies and attachments
- A production signing key stored outside this repository
