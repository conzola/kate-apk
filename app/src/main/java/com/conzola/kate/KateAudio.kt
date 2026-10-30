package com.conzola.kate

import android.annotation.SuppressLint
import android.media.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object KateAudio {
    /**
     * Kate streams a WAV whose header claims a huge length, then 16-bit mono PCM as it is synthesised.
     * MediaPlayer can't handle that, so read the sample rate from the header and feed an AudioTrack directly.
     */
    suspend fun playStream(body: ResponseBody) = withContext(Dispatchers.IO) {
        val src = body.byteStream()
        val head = ByteArray(44); var got = 0
        while (got < 44) { val n = src.read(head, got, 44 - got); if (n < 0) return@withContext; got += n }
        val rate = ByteBuffer.wrap(head, 24, 4).order(ByteOrder.LITTLE_ENDIAN).int.coerceIn(8000, 48000)
        val min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(maxOf(min, rate * 2)).setTransferMode(AudioTrack.MODE_STREAM).build()
        var frames = 0L
        try {
            track.play()
            val buf = ByteArray(8192); var pending = 0
            while (isActive) {
                val n = src.read(buf, pending, buf.size - pending); if (n < 0) break
                val total = pending + n; val even = total and 1.inv()
                if (even > 0) { track.write(buf, 0, even); frames += even / 2 }
                if (total > even) buf[0] = buf[even]          // keep a dangling odd byte for the next read
                pending = total - even
            }
            val deadline = System.currentTimeMillis() + frames * 1000 / rate + 1500
            while (isActive && track.playbackHeadPosition < frames && System.currentTimeMillis() < deadline) delay(40)
        } finally { track.release() }
    }

    private fun ByteArrayOutputStream.putShorts(b: ShortArray, n: Int) {
        for (i in 0 until n) { write(b[i].toInt() and 255); write((b[i].toInt() shr 8) and 255) }
    }

    /**
     * 16 kHz mono PCM16, the format /talk expects. Calibrates to the room for 0.3 s, waits up to 6 s for speech,
     * keeps 0.3 s of pre-roll, stops after 0.8 s of quiet or 15 s. Returns null if nothing was said.
     */
    @SuppressLint("MissingPermission")
    private suspend fun record(): ByteArray? = withContext(Dispatchers.IO) {
        val rate = 16000; val chunk = rate / 10
        val rec = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, chunk * 2 * 8)
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return@withContext null }
        val out = ByteArrayOutputStream(); val pre = ArrayDeque<ShortArray>()
        var floor = 0.0; var spoke = false; var quiet = 0
        rec.startRecording()
        try {
            for (step in 0 until 150) {
                if (!isActive) break
                val buf = ShortArray(chunk); val n = rec.read(buf, 0, chunk)
                if (n <= 0) continue
                var s = 0.0; for (i in 0 until n) s += buf[i] * buf[i].toDouble()
                val rms = sqrt(s / n)
                if (step < 3) floor += rms / 3.0
                val threshold = (floor * 3.0).coerceIn(450.0, 2500.0)
                val loud = step >= 3 && rms > threshold
                if (!spoke) {
                    if (loud) { spoke = true; pre.forEach { out.putShorts(it, it.size) }; out.putShorts(buf, n) }
                    else { pre.addLast(buf); if (pre.size > 3) pre.removeFirst(); if (step >= 60) break }
                } else {
                    out.putShorts(buf, n)
                    if (loud) quiet = 0 else if (++quiet >= 8) break
                }
            }
        } finally { rec.stop(); rec.release() }
        if (spoke) out.toByteArray() else null
    }

    /** Push-to-talk against the same /talk endpoint the ESP32 uses. Follows X-Continue like the bar does. */
    suspend fun talk() = withContext(Dispatchers.IO) {
        if (KateLink.host.isBlank()) { KateState.message.value = "Add Kate's address first"; return@withContext }
        try {
            var turns = 0; var again = true
            while (again && turns++ < 4) {
                again = false
                KateState.phase.value = Phase.LISTENING
                val pcm = record()
                if (pcm == null) { KateState.message.value = "Didn't catch that"; break }
                KateState.phase.value = Phase.THINKING
                val body = pcm.toRequestBody("application/octet-stream".toMediaType())
                KateLink.talkHttp.newCall(Request.Builder().url(KateLink.base() + "/talk").post(body).build()).execute().use { r ->
                    when {
                        r.code == 204 -> {}
                        r.isSuccessful -> {
                            again = r.header("X-Continue") == "1"
                            KateState.phase.value = Phase.SPEAKING
                            r.body?.let { playStream(it) }
                        }
                        else -> { KateState.message.value = "Kate hit a problem (${r.code})" }
                    }
                }
            }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { KateState.message.value = "Couldn't reach Kate"
        } finally { KateState.phase.value = Phase.IDLE }
    }
}
