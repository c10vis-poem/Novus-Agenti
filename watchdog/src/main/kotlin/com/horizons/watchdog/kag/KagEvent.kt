package com.horizons.watchdog.kag

import kotlinx.serialization.Serializable

@Serializable
data class KagEvent(
    val ts: String,
    val event_type: String,
    val device: Device,
    val killer_pkg: String? = null,
    val recovery_layer_hit: Int,
    val latency_ms: Long,
    val outcome: String,
    val context: Map<String, String> = emptyMap()
) {
    @Serializable
    data class Device(
        val oem: String,
        val model: String,
        val chipset: String,
        val android_ver: Int
    )
}
