# VPN Launcher for Fire TV

A replacement home launcher for Fire TV that lets you control which apps require a VPN. Apps are shown in a grid layout that you can reorder, and any app marked as VPN-required is blocked from launching until you connect your VPN. Works with any VPN app — NordVPN, ExpressVPN, Surfshark, or whatever you have installed.

## Features

- **Grid launcher** — 4-column app grid with icons and labels, replaces the Fire TV home screen
- **VPN gating** — mark any app as VPN-required; it won't launch without an active VPN
- **Any VPN supported** — auto-detects installed VPN apps, no configuration needed
- **App reordering** — long-press to pick up an app, D-pad to move, press OK to drop
- **Router VPN mode** — for VPN at the network level (router/gateway), verified via IP check
- **Real-time VPN status** — header shows connected/disconnected, updates live
- **Quick-access bar** — Settings, Appstore, and VPN buttons always at the top
- **Auto-launch** — after connecting VPN, automatically launches the app you were trying to open
- **Fully D-pad navigable** — built for the Fire TV remote
- **No account needed** — everything stored locally on device

## Download

**[Download latest APK](https://github.com/adam-wheater/firestick-vpn-launcher/releases/latest/download/vpn-launcher.apk)**

### Downloader App

If you have [Downloader](https://www.amazon.com/dp/B01N0BP507) installed on your Fire Stick, enter this URL:

```
tinyurl.com/2dhccdb5
```

## Install

### Via Downloader (directly on Fire Stick)

1. On your Fire Stick: **Settings > My Fire TV > Developer Options > Install unknown apps > Downloader > ON**
2. Open Downloader and enter: `tinyurl.com/2dhccdb5`
3. The APK will download — select **Install** when prompted
4. Press the **Home** button — Fire OS will ask which launcher to use
5. Select **VPN Launcher** and choose **Always**

### Via ADB (from computer)

1. On your Fire Stick: **Settings > My Fire TV > Developer Options > ADB debugging > ON**
2. From your computer (same network):

   ```bash
   adb connect <fire-stick-ip>:5555
   adb install vpn-launcher.apk
   ```

3. Press Home and select VPN Launcher as your default launcher

### Set as Default Home Screen (optional)

Fire OS doesn't allow changing the home launcher without a one-time ADB command. This is the same requirement as Wolf Launcher, Projectivy, and every other custom Fire TV launcher.

From a computer on the same WiFi network:

```bash
adb connect <fire-stick-ip>:5555
adb shell pm grant com.vpnlauncher android.permission.WRITE_SECURE_SETTINGS
```

Then open VPN Launcher and press the yellow setup banner — it will activate automatically. Pressing Home now opens VPN Launcher, and it auto-starts on boot.

**Without this step**, VPN Launcher still works perfectly — just open it from your apps list. VPN blocking works either way.

### Update

Re-download via Downloader using the same URL, or:

```bash
adb install -r vpn-launcher.apk
```

### Revert to Fire TV Home

To switch back to the default Fire TV launcher:

**Settings > Applications > Manage Installed Applications > VPN Launcher > Clear defaults**

Then press Home and select the Fire TV launcher.

## How It Works

1. Press Home — VPN Launcher opens as your home screen
2. Browse your apps in the grid
3. **Menu button** on the remote toggles VPN requirement for the focused app (shield icon appears)
4. **Long-press** an app to reorder it — use D-pad to move, press OK to drop
5. When you select a VPN-required app:
   - VPN is on: app launches normally
   - VPN is off: a dialog appears to open your VPN app (auto-detected)
   - After connecting, your app launches automatically
6. Quick-access bar at the top: **Settings** | **Appstore** | **VPN** | **Router VPN**

### Router VPN Mode

If your VPN runs on your router instead of on the Fire Stick:

1. Press the **Router VPN** button in the quick-access bar to toggle it on
2. The header shows "VPN Connected (Router)"
3. VPN-required apps will launch without needing a device-level VPN

## Build from Source

Requires Android SDK with API 33.

```bash
git clone https://github.com/adam-wheater/firestick-vpn-launcher.git
cd firestick-vpn-launcher
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Compatibility

- Fire TV Stick (3rd gen and newer)
- Fire TV Stick 4K / 4K Max
- Fire TV Cube
- Any FireOS 7+ device (Android-based)
- **Not compatible** with Vega OS devices (Fire TV Stick 4K Select)

## License

MIT
