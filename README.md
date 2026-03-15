# VPN Launcher for Fire TV

A Fire TV Stick app that lists your installed apps and lets you configure which ones require a VPN connection. If you try to launch a VPN-required app without VPN connected, it blocks the launch and prompts you to open NordVPN.

## Features

- Lists all installed apps in a single scrollable view
- Toggle any app to require VPN before launching
- Real-time VPN status indicator (connected/disconnected)
- Blocks launch of VPN-required apps when VPN is off
- "Open NordVPN" prompt to quickly connect
- Auto-launches your app after you connect VPN
- Fully navigable with the Fire TV remote
- Config stored locally — no account or cloud needed

## Download

**[Download latest APK](https://github.com/adam-wheater/firestick-vpn-launcher/releases/latest/download/vpn-launcher.apk)**

## Install on Fire TV Stick

1. On your Fire Stick: **Settings > My Fire TV > Developer Options > ADB debugging > ON**
2. Note your Fire Stick's IP address: **Settings > My Fire TV > About > Network**
3. From your computer (same network):

   ```bash
   adb connect <fire-stick-ip>:5555
   adb install vpn-launcher.apk
   ```

4. The app appears in **Your Apps & Channels** on the Fire TV home screen

### Update

```bash
adb install -r vpn-launcher.apk
```

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
   - If VPN is on: app launches normally
   - If VPN is off: a dialog appears with "Open NordVPN" or "Cancel"
   - After connecting NordVPN and returning, your app launches automatically

## Compatibility

- Fire TV Stick (3rd gen and newer)
- Fire TV Stick 4K / 4K Max
- Fire TV Cube
- Any FireOS device (Android-based)
- **Not compatible** with Vega OS devices (Fire TV Stick 4K Select)

## License

MIT
