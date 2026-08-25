package com.granify.app.pdf

import android.os.Bundle
import android.view.View
import androidx.pdf.PdfDocument

sealed interface PdfLoadResult {
    data object Success : PdfLoadResult
    data class Error(val error: Throwable) : PdfLoadResult
}

fun interface PdfLoadCallback {
    fun onPdfLoadResult(result: PdfLoadResult)
}

/**
 * A thin subclass so [PdfViewerActivity] hears load/error results. The base fragment already
 * renders its own password prompt (with retry) when a document needs one, so Oumatjie only has
 * to react to the final success/error outcome.
 */
class OumatjiePdfViewerFragment : androidx.pdf.viewer.fragment.PdfViewerFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Must be set here, not by the host Activity right after committing the fragment
        // transaction: this setter reaches into a view the fragment only creates once its own
        // onViewCreated runs, and throws UninitializedPropertyAccessException before that.
        isToolboxVisible = false
    }

    override fun onLoadDocumentSuccess(document: PdfDocument) {
        super.onLoadDocumentSuccess(document)
        (activity as? PdfLoadCallback)?.onPdfLoadResult(PdfLoadResult.Success)
    }

    override fun onLoadDocumentError(error: Throwable) {
        super.onLoadDocumentError(error)
        (activity as? PdfLoadCallback)?.onPdfLoadResult(PdfLoadResult.Error(error))
    }
}
