package com.conzola.kate

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

val Bg = Color(0xFF050506)
val Panel = Color(0xFF111113)
val PanelHi = Color(0xFF1B1B1E)
val Line = Color(0xFF26262A)
val Ink = Color(0xFFF4F4F5)
val Amber = Color(0xFFE8782A)
val AmberHi = Color(0xFFF59A4D)
val Mute = Color(0xFF93939B)

enum class Screen { Onboarding, Home, Settings, Apps }

@Composable
fun KateTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(
        primary = Amber, onPrimary = Bg, background = Bg, onBackground = Ink,
        surface = Panel, onSurface = Ink, surfaceVariant = PanelHi, onSurfaceVariant = Mute, outline = Line),
    typography = KateTypography, content = content)

@Composable
fun KateRoot() {
    val ctx = LocalContext.current
    var screen by remember { mutableStateOf(if (KateLink.onboarded) Screen.Home else Screen.Onboarding) }
    BackHandler(enabled = screen == Screen.Settings || screen == Screen.Apps) {
        screen = if (screen == Screen.Apps) Screen.Settings else Screen.Home
    }
    LaunchedEffect(screen) {
        if (screen == Screen.Home) try { ctx.startForegroundService(Intent(ctx, KateService::class.java)) } catch (e: Exception) {}
    }
    // Surface sets the default content colour. Without it Compose falls back to black text.
    Surface(Modifier.fillMaxSize(), color = Bg, contentColor = Ink) {
        AnimatedContent(screen, transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(160)) }, label = "nav") { s ->
            when (s) {
                Screen.Onboarding -> Onboarding { KateLink.onboarded = true; screen = Screen.Home }
                Screen.Home -> Home(onSettings = { screen = Screen.Settings }, onApps = { screen = Screen.Apps })
                Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home }, onApps = { screen = Screen.Apps },
                    onRerun = { KateLink.onboarded = false; screen = Screen.Onboarding })
                Screen.Apps -> AppsScreen(onBack = { screen = Screen.Settings })
            }
        }
    }
}

fun Modifier.card(r: Dp = 22.dp): Modifier {
    val s = RoundedCornerShape(r)
    return this.clip(s).background(Panel).border(1.dp, Line, s)
}

/** Click with a small press-in scale instead of a ripple. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .96f else 1f, tween(110), label = "press")
    this.graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun PillButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(58.dp).pressable(enabled, onClick).clip(CircleShape)
        .background(if (enabled) Brush.horizontalGradient(listOf(Amber, AmberHi)) else Brush.horizontalGradient(listOf(PanelHi, PanelHi))),
        contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Bg else Mute, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GhostButton(text: String, color: Color = Mute, onClick: () -> Unit) {
    Box(Modifier.height(44.dp).pressable(true, onClick).padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
        Text(text, color = color, fontSize = 15.sp)
    }
}

@Composable
fun KateField(value: String, onChange: (String) -> Unit, label: String, hint: String = "", kb: KeyboardType = KeyboardType.Text) {
    OutlinedTextField(value, onChange, Modifier.fillMaxWidth(), label = { Text(label) },
        placeholder = { Text(hint, color = Mute) }, singleLine = true, shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = kb),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink, unfocusedTextColor = Ink,
            focusedBorderColor = Amber, unfocusedBorderColor = Line,
            focusedLabelColor = Amber, unfocusedLabelColor = Mute, cursorColor = Amber,
            focusedContainerColor = Panel, unfocusedContainerColor = Panel))
}

@Composable
fun kateSwitch() = SwitchDefaults.colors(checkedThumbColor = Bg, checkedTrackColor = Amber,
    uncheckedThumbColor = Mute, uncheckedTrackColor = PanelHi, uncheckedBorderColor = Line)

@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).pressable(true, onBack).clip(CircleShape).background(PanelHi), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun SectionTitle(text: String) = Text(text, color = Mute, fontSize = 14.sp, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))

/** The front panel of the Kate bar, flat and straight-on, over a softly breathing amber glow. */
@Composable
fun KateFront(modifier: Modifier = Modifier, glow: Float = .2f) {
    val t = rememberInfiniteTransition(label = "breath")
    val b by t.animateFloat(.75f, 1.25f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")
    Box(modifier.drawBehind {
        drawRect(Brush.radialGradient(listOf(Amber.copy(alpha = (glow * b).coerceIn(0f, 1f)), Color.Transparent), center, size.width * .62f))
    }.padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.kate_front), null, Modifier.fillMaxWidth().aspectRatio(2200f / 420f), contentScale = ContentScale.Fit)
    }
}

@Composable
fun Waveform(active: Boolean, color: Color, modifier: Modifier = Modifier.size(28.dp)) {
    val t = rememberInfiniteTransition(label = "w")
    val ph by t.animateFloat(0f, 6.2832f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "ph")
    Canvas(modifier) {
        val bars = floatArrayOf(.35f, .7f, 1f, .7f, .35f)
        val bw = size.width / 9f
        bars.forEachIndexed { i, b ->
            val k = if (active) (.5f + .5f * sin(ph + i * 1.1f)).coerceAtLeast(.2f) else 1f
            val len = size.height * b * k
            drawRoundRect(color, Offset(i * 2 * bw, (size.height - len) / 2), Size(bw, len), CornerRadius(bw / 2))
        }
    }
}

@Composable
fun NfcPulse() {
    val t = rememberInfiniteTransition(label = "nfc")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "p")
    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..2) {
                val q = (p + i / 3f) % 1f
                drawCircle(Amber.copy(alpha = (1f - q) * .55f), radius = q * size.height * .5f, center = center, style = Stroke(2.dp.toPx()))
            }
        }
        Image(painterResource(R.drawable.kate_front), null, Modifier.fillMaxWidth(.78f).aspectRatio(2200f / 420f))
    }
}
