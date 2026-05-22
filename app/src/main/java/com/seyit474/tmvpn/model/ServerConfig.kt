package com.seyit474.tmvpn.model

/**
 * Tüm desteklenen protokollerin ortak veri modeli.
 * VLESS, VMess ve Shadowsocks linklerini parse ettikten sonra
 * hepsi bu sınıfta tutulur.
 */
data class ServerConfig(
    val id: String,                 // benzersiz ID (hash)
    val remark: String,             // sunucu adı (örn: "TM-1")
    val protocol: Protocol,
    val address: String,            // host/IP
    val port: Int,
    val uuid: String? = null,       // VLESS/VMess için
    val password: String? = null,   // Shadowsocks için
    val method: String? = null,     // Shadowsocks encryption method
    val network: String = "tcp",    // tcp, ws, grpc, kcp...
    val security: String = "none",  // none, tls, reality
    val sni: String? = null,
    val fingerprint: String? = null, // chrome, firefox...
    val publicKey: String? = null,  // REALITY
    val shortId: String? = null,    // REALITY
    val flow: String? = null,       // xtls-rprx-vision
    val path: String? = null,       // ws/grpc path
    val host: String? = null,       // ws host header
    val alpn: String? = null,
    val raw: String                 // orijinal link (debug için)
) {
    enum class Protocol { VLESS, VMESS, SHADOWSOCKS }
}
