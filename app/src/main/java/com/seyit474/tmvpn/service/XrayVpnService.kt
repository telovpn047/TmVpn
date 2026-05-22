package com.seyit474.tmvpn.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.seyit474.tmvpn.R

/**
 * VpnService — tun arayüzünü açar, Xray çekirdeğini başlatır,
 * tun trafiğini tun2socks ile Xray'in SOCKS portuna (10808) yönlendirir.
 *
 * BU İSKELET HENÜZ ÇALIŞMIYOR — eksik parçalar:
 *  1. libXray.aar (Xray-core gomobile derlemesi)
 *  2. libtun2socks.so (hev-socks5-tunnel veya badvpn-tun2socks)
 *  3. JNI bağlama
 *
 * Bir sonraki adımda bunları ekleyeceğiz.
 */
class XrayVpnService : VpnService() {

    private var tunInterface: ParcelFileDescriptor? = null

    companion object {
        private const val CHANNEL_ID = "tmvpn_status"
        private const val NOTIF_ID = 1
        const val EXTRA_CONFIG_JSON = "config_json"

        fun start(ctx: Context, configJson: String) {
            val i = Intent(ctx, XrayVpnService::class.java).apply {
                putExtra(EXTRA_CONFIG_JSON, configJson)
            }
            ctx.startForegroundService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, XrayVpnService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Bağlanıyor..."))

        val configJson = intent?.getStringExtra(EXTRA_CONFIG_JSON) ?: return START_NOT_STICKY

        // TODO(aşama 2):
        // 1. tun arayüzünü kur:
        //    tunInterface = Builder()
        //        .setSession("TmVpn")
        //        .addAddress("10.10.10.1", 32)
        //        .addRoute("0.0.0.0", 0)
        //        .addDnsServer("1.1.1.1")
        //        .setMtu(1500)
        //        .establish()
        //
        // 2. Xray başlat:
        //    LibXray.startXray(configJson)
        //
        // 3. tun2socks başlat:
        //    Tun2Socks.start(tunInterface.fd, "127.0.0.1", 10808)

        return START_STICKY
    }

    override fun onDestroy() {
        // TODO: LibXray.stopXray(); Tun2Socks.stop()
        tunInterface?.close()
        tunInterface = null
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "VPN Durumu", NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TmVpn")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_vpn_ic)
            .setOngoing(true)
            .build()
    }
}
