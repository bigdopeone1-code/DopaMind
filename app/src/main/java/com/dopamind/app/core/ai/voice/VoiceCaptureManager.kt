package com.dopamind.app.core.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface VoiceCaptureState {
    data object Idle : VoiceCaptureState
    data object Listening : VoiceCaptureState
    data class PartialResult(val transcript: String) : VoiceCaptureState
    data class FinalResult(val transcript: String) : VoiceCaptureState
    data class Error(val message: String) : VoiceCaptureState
}

/**
 * Thin wrapper around Android's on-device [SpeechRecognizer]. No audio ever
 * leaves the device — this uses the platform's local recognition service,
 * not a cloud speech API.
 */
class VoiceCaptureManager(private val context: Context) {

    fun listen(): Flow<VoiceCaptureState> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceCaptureState.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(VoiceCaptureState.Listening)
            }

            override fun onResults(results: Bundle) {
                val text = results
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                trySend(VoiceCaptureState.FinalResult(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle) {
                val text = partialResults
                    .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull().orEmpty()
                if (text.isNotBlank()) trySend(VoiceCaptureState.PartialResult(text))
            }

            override fun onError(error: Int) {
                trySend(VoiceCaptureState.Error("Recognition error code $error"))
                close()
            }

            override fun onEndOfSpeech() {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "it-IT")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}
