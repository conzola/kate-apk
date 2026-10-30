package com.conzola.kate
import kotlinx.coroutines.flow.MutableStateFlow

enum class Link { OFFLINE, WIFI, BLUETOOTH }
enum class Phase { IDLE, LISTENING, THINKING, SPEAKING }

data class Sent(val app: String, val title: String, val ok: Boolean, val time: Long)

object KateState {
    val log = MutableStateFlow<List<Sent>>(emptyList())
    val link = MutableStateFlow(Link.OFFLINE)
    val nfcNear = MutableStateFlow(false)
    val phase = MutableStateFlow(Phase.IDLE)
    val forwarded = MutableStateFlow(0)
    val lastNote = MutableStateFlow("")
    /** One-line feedback shown on the talk button for a few seconds. */
    val message = MutableStateFlow<String?>(null)
    /** True when Kate's server answered 404 to /notify, i.e. it hasn't had the notify patch yet. */
    val serverOutdated = MutableStateFlow(false)
}
