package com.telo.vpn.service

import android.content.Context
import android.net.LocalServerSocket
import android.net.VpnService
import android.util.Log
import java.io.File

interface XrayCoreEngine {
    fun start(configJson: String, tunFd: Int): Boolean
    fun stop()
    fun isRunning(): Boolean
    fun queryStats(tag: String, uplink: Boolean): Long
}

/**
 * Gerçek motor — libXray.aar (AndroidLibXrayLite) ile çalışır.
 * Reflection kullanılır; AAR olmadan da derlenir.
 *
 * API (gomobile static methods on libv2ray.Libv2ray):
 *   initCoreEnv(dataDir, logDir)
 *   startLoop(configPath, tunFd)  — veya startLoop(configPath, tunFd, protectPath)
 *   stopLoop()
 *   queryStats(tag, direct)
 */
class LibXrayEngine(private val service: VpnService) : XrayCoreEngine {

    private val protectServer = SocketProtectServer(service)
    private var running = false

    companion object {
        private const val TAG = "TeloVPN/Xray"
        private var initialized = false

        fun initEnv(context: Context) {
            if (initialized) return
            runCatching {
                val lib = Class.forName("libv2ray.Libv2ray")
                // Log tüm mevcut metodları — hangi API versiyonu olduğunu görmek için
                lib.methods.forEach { Log.d(TAG, "API: ${it.name}(${it.parameterTypes.joinToString { p -> p.simpleName }})") }
                lib.getMethod("initCoreEnv", String::class.java, String::class.java)
                    .invoke(null, context.filesDir.absolutePath, "")
                initialized = true
                val ver = runCatching { lib.getMethod("checkVersionX").invoke(null) as String }.getOrDefault("?")
                Log.i(TAG, "Xray env hazır — $ver")
            }.onFailure { Log.e(TAG, "initEnv hatası: ${it.message}") }
        }
    }

    override fun start(configJson: String, tunFd: Int): Boolean {
        return runCatching {
            val lib = Class.forName("libv2ray.Libv2ray")

            // Config JSON'ı dosyaya yaz (Xray genellikle dosya yolu bekler)
            val configFile = File(service.filesDir, "xray_config.json")
            configFile.writeText(configJson)
            val configPath = configFile.absolutePath

            // Socket protect sunucusunu başlat
            val protectName = protectServer.start()

            val ok = tryStartLoop(lib, configPath, tunFd, protectName)
            if (ok) {
                running = true
                Log.i(TAG, "Xray başlatıldı (fd=$tunFd)")
            } else {
                Log.e(TAG, "startLoop false döndü — logcat'te API metodlarını kontrol et")
            }
            ok
        }.onFailure { e ->
            Log.e(TAG, "start hatası: ${e.javaClass.simpleName}: ${e.message}", e)
        }.getOrDefault(false)
    }

    /**
     * Farklı imzaları dener; gomobile versiyona göre int/long veya 2/3 param değişebilir.
     */
    private fun tryStartLoop(lib: Class<*>, configPath: String, fd: Int, protectName: String): Boolean {
        // 1) startLoop(String, int, String) — 3 parametre, protect socket yolu ile
        runCatching {
            return lib.getMethod("startLoop",
                String::class.java, Int::class.javaPrimitiveType, String::class.java)
                .invoke(null, configPath, fd, protectName) as? Boolean ?: true
        }
        // 2) startLoop(String, long, String)
        runCatching {
            return lib.getMethod("startLoop",
                String::class.java, Long::class.javaPrimitiveType, String::class.java)
                .invoke(null, configPath, fd.toLong(), protectName) as? Boolean ?: true
        }
        // 3) startLoop(String, int) — 2 parametre
        runCatching {
            return lib.getMethod("startLoop",
                String::class.java, Int::class.javaPrimitiveType)
                .invoke(null, configPath, fd) as? Boolean ?: true
        }
        // 4) startLoop(String, long)
        runCatching {
            return lib.getMethod("startLoop",
                String::class.java, Long::class.javaPrimitiveType)
                .invoke(null, configPath, fd.toLong()) as? Boolean ?: true
        }
        // 5) startXray(String, int) — alternatif isim
        runCatching {
            lib.getMethod("startXray",
                String::class.java, Int::class.javaPrimitiveType)
                .invoke(null, configPath, fd)
            return true
        }
        Log.e(TAG, "Hiçbir startLoop imzası bulunamadı — logcat'te API metodlarına bak")
        return false
    }

    override fun stop() {
        running = false
        runCatching {
            val lib = Class.forName("libv2ray.Libv2ray")
            runCatching { lib.getMethod("stopLoop").invoke(null) }
            runCatching { lib.getMethod("stopXray").invoke(null) }
        }
        protectServer.stop()
        Log.i(TAG, "Xray durduruldu")
    }

    override fun isRunning(): Boolean = running

    override fun queryStats(tag: String, uplink: Boolean): Long = runCatching {
        Class.forName("libv2ray.Libv2ray")
            .getMethod("queryStats", String::class.java, String::class.java)
            .invoke(null, tag, if (uplink) "uplink" else "downlink") as? Long
    }.getOrDefault(0L) ?: 0L
}

/**
 * Go core'un socket protect isteklerini karşılayan Unix domain soket sunucusu.
 * fd'yi ancillary data (SCM_RIGHTS) olarak alır, VpnService.protect() çağırır.
 */
private class SocketProtectServer(private val service: VpnService) {
    private val name = "telo_vpn_protect"
    @Volatile private var active = false
    private var server: LocalServerSocket? = null

    fun start(): String {
        runCatching { server?.close() }
        active = true
        val srv = LocalServerSocket(name)
        server = srv
        Thread {
            while (active) {
                runCatching {
                    val conn = srv.accept()
                    Thread {
                        runCatching {
                            conn.ancillaryFileDescriptors?.forEach { fd ->
                                service.protect(fd)
                                Log.d("TeloVPN/Protect", "fd protected via Unix socket")
                            }
                        }
                        runCatching { conn.close() }
                    }.also { it.daemon = true }.start()
                }
            }
        }.also { it.daemon = true; it.start() }
        return name  // Go core bu isimle bağlanır (abstract namespace: \0telo_vpn_protect)
    }

    fun stop() {
        active = false
        runCatching { server?.close() }
        server = null
    }
}

/** libXray.aar olmadığında kullanılan stub. */
class StubXrayEngine : XrayCoreEngine {
    private var running = false
    override fun start(configJson: String, tunFd: Int): Boolean {
        Log.w("TeloVPN", "StubXrayEngine — libXray.aar yok")
        running = true
        return true
    }
    override fun stop() { running = false }
    override fun isRunning() = running
    override fun queryStats(tag: String, uplink: Boolean) = 0L
}

fun createXrayEngine(context: Context): XrayCoreEngine {
    return try {
        Class.forName("libv2ray.Libv2ray")
        LibXrayEngine.initEnv(context)
        LibXrayEngine(context as VpnService)
    } catch (_: ClassNotFoundException) {
        Log.w("TeloVPN", "libv2ray bulunamadı → StubXrayEngine")
        StubXrayEngine()
    }
}
