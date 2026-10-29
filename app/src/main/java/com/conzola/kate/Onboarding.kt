package com.conzola.kate

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val STEPS = 5

@Composable
fun Onboarding(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(STEPS) { i -> Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (i <= step) Amber else Line)) }
        }
        Box(Modifier.weight(1f)) {
            AnimatedContent(step, transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally { dir * it / 6 } + fadeIn()) togetherWith (slideOutHorizontally { -dir * it / 6 } + fadeOut())
            }, label = "step") { s ->
                when (s) {
                    0 -> Welcome { step = 1 }
                    1 -> ConnectStep { step = 2 }
                    2 -> AccessStep { step = 3 }
                    3 -> LinkStep { step = 4 }
                    else -> ReadyStep(onDone)
                }
            }
        }
    }
}

@Composable
private fun StepFrame(title: String, body: String, primary: String, primaryEnabled: Boolean = true, onPrimary: () -> Unit,
                      secondary: String? = null, onSecondary: () -> Unit = {}, content: @Composable ColumnScope.() -> Unit = {}) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 32.dp)) {
            Text(title, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light)
            Spacer(Modifier.height(12.dp))
            Text(body, color = Mute, fontSize = 16.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(28.dp))
            content()
        }
        PillButton(primary, primaryEnabled, onPrimary)
        Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
            if (secondary != null) GhostButton(secondary, onClick = onSecondary)
        }
    }
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { KateFront(Modifier.fillMaxWidth(), .3f) }
        Text("Meet Kate", fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(12.dp))
        Text("Your local voice assistant, now on your phone. Kate reads your notifications out loud and answers when you talk to her.",
            color = Mute, fontSize = 16.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(32.dp))
        PillButton("Get started", onClick = onNext)
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun ConnectStep(onNext: () -> Unit) {
    var host by remember { mutableStateOf(KateLink.host) }
    var state by remember { mutableIntStateOf(0) } // 0 idle, 1 testing, 2 ok, 3 failed
    val scope = rememberCoroutineScope()
    StepFrame("Find Kate",
        "Enter the IP address of the PC that runs Kate. On Windows, open a terminal, run ipconfig and copy the IPv4 address.",
        primary = when (state) { 2 -> "Continue"; 1 -> "Testing…"; else -> "Test connection" },
        primaryEnabled = host.isNotBlank() && state != 1,
        onPrimary = {
            if (state == 2) onNext() else {
                KateLink.host = host; state = 1
                scope.launch { state = if (KateLink.test(host)) 2 else 3 }
            }
        }, secondary = "Skip for now", onSecondary = onNext) {
        KateField(host, { host = it; state = 0 }, "PC address", "192.168.1.50", KeyboardType.Uri)
        Spacer(Modifier.height(14.dp))
        when (state) {
            2 -> Text("Kate answered. You're connected.", color = Amber, fontSize = 15.sp)
            3 -> Text("No answer. Check the address, that your phone is on the same Wi-Fi, and that Kate's server is running on port 8000.", color = Mute, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun AccessStep(onNext: () -> Unit) {
    StepFrame("Let Kate listen in",
        "Kate needs a few permissions to read your notifications and stay running. Reading notifications is the only required one.",
        primary = "Continue", onPrimary = onNext) { PermissionsList() }
}

@Composable
private fun LinkStep(onNext: () -> Unit) {
    var mac by remember { mutableStateOf(KateLink.btMac) }
    StepFrame("Tap to link",
        "Touch your phone to Kate and the connection moves to Bluetooth, even away from home Wi-Fi. This part is still in development, so you can set it up later.",
        primary = "Continue", onPrimary = { KateLink.btMac = mac; onNext() }) {
        NfcPulse()
        Spacer(Modifier.height(8.dp))
        Text("In development", color = Amber, fontSize = 13.sp,
            modifier = Modifier.clip(CircleShape).background(PanelHi).padding(horizontal = 12.dp, vertical = 6.dp))
        Spacer(Modifier.height(18.dp))
        KateField(mac, { mac = it }, "Bluetooth address (optional)", "AA:BB:CC:DD:EE:FF")
    }
}

@Composable
private fun ReadyStep(onDone: () -> Unit) {
    val ctx = LocalContext.current
    StepFrame("Kate is ready", "You can change any of this later in settings.", primary = "Open Kate", onPrimary = onDone) {
        Column(Modifier.fillMaxWidth().card()) {
            Summary("Kate's address", KateLink.host.ifBlank { "Not set" }, KateLink.host.isNotBlank())
            HorizontalDividerLine()
            Summary("Reading notifications", if (Perms.listener(ctx)) "On" else "Needs access", Perms.listener(ctx))
            HorizontalDividerLine()
            Summary("Microphone", if (Perms.mic(ctx)) "On" else "Off", Perms.mic(ctx))
        }
    }
}

@Composable
private fun HorizontalDividerLine() = Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))

@Composable
private fun Summary(label: String, value: String, ok: Boolean) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = if (ok) Amber else Mute, fontSize = 15.sp)
    }
}
