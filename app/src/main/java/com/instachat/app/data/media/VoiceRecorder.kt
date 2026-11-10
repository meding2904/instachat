package com.instachat.app.data.media

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import java.io.File
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RecordingResult(val file: File, val durationMs: Long)

/**
 * Small MediaRecorder wrapper for hold-to-record voice notes. UI-owned so the
 * recording is always scoped to the visible chat.
 */
class VoiceRecorder(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L
    private var samplerJob: Job? = null

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    var isRecording: Boolean = false
        private set

    @SuppressLint("MissingPermission")
    fun start(target: File): Boolean {
        if (isRecording) return false
        return runCatching {
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(128_000)
            newRecorder.setAudioSamplingRate(44_100)
            newRecorder.setOutputFile(target.absolutePath)
            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            file = target
            startedAt = SystemClock.elapsedRealtime()
            _amplitudes.value = emptyList()
            _elapsedMs.value = 0L
            isRecording = true
            samplerJob = scope.launch {
                while (isActive && isRecording) {
                    val amplitude = runCatching { newRecorder.maxAmplitude }.getOrDefault(0)
                    val normalized = sqrt((amplitude / 32767f).coerceIn(0f, 1f))
                    _amplitudes.update { current ->
                        (current + normalized).takeLast(52)
                    }
                    _elapsedMs.value = SystemClock.elapsedRealtime() - startedAt
                    delay(65)
                }
            }
            true
        }.getOrElse {
            cleanup(false)
            false
        }
    }

    fun stop(): RecordingResult? {
        if (!isRecording) return null
        isRecording = false
        samplerJob?.cancel()
        samplerJob = null
        val recordedFile = file
        val duration = SystemClock.elapsedRealtime() - startedAt
        val active = recorder
        recorder = null
        file = null
        runCatching { active?.stop() }
        runCatching { active?.release() }
        if (recordedFile == null || !recordedFile.exists() || recordedFile.length() <= 0L) {
            recordedFile?.delete()
            return null
        }
        return RecordingResult(recordedFile, duration)
    }

    fun cancel() {
        isRecording = false
        cleanup(true)
    }

    fun release() {
        isRecording = false
        cleanup(true)
        scope.cancel()
    }

    private fun cleanup(deleteFile: Boolean) {
        samplerJob?.cancel()
        samplerJob = null
        val active = recorder
        recorder = null
        if (active != null) {
            runCatching { active.stop() }
            runCatching { active.release() }
        }
        if (deleteFile) {
            file?.delete()
        }
        file = null
        _amplitudes.value = emptyList()
        _elapsedMs.value = 0L
    }
}
