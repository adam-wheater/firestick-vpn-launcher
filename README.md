# VPN Launcher for Fire TV

A Fire TV Stick app that lists your installed apps and lets you configure which ones require a VPN connection. If you try to launch a VPN-required app without VPN connected, it blocks the launch and prompts you to open NordVPN.

## Features

- Lists all installed apps in a single scrollable view
- Toggle any app to require VPN before launching
- Real-time VPN status indicator (connected/disconnected)
- Blocks launch of VPN-required apps when VPN is off
- "Open NordVPN" prompt to quickly connect
- Auto-launches your app after you connect VPN
- **Router VPN support** — enable "Router VPN Mode" if your router handles VPN, verified via IP check
- Fully navigable with the Fire TV remote
- Config stored locally — no account or cloud needed

## Download

**[Download latest APK](https://github.com/adam-wheater/firestick-vpn-launcher/releases/latest/download/vpn-launcher.apk)**

### Downloader App

If you have [Downloader](https://www.amazon.com/dp/B01N0BP507) installed on your Fire Stick, enter this URL:

```
tinyurl.com/2dhccdb5
```

## Install on Fire TV Stick

### Via ADB (from computer)

1. On your Fire Stick: **Settings > My Fire TV > Developer Options > ADB debugging > ON**
2. Note your Fire Stick's IP address: **Settings > My Fire TV > About > Network**
3. From your computer (same network):

   ```bash
   adb connect <fire-stick-ip>:5555
   adb install vpn-launcher.apk
   ```

4. The app appears in **Your Apps & Channels** on the Fire TV home screen

### Via Downloader App (directly on Fire Stick)

1. Install [Downloader](https://www.amazon.com/dp/B01N0BP507) from the Amazon Appstore
2. Open Downloader and enter: `tinyurl.com/2dhccdb5`
3. The APK will download — select **Install** when prompted
4. The app appears in **Your Apps & Channels**

### Update

```bash
adb install -r vpn-launcher.apk
```

Or re-download via Downloader using the same URL.

## Build from Source

Requires Android SDK with API 33.

```bash
git clone https://github.com/adam-wheater/firestick-vpn-launcher.git
cd firestick-vpn-launcher
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release-unsigned.apk`

## How It Works

1. Open VPN Launcher from your Fire TV apps
2. Scroll through your app list with the D-pad
3. Use the toggle on the right to mark apps that need VPN
4. When you select a VPN-required app:
   - If VPN is on (device or router): app launches normally
   - If VPN is off: a dialog appears with "Open NordVPN" or "Cancel"
   - After connecting NordVPN and returning, your app launches automatically

### Router VPN Mode

If your VPN runs on your router instead of on the Fire Stick:

1. Enable the **Router VPN Mode** toggle at the top of the app
2. The app will verify your VPN by checking your public IP against known VPN/datacenter ranges
3. If verified, the header shows "VPN Connected (Router)"
4. If the IP check can't confirm VPN, the app shows a warning but still trusts your toggle

## Compatibility

- Fire TV Stick (3rd gen and newer)
- Fire TV Stick 4K / 4K Max
- Fire TV Cube
- Any FireOS device (Android-based)
- **Not compatible** with Vega OS devices (Fire TV Stick 4K Select)

## License

MIT
