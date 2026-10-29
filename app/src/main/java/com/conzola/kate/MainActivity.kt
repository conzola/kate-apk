package com.conzola.kate

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s); enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        KateLink.init(this); handleNfc(intent)
        setContent { KateTheme { KateRoot() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleNfc(intent) }

    @Suppress("DEPRECATION")
    private fun handleNfc(i: Intent) {
        if (i.action != NfcAdapter.ACTION_NDEF_DISCOVERED) return
        val msg = i.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.firstOrNull() as? NdefMessage ?: return
        msg.records.firstOrNull()?.let { KateLink.onNfcTap(String(it.payload)) }
    }
}
