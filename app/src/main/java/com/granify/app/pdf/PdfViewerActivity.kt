package com.granify.app.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commitNow
import com.granify.app.R
import com.granify.app.data.attachments.AttachmentCache

/**
 * Hosts the document viewer outside Compose, since androidx.pdf's PdfViewerFragment is a
 * classic View/Fragment component (see docs/SETUP.md: "AndroidX PDF viewer behind a
 * replaceable viewer boundary").
 */
class PdfViewerActivity : AppCompatActivity(R.layout.activity_pdf_viewer), PdfLoadCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val documentUri = intent.parcelableExtra(EXTRA_DOCUMENT_URI, Uri::class.java)
        if (documentUri == null) {
            finish()
            return
        }
        val documentTitle = intent.getStringExtra(EXTRA_DOCUMENT_TITLE)
            ?: getString(R.string.pdf_default_title)

        findViewById<TextView>(R.id.document_title).text = documentTitle
        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        findViewById<Button>(R.id.error_back_button).setOnClickListener { finish() }

        if (savedInstanceState == null) {
            val fragment = OumatjiePdfViewerFragment()
            // commitNow (not commit): the fragment must actually be attached — which a
            // merely-scheduled commit does not guarantee — before its documentUri setter can
            // reach its own ViewModelStore. Setting documentUri any earlier (e.g. while still
            // building the fragment) throws "Can't access ViewModels from detached fragment".
            supportFragmentManager.commitNow {
                replace(R.id.pdf_fragment_container, fragment, PDF_FRAGMENT_TAG)
            }
            fragment.documentUri = documentUri
        }
    }

    override fun onPdfLoadResult(result: PdfLoadResult) {
        findViewById<ProgressBar>(R.id.loading_indicator).visibility = View.GONE
        findViewById<View>(R.id.error_container).visibility = when (result) {
            PdfLoadResult.Success -> View.GONE
            is PdfLoadResult.Error -> View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // A true finish (not a rotation) is the last moment Oumatjie can promptly delete the
        // cached copy; OumatjieApplication also sweeps this on next launch as a backstop.
        if (isFinishing) {
            AttachmentCache.clear(applicationContext)
        }
    }

    companion object {
        private const val PDF_FRAGMENT_TAG = "pdf_fragment"
        private const val EXTRA_DOCUMENT_URI = "document_uri"
        private const val EXTRA_DOCUMENT_TITLE = "document_title"

        fun createIntent(context: Context, documentUri: Uri, documentTitle: String? = null): Intent =
            Intent(context, PdfViewerActivity::class.java).apply {
                putExtra(EXTRA_DOCUMENT_URI, documentUri)
                if (documentTitle != null) putExtra(EXTRA_DOCUMENT_TITLE, documentTitle)
            }
    }
}

private fun <T : Parcelable> Intent.parcelableExtra(key: String, clazz: Class<T>): T? =
    if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(key, clazz)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
