# Kate for Android (0.4)

## Build
Push this folder to GitHub: Actions -> "Build APK" -> download `Kate-debug-apk`. Uninstall any older Kate first
(each build is signed with a fresh debug key). Or open it in Android Studio and run.

## Server (one-time)
Kate's bridge needs the `/notify` endpoint so the bar can read phone notifications aloud.
From the root of your kate repo:

    git apply path/to/notify.patch      # adds POST /notify to ESP/on-pc/server.py
    # restart the bridge: python ESP\on-pc\server.py

Until then the app shows "Server needs the notify update" on the home screen.

## First run
Onboarding asks for the PC's IP (port 8000 is assumed), then the permissions. Reading notifications is the only
required one. On Xiaomi also set Autostart on and battery to "No restrictions".

## How it talks to Kate
- Talk button: POST /talk with 16 kHz PCM, plays the streamed reply, keeps listening when Kate sends X-Continue.
- Phone notifications: POST /notify, queued by the server like a timer and spoken on the bar's speaker.
- Timers and reminders stay on the bar. Settings -> "Play on this phone" makes this phone long-poll /announce instead
  (the bar and the phone would otherwise compete for each announcement, so keep it off unless the bar is unplugged).
- Reachability is checked with GET /openapi.json, which never consumes an announcement.

## NFC tag (work in progress)
One NDEF MIME record, type `application/vnd.kate.link`, payload `kate;bt=AA:BB:CC:DD:EE:FF;host=192.168.1.50`.
Tapping it opens the app and links over Bluetooth. ESP32 side: server/esp32_bt_snippet.ino (relays to /notify).

Fonts: Poppins (SIL Open Font License 1.1), app/src/main/res/font.
