package com.seyit474.tmvpn.ping

import com.seyit474.tmvpn.model.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Sunucuların gerçek bağlantı gecikmesini ölçer.
 *
 * ICMP ping Android'de root gerektirir, o yüzden TCP ping kullanıyoruz:
 * SYN gönder → SYN/ACK gelene kadar geçen süre. Bu, gerçek kullanım
 * gecikmesine en yakın metrik.
 *
 * Tüm sunuculara aynı anda paralel istek atılır, sonuçlar sıralı döner.
 */
class ServerPinger(
    private val timeoutMs: Int = 3000,
    private val attempts: Int = 2
) {

    data class Result(
        val config: ServerConfig,
        val latencyMs: Long,        // -1L = ulaşılamadı
        val isReachable: Boolean
    )

    suspend fun pingAll(configs: List<ServerConfig>): List<Result> = coroutineScope {
        configs.map { cfg ->
            async(Dispatchers.IO) { pingOne(cfg) }
        }.map { it.await() }
            .sortedWith(
                compareByDescending<Result> { it.isReachable }
                    .thenBy { it.latencyMs }
            )
    }

    suspend fun pickFastest(configs: List<ServerConfig>): ServerConfig? =
        pingAll(configs).firstOrNull { it.isReachable }?.config

    private suspend fun pingOne(cfg: ServerConfig): Result = withContext(Dispatchers.IO) {
        var best = Long.MAX_VALUE
        var reachable = false
        repeat(attempts) {
            val t = measureTcpHandshake(cfg.address, cfg.port)
            if (t != null) {
                reachable = true
                if (t < best) best = t
            }
        }
        Result(
            config = cfg,
            latencyMs = if (reachable) best else -1L,
            isReachable = reachable
        )
    }

    private suspend fun measureTcpHandshake(host: String, port: Int): Long? =
        withTimeoutOrNull(timeoutMs.toLong()) {
            runCatching {
                val socket = Socket()
                val start = System.currentTimeMillis()
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val elapsed = System.currentTimeMillis() - start
                socket.close()
                elapsed
            }.getOrNull()
        }
}
