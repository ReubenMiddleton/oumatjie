package com.granify.app.ui.mail

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import java.util.UUID

/**
 * A small wrapper around [TextToSpeech] (docs/AI_ASSISTANT.md, "Read-aloud (text-to-speech)").
 * Ships in the Android SDK itself — no new dependency, no network call, no runtime permission,
 * and works fully offline. Not an AI feature, and has no relationship to the AI provider work
 * at all — it can and does work regardless of whether AI features are turned on.
 */
class ReadAloudController(
    context: Context,
    private val onSpeakingChanged: (Boolean) -> Unit,
) {
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onSpeakingChanged(true)
            }

            override fun onDone(utteranceId: String?) {
                onSpeakingChanged(false)
            }

            @Deprecated("Deprecated in the platform API; still the only override available.")
            override fun onError(utteranceId: String?) {
                onSpeakingChanged(false)
            }
        })
    }

    fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts.stop()
        onSpeakingChanged(false)
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

/** Ties a [ReadAloudController] to the caller's composition lifetime, shutting it down (and
 * so silencing any in-progress speech) as soon as the caller leaves composition — e.g. the
 * user navigates away from the message they were listening to. */
@Composable
fun rememberReadAloudController(): Pair<ReadAloudController, Boolean> {
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    val controller = remember {
        ReadAloudController(context) { speaking -> isSpeaking = speaking }
    }
    DisposableEffect(Unit) {
        onDispose { controller.shutdown() }
    }
    return controller to isSpeaking
}
