#!/bin/bash
# Lokal'de derlemek istersen gradle-wrapper.jar'ı indirir.
# GitHub Actions bu dosyayı kendi üretir, bu script sadece lokal için.

set -e

WRAPPER_DIR="gradle/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/gradle-wrapper.jar"

if [ -f "$WRAPPER_JAR" ]; then
    echo "✅ gradle-wrapper.jar zaten var"
    exit 0
fi

mkdir -p "$WRAPPER_DIR"

echo "📥 gradle-wrapper.jar indiriliyor..."
curl -L -o "$WRAPPER_JAR" \
    "https://github.com/gradle/gradle/raw/v8.7.0/gradle/wrapper/gradle-wrapper.jar"

chmod +x gradlew
echo "✅ Hazır. Artık ./gradlew assembleDebug çalıştırabilirsin."
