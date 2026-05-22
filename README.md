# TmVpn — Android VPN Uygulaması

Xray çekirdeği üzerinde VLESS / VMess / Shadowsocks destekleyen,
abonelik tabanlı, otomatik en hızlı sunucu seçen Android uygulaması.

## Tamamlanan (Aşama 1)

- ✅ Gradle yapılandırması (Kotlin + Compose + AGP 8.5)
- ✅ `ServerConfig` ortak veri modeli
- ✅ `ConfigParser` — VLESS / VMess / Shadowsocks link parser
  - VLESS REALITY parametreleri (pbk, sid, fp, sni, flow)
  - VMess JSON base64
  - Shadowsocks (modern + eski format)
  - Subscription base64 otomatik decode
- ✅ `SubscriptionFetcher` — OkHttp ile abonelik çekme
- ✅ `ServerPinger` — paralel TCP handshake latency ölçümü
- ✅ `XrayConfigBuilder` — Xray-core JSON üretici (SOCKS 10808 + DNS 10853)
- ✅ `VpnViewModel` — UI state machine
- ✅ Compose UI — büyük yuvarlak BAĞLAN butonu + sunucu listesi (renkli ping)
- ✅ `XrayVpnService` iskeleti

## Sonraki Adım: Aşama 2 — Xray entegrasyonu

Bu üç parça gerekiyor:

### 1. libXray.aar derleme

Xray-core'u Android için derlemek lazım. İki yol var:

**A) Hazır AAR kullan (önerilir):**
- `github.com/2dust/AndroidLibXrayLite` reposundan releases altındaki AAR
- Veya `github.com/xtls/libxray` (resmi)
- `app/libs/libXray.aar` olarak koy, gradle'a ekle:
  ```kotlin
  implementation(files("libs/libXray.aar"))
  ```

**B) Kendin derle:**
- Linux makinede:
  ```bash
  git clone https://github.com/xtls/libxray
  cd libxray
  # Go 1.21+ ve gomobile gerekli
  go install golang.org/x/mobile/cmd/gomobile@latest
  gomobile init
  gomobile bind -target=android -androidapi=24 \
    -o libXray.aar github.com/xtls/libxray
  ```

### 2. tun2socks

Tun arayüzü trafiğini SOCKS proxy'ye yönlendirmek için:
- `hev-socks5-tunnel` (önerilir, en hızlı) — `github.com/heiher/hev-socks5-tunnel`
- Önceden derlenmiş `.so` dosyaları için: `2dust/AndroidLibXrayLite`'taki releases

`.so` dosyalarını `app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86_64}/` altına koy.

### 3. JNI bağlama

`XrayVpnService.kt` içindeki TODO'ları doldur:
```kotlin
import libv2ray.Libv2ray   // libXray.aar paketi

val v2rayPoint = Libv2ray.newV2RayPoint(callback, false)
v2rayPoint.configureFileContent = configJson
v2rayPoint.runLoop(false)
```

## Yapılandırma

`app/build.gradle.kts` içindeki `SUBSCRIPTION_URL` placeholder'ını gerçek
`doc.google.com` endpoint'inle değiştir:

```kotlin
buildConfigField(
    "String",
    "SUBSCRIPTION_URL",
    "\"https://doc.google.com/document/d/<DOC_ID>/export?format=txt\""
)
```

## Test (Aşama 1 — Xray olmadan)

UI ve ping mantığı şu an çalışır durumda:
1. Android Studio'da projeyi aç
2. `SUBSCRIPTION_URL`'i ayarla
3. Çalıştır → "Sunucular alınıyor → Test ediliyor → Liste" akışı görünmeli
4. BAĞLAN butonu VPN izni isteyecek ama henüz tünel kurmuyor

## Mimari

```
┌─────────────────────────────────────┐
│  Compose UI (MainActivity)          │
│        ↓                            │
│  VpnViewModel (state machine)       │
└─────────────────────────────────────┘
        ↓                ↓
┌──────────────┐  ┌──────────────┐
│ Subscription │  │ ServerPinger │
│   Fetcher    │  │  (TCP ping)  │
└──────────────┘  └──────────────┘
        ↓
  ConfigParser → List<ServerConfig>
        ↓
  XrayConfigBuilder → Xray JSON
        ↓
  XrayVpnService:
    ├── VpnService.Builder → tun fd
    ├── libXray (Xray-core) → SOCKS :10808
    └── tun2socks: tun ↔ SOCKS :10808
```

## Sonraki konular

- [ ] libXray.aar entegrasyonu
- [ ] tun2socks bağlama
- [ ] Bağlantı testi (gerçek tünel)
- [ ] Trafik istatistikleri (upload/download)
- [ ] Kill switch
- [ ] Subscription güncelleme zamanlayıcısı
- [ ] Türkmence/Türkçe çeviri
- [ ] App icon + splash screen
- [ ] ProGuard kuralları
- [ ] Release imzalama
