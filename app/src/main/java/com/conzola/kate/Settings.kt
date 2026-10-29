package com.conzola.kate

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Intent

@Composable
fun PermissionsList() {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val refresh = tick
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = if (refresh >= 0) 0.dp else 0.dp)) {
        PermRow("Read notifications", "Required. Lets Kate hear what your phone receives.", Perms.listener(ctx)) { Perms.openListener(ctx) }
        PermRow("Microphone", "For talking to Kate from the app.", Perms.mic(ctx)) { ask.launch(Manifest.permission.RECORD_AUDIO) }
        PermRow("Show Kate's status", "A quiet notification that keeps Kate running.", Perms.notif(ctx)) { ask.launch(Manifest.permission.POST_NOTIFICATIONS) }
        PermRow("Bluetooth", "For linking to Kate after a tap.", Perms.bt(ctx)) { ask.launch(Manifest.permission.BLUETOOTH_CONNECT) }
        PermRow("Stay running", "Stops the phone from putting Kate to sleep.", Perms.battery(ctx)) { Perms.requestBattery(ctx) }
    }
}

@Composable
private fun PermRow(title: String, sub: String, granted: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().card().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(sub, color = Mute, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        if (granted) Icon(Icons.Default.Check, "Allowed", tint = Amber)
        else Text("Allow", color = Amber, fontSize = 14.sp, modifier = Modifier.clip(CircleShape)
            .border(1.dp, Amber.copy(alpha = .7f), CircleShape).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onApps: () -> Unit, onRerun: () -> Unit) {
    var host by remember { mutableStateOf(KateLink.host) }
    var mac by remember { mutableStateOf(KateLink.btMac) }
    var fwd by remember { mutableStateOf(KateLink.forward) }
    var state by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        TopBar("Settings", onBack)
        SectionTitle("Connection")
        KateField(host, { host = it; state = 0 }, "PC address", "192.168.1.50", KeyboardType.Uri)
        Spacer(Modifier.height(12.dp))
        KateField(mac, { mac = it }, "Bluetooth address", "AA:BB:CC:DD:EE:FF")
        Spacer(Modifier.height(16.dp))
        PillButton(if (state == 1) "Testing…" else "Save and test", enabled = state != 1) {
            KateLink.host = host; KateLink.btMac = mac; state = 1
            scope.launch { state = if (KateLink.test(host)) 2 else 3; withContext(Dispatchers.IO) { KateLink.refresh() } }
        }
        when (state) {
            2 -> Text("Kate answered.", color = Amber, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
            3 -> Text("No answer from that address.", color = Mute, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        }

        SectionTitle("Notifications")
        Column(Modifier.fillMaxWidth().card()) {
            Row(Modifier.fillMaxWidth().clickable { fwd = !fwd; KateLink.forward = fwd }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Read notifications aloud", fontSize = 16.sp); Text("Sends them to Kate as they arrive", color = Mute, fontSize = 13.sp) }
                Switch(fwd, null, colors = kateSwitch())
            }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(Line))
            Row(Modifier.fillMaxWidth().clickable(onClick = onApps).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Choose apps", fontSize = 16.sp); Text("Mute the ones you don't want read out", color = Mute, fontSize = 13.sp) }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Mute)
            }
        }

        SectionTitle("Access")
        PermissionsList()

        SectionTitle("Setup")
        Row(Modifier.fillMaxWidth().pressable(true, onRerun).card().padding(16.dp)) { Text("Run setup again", fontSize = 16.sp) }
        Text("Kate for Android 0.3", color = Mute, fontSize = 13.sp, modifier = Modifier.padding(vertical = 24.dp))
    }
}

private class AppRow(val pkg: String, val label: String, val icon: ImageBitmap)

@Composable
fun AppsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    var blocked by remember { mutableStateOf(KateLink.blocked) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != ctx.packageName }
                .map { AppRow(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm).toBitmap(96, 96).asImageBitmap()) }
                .sortedBy { it.label.lowercase() }
        }
    }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)) {
        TopBar("Apps", onBack)
        Text("Kate reads out notifications from the apps switched on.", color = Mute, fontSize = 14.sp, modifier = Modifier.padding(bottom = 14.dp))
        KateField(q, { q = it }, "Search apps")
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(apps.filter { it.label.contains(q, ignoreCase = true) }, key = { it.pkg }) { a ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(a.icon, null, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(14.dp))
                    Text(a.label, fontSize = 16.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Switch(a.pkg !in blocked, { on ->
                        blocked = if (on) blocked - a.pkg else blocked + a.pkg
                        KateLink.blocked = blocked
                    }, colors = kateSwitch())
                }
            }
        }
    }
}
