package com.granify.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenablePdfTest {
    @Test
    fun applicationPdf_matchesIgnoringCaseAndParameters() {
        listOf(
            "application/pdf",
            "Application/PDF",
            "APPLICATION/pdf",
            "application/pdf; charset=binary",
            "application/pdf;charset=binary",
            "application/pdf; name=\"statement.pdf\"",
            " application/pdf ",
            "application/pdf ; charset=binary",
            "Application/PDF; Name=\"Monthly statement.pdf\"",
        ).forEach { mimeType ->
            assertTrue(mimeType, isOpenablePdf(mimeType))
        }
    }

    @Test
    fun otherListedTypes_areNotPdf() {
        listOf(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/octet-stream",
            "application/x-pdf",
            "application/pdfx",
            "text/plain",
            "text/calendar",
            "",
            "   ",
            ";application/pdf",
            "image/jpeg; name=\"statement.pdf\"",
            "application/octet-stream; name=\"file.pdf\"",
        ).forEach { mimeType ->
            assertFalse(mimeType, isOpenablePdf(mimeType))
        }
    }
}
