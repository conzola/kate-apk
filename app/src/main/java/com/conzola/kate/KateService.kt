package com.conzola.kate

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.*

/** Always-on foreground service, same idea as Mi Fitness: a quiet ongoing notification keeps it alive. */
class KateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false
    override fun onBind(i: Intent?) = null

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        KateLink.init(this)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("kate", "Kate connection", NotificationManager.IMPORTANCE_MIN))
        val type = if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE else 0
        ServiceCompat.startForeground(this, 1, build(), type)
        if (!started) {
            started = true
            scope.launch { while (isActive) { KateLink.refresh(); delay(10_000) } }
            scope.launch { while (isActive) { if (KateLink.playHere && KateState.link.value == Link.WIFI) KateLink.pollAnnounce() else delay(5000) } }
            scope.launch { KateState.link.collect { nm.notify(1, build()) } }
        }
        return START_STICKY
    }

    private fun build(): Notification {
        val text = when (KateState.link.value) {
            Link.WIFI -> "Connected over home Wi-Fi"; Link.BLUETOOTH -> "Connected over Bluetooth"; Link.OFFLINE -> "Offline"
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "kate").setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Kate").setContentText(text)
            .setOngoing(true).setContentIntent(open).setPriority(NotificationCompat.PRIORITY_MIN).build()
    }

    override fun onDestroy() { scope.cancel() }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { c.startForegroundService(Intent(c, KateService::class.java)) }
}
