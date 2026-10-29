package com.conzola.kate

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.update
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Single place that decides how a message reaches Kate: Wi-Fi bridge first, Bluetooth after an NFC tap. */
@SuppressLint("MissingPermission")
object KateLink {
    lateinit var app: Context
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val http: OkHttpClient = OkHttpClient.Builder().connectTimeout(2, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build()
    private var sock: BluetoothSocket? = null

    fun init(c: Context) { if (!::app.isInitialized) app = c.applicationContext }
    private val prefs get() = app.getSharedPreferences("kate", 0)
    var host: String
        get() = prefs.getString("host", "") ?: ""
        set(v) = prefs.edit().putString("host", v.trim()).apply()
    var btMac: String
        get() = prefs.getString("mac", "") ?: ""
        set(v) = prefs.edit().putString("mac", v.trim().uppercase()).apply()
    var forward: Boolean
        get() = prefs.getBoolean("fwd", true)
        set(v) = prefs.edit().putBoolean("fwd", v).apply()
    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()
    var blocked: Set<String>
        get() = (prefs.getStringSet("blocked", emptySet()) ?: emptySet()).toSet()
        set(v) = prefs.edit().putStringSet("blocked", v).apply()
    private fun base() = "http://$host:8000"

    private fun wifiOk() = host.isNotBlank() && try {
        http.newBuilder().callTimeout(2, TimeUnit.SECONDS).build()
            .newCall(Request.Builder().url(base() + "/last.wav").head().build()).execute().use { true }
    } catch (e: Exception) { false }

    suspend fun test(h: String): Boolean = withContext(Dispatchers.IO) {
        try {
            http.newBuilder().callTimeout(3, TimeUnit.SECONDS).build()
                .newCall(Request.Builder().url("http://${h.trim()}:8000/last.wav").head().build()).execute().use { true }
        } catch (e: Exception) { false }
    }

    /** Re-evaluates the transport. Bluetooth wins while its socket is alive; otherwise Wi-Fi; otherwise offline. */
    fun refresh() {
        val bt = sock?.isConnected == true
        if (!bt) sock = null
        KateState.link.value = when { bt -> Link.BLUETOOTH; wifiOk() -> Link.WIFI; else -> Link.OFFLINE }
    }

    fun connectBluetooth(mac: String = btMac): Boolean = try {
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        adapter.cancelDiscovery()
        sock?.close()
        val s = adapter.getRemoteDevice(mac).createRfcommSocketToServiceRecord(SPP)
        s.connect(); sock = s
        KateState.link.value = Link.BLUETOOTH; true
    } catch (e: Exception) { sock = null; false }

    /** Called when the phone touches Kate's NFC tag. Payload: kate;bt=AA:BB:CC:DD:EE:FF;host=192.168.1.50 */
    fun onNfcTap(payload: String) {
        payload.split(';').forEach {
            val p = it.split('=', limit = 2)
            val k = p[0]; val v = p.getOrElse(1) { "" }
            if (k == "bt" && v.isNotBlank()) btMac = v
            if (k == "host" && v.isNotBlank()) host = v
        }
        KateState.nfcNear.value = true
        scope.launch {
            connectBluetooth()
            delay(20_000); KateState.nfcNear.value = false
        }
    }

    private fun viaWifi(a: String, t: String, x: String): Boolean = try {
        val body = JSONObject().put("app", a).put("title", t).put("text", x).toString()
            .toRequestBody("application/json".toMediaType())
        http.newCall(Request.Builder().url(base() + "/notify").post(body).build()).execute().use { it.isSuccessful }
    } catch (e: Exception) { false }

    private fun viaBt(a: String, t: String, x: String): Boolean = try {
        fun c(s: String) = s.replace('|', ' ').replace('\n', ' ')
        sock!!.outputStream.apply { write("N|${c(a)}|${c(t)}|${c(x)}\n".toByteArray()); flush() }; true
    } catch (e: Exception) { sock = null; false }

    suspend fun deliver(a: String, t: String, x: String): Boolean = withContext(Dispatchers.IO) {
        val order = if (KateState.link.value == Link.BLUETOOTH) listOf(::viaBt, ::viaWifi) else listOf(::viaWifi, ::viaBt)
        val ok = order.any { it(a, t, x) }
        if (ok) { KateState.forwarded.update { it + 1 }; KateState.lastNote.value = a }
        KateState.log.update { (listOf(Sent(a, t.ifBlank { x }, ok, System.currentTimeMillis())) + it).take(20) }
        refresh(); ok
    }

    /** Long poll for timers/automations from the PC bridge; assumes a 200 body is a WAV to play. */
    suspend fun pollAnnounce() = withContext(Dispatchers.IO) {
        try {
            http.newCall(Request.Builder().url(base() + "/announce").build()).execute().use { r ->
                if (r.code == 200) r.body?.bytes()?.let { if (it.size > 44) KateAudio.play(it) }
            }
        } catch (e: Exception) { delay(3000) }
    }
}
