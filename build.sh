#!/bin/bash
set -e

# ============================================================
# Jaiho91 MOD APK Builder
# Usage: ./build.sh input.apk "com.new.package" "App Name" [icon.png]
# ============================================================

APK="$1"
NEW_PACKAGE="$2"
NEW_NAME="$3"
ICON="${4:-icon.png}"
WORKDIR="build_$(date +%s)"
TOOLS="$PWD/tools"
APKTOOL_JAR="$TOOLS/apktool_2.9.3.jar"
APKTOOL="java -jar $APKTOOL_JAR"
SIGNER="$TOOLS/uber-apk-signer.jar"

echo "=== Jaiho91 MOD Builder ==="
echo "Input: $APK"
echo "Package: $NEW_PACKAGE"
echo "Name: $NEW_NAME"
echo ""

# Check inputs
[ -z "$APK" ] && echo "Usage: $0 input.apk new.package.name \"App Name\"" && exit 1
[ ! -f "$APK" ] && echo "APK not found: $APK" && exit 1

# Setup tools
mkdir -p "$TOOLS"
if [ ! -f "$APKTOOL_JAR" ]; then
    echo "[*] Downloading apktool..."
    curl -sL -o "$APKTOOL_JAR" "https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar"
fi
if [ ! -f "$SIGNER" ]; then
    echo "[*] Downloading uber-apk-signer..."
    curl -sL -o "$SIGNER" "https://github.com/patrickfav/uber-apk-signer/releases/download/v1.3.0/uber-apk-signer-1.3.0.jar"
fi

OLD_PACKAGE="com.jaiho.no.games"
OLD_NAME="Jaiho91"

# Step 1: Decompile
echo ""
echo "[1/6] Decompiling APK..."
mkdir -p "$WORKDIR"
$APKTOOL d "$APK" -o "$WORKDIR/decompiled" -f

# Step 2: Rename package
echo "[2/6] Renaming package: $OLD_PACKAGE -> $NEW_PACKAGE"
cd "$WORKDIR/decompiled"

# Rename in AndroidManifest.xml
sed -i "s/package=\"$OLD_PACKAGE\"/package=\"$NEW_PACKAGE\"/g" AndroidManifest.xml

# Rename in all smali files
OLD_PATH="${OLD_PACKAGE//./\/}"
NEW_PATH="${NEW_PACKAGE//./\/}"

# Rename directory structure
find smali* -type d -path "*$OLD_PATH*" 2>/dev/null | while read d; do
    new_d="${d//$OLD_PATH/$NEW_PATH}"
    mkdir -p "$(dirname "$new_d")"
    mv "$d" "$new_d" 2>/dev/null || true
done

# Rename references in all smali files
find smali* -name "*.smali" 2>/dev/null | while read f; do
    sed -i "s|$OLD_PACKAGE|$NEW_PACKAGE|g" "$f"
done

# Rename in Android resources
sed -i "s/$OLD_PACKAGE/$NEW_PACKAGE/g" AndroidManifest.xml

# Fix app name in strings
find res -name "*.xml" 2>/dev/null | while read f; do
    sed -i "s/$OLD_NAME/$NEW_NAME/g" "$f" 2>/dev/null || true
done

cd "$OLDPWD"

# Step 3: Replace launcher icon
echo "[3/6] Replacing launcher icon..."
ICON_DIR="$WORKDIR/decompiled/res"
if [ -f "$ICON" ]; then
    # Find all launcher icon pngs and replace
    find "$ICON_DIR" -name "icon.png" -o -name "ic_launcher.png" | while read f; do
        cp "$ICON" "$f" 2>/dev/null || true
    done
    find "$ICON_DIR" -name "ic_launcher_round.png" | while read f; do
        cp "$ICON" "$f" 2>/dev/null || true
    done
    echo "   Icon replaced"
else
    echo "   No icon file provided, using default"
fi

# Step 4: Inject Jazpays Payment Activity
echo "[4/6] Injecting Jazpays payment gateway..."
PAYOUT_DIR="$WORKDIR/decompiled/smali_classes2/com/jazpays/payment"
mkdir -p "$PAYOUT_DIR"

# Create PaymentActivity.smali
cat > "$PAYOUT_DIR/PaymentActivity.smali" << 'SMALIEOF'
.class public Lcom/jazpays/payment/PaymentActivity;
.super Landroid/app/Activity;
.source "PaymentActivity.java"

# instance fields
.field private merchantId:Ljava/lang/String;
.field private apiKey:Ljava/lang/String;
.field private apiUrl:Ljava/lang/String;

# direct methods
.method public constructor <init>()V
    .locals 1
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V
    const-string v0, "100222099"
    iput-object v0, p0, Lcom/jazpays/payment/PaymentActivity;->merchantId:Ljava/lang/String;
    const-string v0, "25aa23a6200008a506628fa5f971fc1d"
    iput-object v0, p0, Lcom/jazpays/payment/PaymentActivity;->apiKey:Ljava/lang/String;
    const-string v0, "https://api.jazpays.com/v1/create"
    iput-object v0, p0, Lcom/jazpays/payment/PaymentActivity;->apiUrl:Ljava/lang/String;
    return-void
.end method

.method public onCreate(Landroid/os/Bundle;)V
    .locals 0
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V
    .line 1
    invoke-virtual {p0}, Lcom/jazpays/payment/PaymentActivity;->loadPaymentWebView()V
    return-void
.end method

.method public loadPaymentWebView()V
    .locals 4
    .line 10
    new-instance v0, Landroid/webkit/WebView;
    invoke-direct {v0, p0}, Landroid/webkit/WebView;-><init>(Landroid/content/Context;)V
    .line 11
    invoke-virtual {p0, v0}, Lcom/jazpays/payment/PaymentActivity;->setContentView(Landroid/view/View;)V
    .line 12
    invoke-virtual {v0}, Landroid/webkit/WebView;->getSettings()Landroid/webkit/WebSettings;
    move-result-object v1
    const/4 v2, 0x1
    invoke-virtual {v1, v2}, Landroid/webkit/WebSettings;->setJavaScriptEnabled(Z)V
    .line 13
    iget-object v1, p0, Lcom/jazpays/payment/PaymentActivity;->apiUrl:Ljava/lang/String;
    invoke-virtual {v0, v1}, Landroid/webkit/WebView;->loadUrl(Ljava/lang/String;)V
    return-void
.end method
SMALIEOF

# Add activity to AndroidManifest
cd "$WORKDIR/decompiled"
ACTIVITY_LINE='<activity android:name="com.jazpays.payment.PaymentActivity" android:configChanges="keyboardHidden|orientation|screenSize" android:screenOrientation="portrait" \/>'
sed -i "/<application/i\    $ACTIVITY_LINE" AndroidManifest.xml
cd "$OLDPWD"
echo "   Payment activity injected"

# Step 5: Rebuild
echo "[5/6] Rebuilding APK..."
$APKTOOL b "$WORKDIR/decompiled" -o "$WORKDIR/mod_unsigned.apk"

# Step 6: Sign
echo "[6/6] Signing APK..."
if [ -f "keystore.jks" ]; then
    java -jar "$SIGNER" --apks "$WORKDIR/mod_unsigned.apk" \
        --ks keystore.jks \
        --ksAlias "$KS_ALIAS" \
        --ksPass "$KS_PASS" \
        --keyPass "$KEY_PASS"
else
    # Generate debug key
    keytool -genkey -v -keystore "$WORKDIR/debug.keystore" \
        -alias debug -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Debug, OU=Debug, O=Debug, L=Debug, ST=Debug, C=US"
    java -jar "$SIGNER" --apks "$WORKDIR/mod_unsigned.apk" \
        --ks "$WORKDIR/debug.keystore" \
        --ksAlias debug \
        --ksPass android \
        --keyPass android
fi

# Rename output
mv "$WORKDIR/mod_unsigned-aligned-signed.apk" "$WORKDIR/${NEW_NAME// /_}_MOD.apk" 2>/dev/null || \
mv "$WORKDIR/mod_unsigned.apk" "$WORKDIR/${NEW_NAME// /_}_MOD.apk" 2>/dev/null || true

echo ""
echo "========================================"
echo "MOD APK Ready!"
echo "Output: $WORKDIR/${NEW_NAME// /_}_MOD.apk"
echo "========================================"
