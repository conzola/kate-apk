package com.conzola.kate

import android.annotation.SuppressLint
import android.media.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import kotlin.coroutines.resume
import kotlin.math.sqrt

object KateAudio {
    suspend fun play(wav: ByteArray) = suspendCancellableCoroutine<Unit> { cont ->
        val f = File(KateLink.app.cacheDir, "kate.wav").apply { writeBytes(wav) }
        val mp = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT).build())
            setDataSource(f.path); prepare()
            setOnCompletionListener { release(); if (cont.isActive) cont.resume(Unit) }
        }
        cont.invokeOnCancellation { mp.release() }
        mp.start()
    }

    /** 16 kHz mono PCM16, stops after ~0.9 s of silence or 10 s, like the ESP32 path. */
    @SuppressLint("MissingPermission")
    suspend fun record(): ByteArray = withContext(Dispatchers.IO) {
        val rate = 16000; val chunk = rate / 10
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, chunk * 8)
        val out = java.io.ByteArrayOutputStream(); val buf = ShortArray(chunk)
        var spoke = false; var quiet = 0
        rec.startRecording()
        try {
            for (step in 0 until 100) {
                val n = rec.read(buf, 0, chunk); if (n <= 0) continue
                var s = 0.0; for (i in 0 until n) s += buf[i] * buf[i].toDouble()
                val loud = sqrt(s / n) > 600
                if (loud) { spoke = true; quiet = 0 } else if (spoke) quiet++
                for (i in 0 until n) { out.write(buf[i].toInt() and 255); out.write(buf[i].toInt() shr 8 and 255) }
                if (spoke && quiet >= 9) break
            }
        } finally { rec.stop(); rec.release() }
        out.toByteArray()
    }

    /** Push-to-talk against the same /talk endpoint the ESP32 uses. */
    suspend fun talk() {
        KateState.listening.value = true
        try {
            val pcm = record(); KateState.listening.value = false
            if (pcm.size < 8000) return
            val wav = withContext(Dispatchers.IO) {
                KateLink.http.newCall(Request.Builder().url("http://${KateLink.host}:8000/talk")
                    .post(pcm.toRequestBody("application/octet-stream".toMediaType())).build())
                    .execute().use { it.body?.bytes() }
            }
            if (wav != null && wav.size > 44) play(wav)
        } catch (e: Exception) { /* offline: nothing to play */ }
        finally { KateState.listening.value = false }
    }
}
