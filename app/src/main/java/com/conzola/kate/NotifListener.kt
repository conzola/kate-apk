package com.conzola.kate

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.launch

/** Every normal notification on the phone is handed to Kate. */
class NotifListener : NotificationListenerService() {
    private var lastKey = ""

    override fun onListenerConnected() {
        KateLink.init(this); startForegroundService(Intent(this, KateService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        KateLink.init(this)
        val n = sbn.notification
        if (!KateLink.forward || sbn.packageName == packageName || sbn.packageName in KateLink.blocked) return
        if (n.flags and (Notification.FLAG_ONGOING_EVENT or Notification.FLAG_GROUP_SUMMARY) != 0) return
        val title = n.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = n.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val key = "${sbn.packageName}|$title|$text"
        if (key == lastKey) return
        lastKey = key
        val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString() } catch (e: Exception) { sbn.packageName }
        KateLink.scope.launch { KateLink.deliver(label, title, text) }
    }
}
