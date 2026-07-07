package com.wirewaypro.app.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.wirewaypro.app.domain.voice.VoiceTranscript
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import java.util.Locale

/**
 * In-app voice capture (job-walk layer 1). Taps the on-device
 * [SpeechRecognizer] so the electrician talks the scope and Wireway transcribes
 * it live — a pulsing waveform confirms it's hearing them, and the transcript is
 * fully editable before it's used. No AI, no pricing here: this is deterministic
 * dictation that fills a text field. When the mic is declined or no recognizer
 * exists, it falls back to a plain editable field so the typed path never breaks.
 *
 * @param onUse called with the final (edited) transcript when the user accepts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceDictationSheet(
    title: String = "Talk the job walk",
    prompt: String = "Say what you see — fixtures, circuits, panels, rooms. You can edit it after.",
    onUse: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberWirewayHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val recognizerAvailable = remember { SpeechRecognizer.isRecognitionAvailable(context) }

    var transcript by remember { mutableStateOf(VoiceTranscript()) }
    var edited by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var micDenied by remember { mutableStateOf(false) }
    var statusNote by remember {
        mutableStateOf(
            if (!recognizerAvailable) "No speech service on this device — type the scope below." else null,
        )
    }
    // Rolling amplitude buffer that drives the waveform (0f..1f, newest last).
    val amplitudes = remember { mutableStateListOf<Float>() }

    // The recognizer lives for the sheet's lifetime; created lazily, freed on close.
    val recognizer = remember {
        if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    fun startListening() {
        val r = recognizer ?: return
        listening = true
        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Prefer on-device recognition when the platform can do it offline.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        runCatching { r.startListening(intent) }
            .onFailure { listening = false; statusNote = "Couldn't start the mic — type the scope below." }
    }

    fun stopListening() {
        listening = false
        amplitudes.clear()
        runCatching { recognizer?.stopListening() }
    }

    // Wire the recognizer callbacks to the transcript + waveform state.
    DisposableEffect(recognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { statusNote = null }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB is roughly -2 (silence) .. 10 (loud); normalize to 0..1.
                val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                amplitudes.add(level)
                if (amplitudes.size > WAVE_BARS) amplitudes.removeAt(0)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.stringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { transcript = transcript.withPartial(it) }
            }

            override fun onResults(results: Bundle?) {
                results?.stringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.let { transcript = transcript.commitFinal(it) }
                // Keep the session going until the user taps Stop — the recognizer
                // ends after each pause, so re-arm it for the next sentence.
                if (listening) startListening()
            }

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                    -> if (listening) startListening() // silence between sentences — keep going
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        listening = false; micDenied = true
                    }
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> { /* a restart is already in flight */ }
                    else -> {
                        listening = false
                        if (transcript.isEmpty && edited.isBlank()) {
                            statusNote = "Didn't catch that — tap the mic to try again, or type it."
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        recognizer?.setRecognitionListener(listener)
        onDispose {
            runCatching { recognizer?.destroy() }
        }
    }

    // Mirror the live transcript into the editable field while listening; once
    // stopped, the field is the user's to correct freely.
    LaunchedEffect(transcript.display, listening) {
        if (listening && transcript.display.isNotBlank()) edited = transcript.display
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) { micDenied = false; startListening() } else micDenied = true
    }

    fun toggleMic() {
        if (listening) { haptics.tick(); stopListening(); return }
        if (recognizer == null) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) { haptics.tap(); startListening() } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Auto-start on open when we already hold the mic — one fewer tap on the job.
    LaunchedEffect(Unit) {
        if (recognizer != null &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        }
    }

    ModalBottomSheet(onDismissRequest = { stopListening(); onDismiss() }, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(prompt, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            VoiceWaveform(amplitudes = amplitudes, listening = listening, modifier = Modifier.fillMaxWidth().height(72.dp))

            Text(
                when {
                    listening -> "Listening…"
                    micDenied -> "Mic permission is off — enable it to dictate, or type below."
                    statusNote != null -> statusNote!!
                    else -> "Tap the mic to start."
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (listening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = edited,
                onValueChange = { edited = it },
                label = { Text("Transcript (edit before using)") },
                placeholder = { Text("Your dictated scope appears here…") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (recognizer != null) {
                    OutlinedButton(onClick = { toggleMic() }, modifier = Modifier.weight(1f)) {
                        Icon(if (listening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(if (listening) "Stop" else "Speak")
                    }
                }
                GradientButton(
                    text = "Use this",
                    onClick = {
                        stopListening()
                        haptics.confirm()
                        onUse(edited.trim())
                    },
                    enabled = edited.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private const val WAVE_BARS = 48

/**
 * A live electric-blue waveform: mirrored bars whose height tracks the mic
 * amplitude buffer, with a gentle breathing idle when nothing's been heard yet.
 * Pure Canvas — one gradient, no per-frame allocation beyond the bar list.
 */
@Composable
private fun VoiceWaveform(
    amplitudes: List<Float>,
    listening: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val faded = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val idle = rememberInfiniteTransition(label = "idle-wave")
    val phase by idle.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Restart),
        label = "phase",
    )
    Canvas(modifier) {
        val barCount = WAVE_BARS
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (barCount - 1)) / barCount).coerceAtLeast(1f)
        val midY = size.height / 2f
        val maxH = size.height * 0.92f
        val brush = Brush.verticalGradient(
            0f to primary,
            1f to primary.copy(alpha = 0.5f),
        )
        for (i in 0 until barCount) {
            // Newest amplitude sits on the right; older scrolls left.
            val ampIdx = amplitudes.size - barCount + i
            val amp = amplitudes.getOrNull(ampIdx)
            val level = when {
                amp != null -> amp
                listening -> 0.10f + 0.06f * (1f + kotlin.math.sin(phase + i * 0.5f)) // faint breathing
                else -> 0.06f
            }
            val h = (level * maxH).coerceAtLeast(barWidth)
            val x = i * (barWidth + gap) + barWidth / 2f
            drawLine(
                brush = if (amp != null || listening) brush else Brush.verticalGradient(0f to faded, 1f to faded),
                start = Offset(x, midY - h / 2f),
                end = Offset(x, midY + h / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
    Spacer(Modifier.width(0.dp))
}

private fun Bundle.stringArrayList(key: String): java.util.ArrayList<String>? =
    getStringArrayList(key)
