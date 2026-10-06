package com.granify.app.data

private const val PDF_MEDIA_TYPE = "application/pdf"

/**
 * Oumatjie opens an attachment only when its listed MIME type is application/pdf.
 * Letter case does not matter, and parameters after the first semicolon are ignored.
 * The file name and the file bytes are not consulted.
 */
internal fun isOpenablePdf(mimeType: String): Boolean {
    val mediaType = mimeType.substringBefore(';').trim()
    return mediaType.equals(PDF_MEDIA_TYPE, ignoreCase = true)
}
