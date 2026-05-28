#!/data/data/com.termux/files/usr/bin/bash
# TmVpn — Termux otomatik GitHub kurulum scripti
# Kullanım: bash termux-setup.sh

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}=== TmVpn GitHub Kurulumu ===${NC}"
echo ""

# 1. Gerekli paketler
echo -e "${YELLOW}[1/6] Paketler kontrol ediliyor...${NC}"
for pkg in git gh nano; do
    if ! command -v $pkg &> /dev/null; then
        echo "Kuruluyor: $pkg"
        pkg install -y $pkg
    fi
done

# 2. GitHub girişi
echo ""
echo -e "${YELLOW}[2/6] GitHub girişi kontrol ediliyor...${NC}"
if ! gh auth status &> /dev/null; then
    echo "GitHub'a giriş yapman lazım. Tarayıcı açılacak."
    echo "Bittiğinde Enter'a bas."
    gh auth login -h github.com -p https -w
fi
echo -e "${GREEN}✅ GitHub girişi tamam${NC}"

# 3. SUBSCRIPTION_URL sor
echo ""
echo -e "${YELLOW}[3/6] Subscription URL${NC}"
echo "Marzban-docs-sync sisteminden gelen Google Docs URL'ini yapıştır."
echo "Örnek: https://doc.google.com/document/d/ABC123XYZ/export?format=txt"
echo ""
read -p "URL: " SUB_URL

if [ -z "$SUB_URL" ]; then
    echo -e "${RED}URL boş, çıkılıyor.${NC}"
    exit 1
fi

# build.gradle.kts içindeki placeholder'ı değiştir
sed -i "s|https://doc.google.com/document/d/PLACEHOLDER/export?format=txt|${SUB_URL}|g" app/build.gradle.kts
echo -e "${GREEN}✅ URL ayarlandı${NC}"

# 4. Git init
echo ""
echo -e "${YELLOW}[4/6] Git deposu hazırlanıyor...${NC}"
if [ ! -d .git ]; then
    git init -q
    git branch -M main
fi

# Git kullanıcı bilgisi
if [ -z "$(git config user.email)" ]; then
    GH_USER=$(gh api user --jq .login)
    GH_EMAIL=$(gh api user --jq '.email // empty')
    git config user.name "$GH_USER"
    git config user.email "${GH_EMAIL:-${GH_USER}@users.noreply.github.com}"
fi

git add .
git commit -q -m "Aşama 1: iskelet + parser + ping + GitHub Actions" || echo "Zaten commitli"
echo -e "${GREEN}✅ Lokal commit hazır${NC}"

# 5. GitHub repo oluştur veya bağla
echo ""
echo -e "${YELLOW}[5/6] GitHub deposu...${NC}"
GH_USER=$(gh api user --jq .login)

if gh repo view "${GH_USER}/TmVpn" &> /dev/null; then
    echo "Repo zaten var: ${GH_USER}/TmVpn"
    if ! git remote | grep -q origin; then
        git remote add origin "https://github.com/${GH_USER}/TmVpn.git"
    fi
else
    echo "Yeni private repo oluşturuluyor: ${GH_USER}/TmVpn"
    gh repo create TmVpn --private --source=. --remote=origin
fi
echo -e "${GREEN}✅ Repo hazır${NC}"

# 6. Push
echo ""
echo -e "${YELLOW}[6/6] Push ediliyor...${NC}"
git push -u origin main
echo -e "${GREEN}✅ Push tamam!${NC}"

# Son bilgi
echo ""
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo -e "${GREEN}🎉 Hazır! GitHub Actions otomatik derliyor.${NC}"
echo -e "${GREEN}═══════════════════════════════════════${NC}"
echo ""
echo "📍 İzlemek için:"
echo "   https://github.com/${GH_USER}/TmVpn/actions"
echo ""
echo "⏱  5-8 dakika sonra APK hazır olur."
echo ""
echo "📦 APK indirmek için (terminalden):"
echo "   gh run list --limit 1"
echo "   gh run download <RUN_ID>"
echo ""
echo "💡 Veya tarayıcıda Actions → workflow → Artifacts kısmından."
echo ""
