package com.conzola.kate
import kotlinx.coroutines.flow.MutableStateFlow

enum class Link { OFFLINE, WIFI, BLUETOOTH }

data class Sent(val app: String, val title: String, val ok: Boolean, val time: Long)

object KateState {
    val log = MutableStateFlow<List<Sent>>(emptyList())
    val link = MutableStateFlow(Link.OFFLINE)
    val nfcNear = MutableStateFlow(false)
    val listening = MutableStateFlow(false)
    val forwarded = MutableStateFlow(0)
    val lastNote = MutableStateFlow("")
}
