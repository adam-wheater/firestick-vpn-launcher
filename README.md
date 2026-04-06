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

---

## Step 1 — Download the APK onto your Fire Stick

The easiest way to install VPN Launcher is using the free **Downloader** app directly on your Fire Stick. No computer needed for this step.

### 1a. Install Downloader from the Amazon Appstore

1. On your Fire Stick, go to the **Home screen**
2. Open the **Appstore** (search icon at the top)
3. Search for **Downloader** (by AFTVnews)
4. Install it — it's free

### 1b. Allow apps from unknown sources

Fire OS blocks third-party APKs by default. You need to allow Downloader to install them:

1. Go to **Settings** (gear icon)
2. Select **My Fire TV**
3. Select **Developer Options**
   - If you don't see Developer Options, go back to **My Fire TV**, click **About**, then rapidly click the **Fire TV Stick** entry 7 times until a message says "Developer options enabled"
4. Select **Install unknown apps**
5. Find **Downloader** in the list and turn it **ON**

### 1c. Download the APK

1. Open **Downloader**
2. Select the URL bar and enter the code:
   ```
   6001144
   ```
   Or enter the full URL:
   ```
   https://github.com/adam-wheater/firestick-vpn-launcher/releases/latest/download/vpn-launcher.apk
   ```
3. Press **Go** (or the centre button on your remote)
4. The APK will download automatically — this takes a few seconds
5. When prompted, select **Install**
6. Once installed, select **Done** (not "Open" — you'll set it up properly in the next step)

---

## Step 2 — Open VPN Launcher for the first time

1. Press the **Home** button on your remote
2. Fire OS may ask which app to use as your home screen — if it does, select **VPN Launcher** and choose **Always**
3. If it doesn't ask, find VPN Launcher in your apps list and open it from there

> **Note:** VPN Launcher works fully without being set as your default home screen. You can use it as a regular app and still get VPN blocking. Setting it as the home screen just means pressing Home opens it automatically. To do that you'll need ADB — see [Step 4](#step-4--set-vpn-launcher-as-your-home-screen-requires-adb).

---

## Step 3 — Using the app

### The header bar

At the very top of the screen you'll see:

| Element | What it does |
|---|---|
| **Date & time** (top left) | Live clock |
| **VPN status** (top right) | Shows "VPN Connected" (green) or "VPN Disconnected" (red) based on whether a VPN is active on your device |

### The quick-access bar

Just below the header is a row of buttons:

| Button | What it does |
|---|---|
| **Settings** | Opens Fire OS system settings |
| **Appstore** | Opens the Amazon Appstore |
| **VPN** | Opens your VPN app (if you have one installed). If you have multiple VPN apps, it shows a list to choose from |
| **Router VPN** | Toggles Router VPN mode on/off (see [Router VPN mode](#router-vpn-mode) below) |

### The app grid

Your installed apps appear as a 4-column grid below the quick-access bar. The last tile in the grid is always the **Show Hidden / Hide Hidden** tile (see [Hiding apps](#hiding-apps) below).

**Navigating:** Use the D-pad on your remote to move between apps. Press the **centre button** (OK) to launch an app.

### Setting an app to require VPN (the shield icon)

The **Menu button** on your Fire TV remote (the three-line hamburger button) cycles an app through three states:

```
Normal  →  VPN Required (🛡 shield)  →  Hidden (eye-off)  →  Normal  →  ...
```

1. Navigate to any app in the grid
2. Press the **Menu button** once — a **shield icon** appears on the app. This app now requires VPN to launch
3. Press **Menu** again — the shield disappears and a **hidden icon** appears. The app is now hidden from the grid
4. Press **Menu** again — the app returns to normal (visible, no VPN requirement)

**Recommended apps to mark as VPN Required:**

The following apps should always be set to require VPN. Navigate to each one and press **Menu** once to add the shield:

- **Sky Sports**
- **Kodi**
- **Stremio**

**What happens when you try to open a VPN-required app without a VPN connected:**

- A dialog appears explaining that VPN is not connected
- It shows a button to open your VPN app (auto-detected from your installed apps)
- Connect your VPN, then come back — the app will launch automatically

### Reordering apps

You can drag apps into any order:

1. Navigate to the app you want to move
2. **Long-press the centre button** (hold it down for about a second) — the app lifts slightly and a hint appears at the bottom of the screen
3. Use the **D-pad** to move the app left, right, up, or down
4. Press the **centre button** again to drop it in place (or press Back to cancel)

The order is saved automatically and persists after reboot.

### Hiding apps

You can hide apps you don't want cluttering the grid (e.g. system apps, things you never use).

**To hide an app:**

1. Navigate to the app
2. Press **Menu** twice (once for VPN shield, twice for hidden)
3. The app disappears from the grid immediately

**To un-hide an app:**

Hidden apps don't show in the main grid — you need to reveal them first:

1. Scroll to the **end of the app grid** and select the **"Show Hidden"** tile
2. All hidden apps reappear in the grid, dimmed with a small eye-off icon
3. Navigate to the app you want to un-hide
4. Press **Menu** — it cycles from hidden back to normal
5. The app is now visible and normal again
6. Select the **"Hide Hidden"** tile (same position, label changed) to go back to the clean view

> **Important:** If you mark an app as hidden and it disappears, you **must** use the "Show Hidden" tile to bring it back before you can change its state. It won't appear anywhere else.

### Router VPN mode

If your VPN runs on your **router** rather than on the Fire Stick itself (common with home setups using pfSense, OPNsense, or a VPN router), use this mode:

1. Press the **Router VPN** button in the quick-access bar
2. The button turns green and the header shows **"VPN Connected (Router)"**
3. All apps marked as VPN-required will now launch freely, since your router handles the VPN
4. Press **Router VPN** again to turn it off

> Router VPN mode is a trust toggle — it takes your word that the VPN is active at the network level. It does not check your IP automatically on a schedule.

---

## Step 4 — Set VPN Launcher as your home screen (requires ADB)

Fire OS does not allow apps to set themselves as the default home screen without a one-time ADB command from a computer. This is the same requirement as Wolf Launcher, Projectivy, and every other third-party Fire TV launcher.

**You only need to do this once.** After that, pressing Home always opens VPN Launcher, and it starts automatically on boot.

If you don't want to do this, skip it — VPN Launcher still works perfectly as a regular app.

---

### Step 4a — Find your Fire Stick's IP address

1. On your Fire Stick, go to **Settings**
2. Select **My Fire TV**
3. Select **About**
4. Select **Network** — note the **IP address** (e.g. `192.168.1.45`)

---

### Step 4b — Enable ADB debugging on the Fire Stick

1. Go to **Settings > My Fire TV > Developer Options**
2. Turn **ADB debugging** to **ON**
3. Turn **Apps from unknown sources** to **ON** if not already done

---

### Step 4c — Install ADB on your computer

ADB (Android Debug Bridge) is a free tool from Google. Pick your OS:

#### Windows

1. Download the [Android Platform Tools ZIP](https://developer.android.com/tools/releases/platform-tools) from Google
2. Extract the ZIP to a folder, e.g. `C:\platform-tools`
3. Open **Command Prompt**: press `Win + R`, type `cmd`, press Enter
4. Navigate to the folder:
   ```
   cd C:\platform-tools
   ```
5. All `adb` commands below should be run from this Command Prompt window

#### macOS

Open **Terminal** (press `Cmd + Space`, type "Terminal") and run:

```bash
brew install android-platform-tools
```

If you don't have Homebrew, install it first from [brew.sh](https://brew.sh), or download the Platform Tools ZIP from Google and extract it.

#### Linux (Ubuntu/Debian)

Open a terminal and run:

```bash
sudo apt update && sudo apt install adb
```

---

### Step 4d — Connect ADB to your Fire Stick

Your computer and Fire Stick must be on the **same WiFi network**.

In your terminal / command prompt, run:

```bash
adb connect <your-fire-stick-ip>:5555
```

Replace `<your-fire-stick-ip>` with the IP you noted in Step 4a. For example:

```bash
adb connect 192.168.1.45:5555
```

You should see:
```
connected to 192.168.1.45:5555
```

> **Fire Stick prompts you?** A dialog may appear on your TV asking "Allow ADB debugging?" — select **Always allow from this computer** and press OK.

If you see `failed to connect` or `connection refused`:
- Double-check the IP address
- Make sure ADB debugging is ON (Step 4b)
- Make sure both devices are on the same WiFi network
- Try turning ADB debugging off and back on

---

### Step 4e — Grant the permission

Run this command (copy it exactly):

```bash
adb shell pm grant com.vpnlauncher android.permission.WRITE_SECURE_SETTINGS
```

You won't see any output if it works — that's normal. If you see an error, check that VPN Launcher is actually installed by running `adb shell pm list packages | grep vpnlauncher`.

---

### Step 4f — Activate in the app

1. Open **VPN Launcher** on your Fire Stick
2. A **yellow banner** at the top says "Tap to set as home screen" — navigate to it and press the centre button
3. A confirmation dialog appears — press **OK**
4. Done. Press **Home** — VPN Launcher opens

> If the yellow banner is gone, press the **Home** button. If VPN Launcher doesn't open, open VPN Launcher manually, navigate to the banner, and try again. If you dismissed the banner by accident, **long-press the clock** (top-left) and select **"Restore 'Set as Home' banner"**.

---

## Updating

Re-download using the same Downloader code (`6001144`) or URL (`https://github.com/adam-wheater/firestick-vpn-launcher/releases/latest/download/vpn-launcher.apk`) and install over the top — your settings and app order are preserved.

Via ADB:

```bash
adb install -r vpn-launcher.apk
```

---

## Reverting to the Fire TV home screen

To switch back to the default Fire TV launcher:

1. Go to **Settings > Applications > Manage Installed Applications**
2. Select **VPN Launcher**
3. Select **Clear defaults**
4. Press **Home** and select the Fire TV launcher

---

## Build from Source

Requires Android SDK with API 33.

```bash
git clone https://github.com/adam-wheater/firestick-vpn-launcher.git
cd firestick-vpn-launcher
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

---

## Compatibility

- Fire TV Stick (3rd gen and newer)
- Fire TV Stick 4K / 4K Max
- Fire TV Cube
- Any FireOS 7+ device (Android-based)
- **Not compatible** with Vega OS devices (Fire TV Stick 4K Select)

---

## License

MIT
