package com.granify.app.auth

/**
 * Gmail API scopes, requested incrementally and only when a feature needs them
 * (see docs/SETUP.md, "Planned integration order").
 */
object GmailScopes {
    const val READONLY = "https://www.googleapis.com/auth/gmail.readonly"
    const val MODIFY = "https://www.googleapis.com/auth/gmail.modify"
}
