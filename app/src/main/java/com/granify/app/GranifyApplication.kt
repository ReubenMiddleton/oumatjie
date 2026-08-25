package com.granify.app

import android.app.Application
import com.granify.app.data.attachments.AttachmentCache
import com.granify.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OumatjieApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Sweep any attachment left behind by a crash or force-stop from a previous run.
        applicationScope.launch { AttachmentCache.clear(this@OumatjieApplication) }
    }
}
