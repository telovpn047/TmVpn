package com.telo.wg.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TunnelConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val configText: String
)

fun TunnelConfig.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("configText", configText)
}

fun JSONObject.toTunnelConfig() = TunnelConfig(
    id = getString("id"),
    name = getString("name"),
    configText = getString("configText")
)

fun List<TunnelConfig>.toJsonString(): String =
    JSONArray().also { arr -> forEach { arr.put(it.toJson()) } }.toString()

fun tunnelListFromJson(json: String): List<TunnelConfig> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getJSONObject(it).toTunnelConfig() }
    } catch (_: Exception) {
        emptyList()
    }
}

fun parseTunnelName(configText: String): String =
    configText.lines()
        .firstOrNull { it.trimStart().startsWith("#") && it.contains("Name", ignoreCase = true) }
        ?.substringAfter("=")?.trim()
        ?: "Tunel"
