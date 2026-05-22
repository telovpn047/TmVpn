# GitHub Üzerinden APK Derleme — Kurulum

Bu döküman, kodu GitHub'a push edip bulutta APK derletmeyi anlatır.
Termux'tan veya PC'den, fark etmez.

---

## 1. GitHub'da repo oluştur

1. https://github.com/Seyit474 → **New repository**
2. Repository name: `TmVpn`
3. **Private** seç (subscription URL'in repoda olacak)
4. **Create repository** tıkla. Hiçbir dosya ekleme (README, .gitignore vb).

---

## 2. Lokal projeyi GitHub'a push et

Termux'ta veya PC'de:

```bash
# Projeyi açtığın klasöre gir
cd TmVpn

# Git başlat
git init
git branch -M main

# Tüm dosyaları ekle
git add .
git commit -m "Aşama 1: iskelet + parser + ping"

# GitHub'a bağla (KULLANICI ADI yerine Seyit474 olacak zaten)
git remote add origin https://github.com/Seyit474/TmVpn.git

# Push
git push -u origin main
```

İlk push'ta GitHub kullanıcı adı + **Personal Access Token** isteyecek
(şifre değil). Token'ı şuradan oluştur:
https://github.com/settings/tokens → **Generate new token (classic)**
→ `repo` ve `workflow` kutularını işaretle → kaydet, kopyala.

---

## 3. SUBSCRIPTION_URL'i ayarla

`app/build.gradle.kts` içindeki placeholder'ı değiştir:

```kotlin
buildConfigField(
    "String",
    "SUBSCRIPTION_URL",
    "\"https://doc.google.com/document/d/GERCEK_DOC_ID/export?format=txt\""
)
```

Sonra:
```bash
git add app/build.gradle.kts
git commit -m "Subscription URL ayarlandı"
git push
```

---

## 4. İlk Derleme — Debug APK

Push yaptıktan **30 saniye** sonra GitHub Actions otomatik tetiklenir.

1. https://github.com/Seyit474/TmVpn → **Actions** sekmesi
2. "Build Debug APK" workflow'unu gör — sarı (çalışıyor) → yeşil (bitti)
3. **5-8 dakika sürer** (ilk derleme yavaş, sonrakiler cache'ten 2-3 dk)
4. Tamamlandığında workflow sayfasının altında **Artifacts** kısmı:
   - `TmVpn-debug-apk` → tıkla, ZIP iner
   - ZIP'i aç, içinden APK çıkar
5. Telefonda APK'yı aç, "Bilinmeyen kaynaktan kur" izni ver

> **Not:** Artifacts 30 gün saklanır, sonra otomatik silinir.

---

## 5. Release Derlemesi — İmzalı APK

Production için (yani gerçekten dağıtacağın versiyon) imzalı APK lazım.
Aksi halde Android "Bu uygulama güvensiz" uyarısı verir.

### 5.1. Keystore oluştur (sadece bir kere)

PC veya Termux'ta:

```bash
# Termux'ta önce: pkg install openjdk-17
keytool -genkeypair -v \
  -keystore tmvpn-release.keystore \
  -alias tmvpn \
  -keyalg RSA -keysize 2048 \
  -validity 10000

# Şifre belirle, bilgileri doldur (Ad, Şirket, vb. — istediğin gibi)
# Bu keystore dosyasını ASLA kaybetme ve ASLA repoya pushlama!
```

### 5.2. Keystore'u base64'e çevir

```bash
base64 tmvpn-release.keystore > keystore.base64.txt
cat keystore.base64.txt
```

Çıkan uzun metni kopyala.

### 5.3. GitHub Secrets ekle

https://github.com/Seyit474/TmVpn/settings/secrets/actions → **New secret**

4 tane secret ekle:

| Name | Value |
|------|-------|
| `KEYSTORE_BASE64` | (base64 metni) |
| `KEYSTORE_PASSWORD` | (keystore oluştururken verdiğin şifre) |
| `KEY_ALIAS` | `tmvpn` |
| `KEY_PASSWORD` | (key şifresi, çoğu zaman keystore şifresiyle aynı) |

### 5.4. Tag at, release tetikle

```bash
git tag v0.1.0
git push origin v0.1.0
```

GitHub Actions otomatik:
1. Release APK'yı derler
2. Keystore ile imzalar
3. **Releases** sekmesine yükler

Şuradan inebilirsin:
https://github.com/Seyit474/TmVpn/releases

---

## 6. Sonraki güncellemeler

Kodda her değişiklikten sonra:

```bash
git add .
git commit -m "ne değiştirdiğini yaz"
git push
```

→ Otomatik debug APK üretilir.

Yeni sürüm çıkarmak için:
```bash
git tag v0.2.0
git push origin v0.2.0
```

→ Otomatik release + GitHub Release sayfası.

---

## Sorun giderme

**Workflow başarısız oldu, ne yapayım?**

Actions sekmesinde kırmızı workflow'a tıkla → "build" job'a tıkla →
hangi adımda hata olduğunu gör. Genelde:
- `KEYSTORE_BASE64` secret eksikse release derlemesi unsigned çıkar (sorun değil)
- Gradle sürümü uyuşmazlığı → workflow `gradle wrapper --gradle-version 8.7` ile düzeltir

**Termux'tan git push çalışmıyor:**
```bash
pkg install git gh
gh auth login   # GitHub hesabınla giriş
```

**APK telefonda "App not installed" hatası veriyor:**
- Önceki sürümü kaldır (debug ve release applicationId farklı, debug'da `.debug` eki var, beraber yüklenebilir)
- Android 7+ için signed APK gerekli, debug imzasıyla denersin
