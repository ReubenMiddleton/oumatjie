package com.granify.app.data

/** Thrown when Gmail access could not be confirmed; the message is safe to show as-is. */
class MailAuthException(message: String) : Exception(message)
