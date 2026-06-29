# Jaiho91 MOD APK Builder

Custom branded casino app with your own payment gateway (Jazpays).

## Features
- ✅ Custom package name + app name
- ✅ Custom launcher icon
- ✅ Jazpays payment gateway integration
- ✅ Original games intact (same server)
- ✅ Auto-build via GitHub Actions

## How To Use

### 1. Fork This Repo
Click "Fork" on GitHub.

### 2. Set Your Secrets
Go to repo → Settings → Secrets → Actions, add:

| Secret | Value |
|--------|-------|
| `NEW_PACKAGE` | `com.yourbrand.casino` |
| `NEW_APP_NAME` | `Your Casino Name` |
| `KEYSTORE_B64` | base64 of your keystore file |
| `KEYSTORE_PASS` | your keystore password |
| `KEY_ALIAS` | your key alias |
| `KEY_PASS` | your key password |

### 3. Upload APK
Go to Actions → Run workflow → Upload original `jaiho91_prosafebet.apk`

### 4. Download MOD APK
After build completes → Download from Artifacts.

## Payment Gateway (Jazpays)

| Parameter | Value |
|-----------|-------|
| Merchant ID | 100222099 |
| API Key | 25aa23a6200008a506628fa5f971fc1d |
| API Endpoint | `https://api.jazpays.com/v1/create` |
| Signature | MD5( sorted params + "&key=API_KEY" ) |

### Payment Flow
1. User taps "Buy Coins" in game
2. PaymentActivity opens with amount selector
3. App creates order via Jazpays API
4. User completes payment in WebView
5. Callback updates user balance
6. Success → game resumes

## Manual Build (Local PC)

```bash
# Requirements: Java 8+, apktool, zipalign, apksigner
chmod +x build.sh
./build.sh jaiho91_prosafebet.apk "com.yourbrand.casino" "Your Casino"
```
