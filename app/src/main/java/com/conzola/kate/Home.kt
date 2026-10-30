package com.conzola.kate

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings as SettingsIcon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Home(onSettings: () -> Unit, onApps: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val link by KateState.link.collectAsState()
    val near by KateState.nfcNear.collectAsState()
    val phase by KateState.phase.collectAsState()
    val message by KateState.message.collectAsState()
    val outdated by KateState.serverOutdated.collectAsState()
    val sent by KateState.forwarded.collectAsState()
    val log by KateState.log.collectAsState()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++; scope.launch(Dispatchers.IO) { KateLink.refresh() } }
    var fwd by remember { mutableStateOf(KateLink.forward) }
    val host = remember(tick, link) { KateLink.host }
    val listenerOn = remember(tick) { Perms.listener(ctx) }
    val unrestricted = remember(tick) { Perms.battery(ctx) }
    val muted = remember(tick) { KateLink.blocked.size }
    val btMac = remember(tick, link) { KateLink.btMac }
    val busy = phase != Phase.IDLE
    LaunchedEffect(message) { if (message != null) { delay(3500); KateState.message.value = null } }

    val askMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) scope.launch { KateAudio.talk() } }
    val talk: () -> Unit = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (host.isBlank()) onSettings()
        else if (!Perms.mic(ctx)) askMic.launch(Manifest.permission.RECORD_AUDIO)
        else { scope.launch { KateAudio.talk() } }
    }

    val breathe = rememberInfiniteTransition(label = "breath")
    val b by breathe.animateFloat(.8f, 1.2f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    val glow by animateFloatAsState(if (busy) .5f else if (near) .4f else .24f, tween(600), label = "glow")

    val title = when {
        phase == Phase.LISTENING -> "Listening"
        phase == Phase.THINKING -> "Thinking"
        phase == Phase.SPEAKING -> "Speaking"
        near -> "Kate is right here"
        host.isBlank() -> "Not set up"
        link == Link.OFFLINE -> "Out of reach"
        else -> "Online"
    }
    val cta: Pair<String, () -> Unit>? = when {
        host.isBlank() -> "Add Kate's address" to onSettings
        link == Link.OFFLINE -> "Can't reach $host" to onSettings
        outdated -> "Server needs the notify update" to onSettings
        else -> null
    }
    val linkName = when (link) { Link.WIFI -> "Wi-Fi"; Link.BLUETOOTH -> "Bluetooth"; Link.OFFLINE -> "Offline" }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Kate", fontSize = 34.sp, fontWeight = FontWeight.Light, letterSpacing = 4.sp)
                Text("Voice assistant", color = Mute, fontSize = 13.sp)
            }
            Box(Modifier.size(46.dp).pressable(true, onSettings).clip(CircleShape).background(PanelHi), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.SettingsIcon, "Settings", tint = Ink, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(18.dp))

        // Hero: the glow sits behind everything on the card, so nothing has a visible edge.
        val heroShape = RoundedCornerShape(32.dp)
        Column(Modifier.fillMaxWidth().clip(heroShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF18181B), Color(0xFF0B0B0D))))
            .amberGlow({ glow * b }, cy = .42f, ry = .5f)
            .border(1.dp, Brush.verticalGradient(listOf(Amber.copy(alpha = .38f), Line, Line)), heroShape)
            .padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(Modifier.clip(CircleShape).background(Color(0x55000000)).border(1.dp, Line, CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(link != Link.OFFLINE || busy)
                Spacer(Modifier.width(9.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            KateFront(Modifier.fillMaxWidth().padding(vertical = 8.dp), pad = 14.dp)
            if (cta != null) {
                val (label, go) = cta
                Row(Modifier.padding(bottom = 14.dp).pressable(true, go).clip(CircleShape)
                    .background(Amber.copy(alpha = .12f)).border(1.dp, Amber.copy(alpha = .4f), CircleShape)
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = Amber, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Amber, modifier = Modifier.size(18.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Stat(linkName, "Connection", Modifier.weight(1f), if (link == Link.OFFLINE) Mute else Ink)
                Box(Modifier.width(1.dp).height(30.dp).background(Line))
                Stat("$sent", "Read aloud", Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(30.dp).background(Line))
                Stat("$muted", "Apps muted", Modifier.weight(1f))
            }
        }

        // Talk
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(68.dp)
            .then(if (!busy) Modifier.shadow(20.dp, CircleShape, ambientColor = Amber, spotColor = Amber) else Modifier)
            .pressable(!busy, talk)
            .clip(CircleShape)
            .background(if (busy) Brush.horizontalGradient(listOf(Panel, Panel)) else Brush.horizontalGradient(listOf(Amber, AmberHi)))
            .border(1.dp, if (busy) Amber else Color.Transparent, CircleShape),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (message == null) { Waveform(busy, if (busy) Amber else Bg); Spacer(Modifier.width(14.dp)) }
            Text(message ?: when (phase) {
                Phase.LISTENING -> "Listening…"
                Phase.THINKING -> "Thinking…"
                Phase.SPEAKING -> "Speaking…"
                Phase.IDLE -> "Talk to Kate"
            }, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = if (busy) Ink else Bg)
        }

        // Quick controls
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Icons.Default.Notifications, "Alerts",
                if (!listenerOn) "Allow access" else if (fwd) "Read aloud" else "Paused", fwd && listenerOn, Modifier.weight(1f)) {
                if (!listenerOn) Perms.openListener(ctx) else { fwd = !fwd; KateLink.forward = fwd }
            }
            Tile(KateIcons.Grid, "Apps", if (muted == 0) "All on" else "$muted muted", false, Modifier.weight(1f)) { onApps() }
            Tile(KateIcons.Bluetooth, "Bluetooth",
                if (link == Link.BLUETOOTH) "Linked" else if (btMac.isBlank()) "Set up" else "Connect", link == Link.BLUETOOTH, Modifier.weight(1f)) {
                if (btMac.isBlank()) onSettings() else scope.launch(Dispatchers.IO) { KateLink.connectBluetooth() }
            }
            Tile(KateIcons.Bolt, "Background", if (unrestricted) "Always on" else "Allow", unrestricted, Modifier.weight(1f)) {
                if (!unrestricted) Perms.requestBattery(ctx)
            }
        }

        // Recent
        Text("Recent", color = Mute, fontSize = 14.sp, modifier = Modifier.padding(top = 30.dp, bottom = 12.dp))
        if (log.isEmpty()) {
            Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
                Text("Nothing yet", fontSize = 16.sp)
                Text("Notifications Kate reads out will show up here.", color = Mute, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        } else Column(Modifier.fillMaxWidth().card()) {
            log.forEachIndexed { i, s ->
                if (i > 0) Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Amber.copy(alpha = .13f)), contentAlignment = Alignment.Center) {
                        Text(s.app.take(1).uppercase(), color = Amber, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.app, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.title, color = Mute, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(s.time)), color = Mute, fontSize = 13.sp)
                        Text(if (s.ok) "Sent" else "Not sent", color = if (s.ok) Amber else Mute, fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier, valueColor: Color = Ink) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Light, maxLines = 1, color = valueColor)
        Text(label, color = Mute, fontSize = 12.sp)
    }
}

@Composable
private fun Tile(icon: ImageVector, label: String, sub: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.aspectRatio(.8f).pressable(true, onClick).clip(shape)
        .background(if (on) Amber.copy(alpha = .14f) else Panel)
        .border(1.dp, if (on) Amber.copy(alpha = .55f) else Line, shape),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = if (on) Amber else Mute, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (on) Ink else Mute, maxLines = 1)
        Text(sub, fontSize = 10.sp, color = if (on) Amber else Mute, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}
