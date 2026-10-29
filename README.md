# Kate for Android
Version 0.2: onboarding, new home screen, per-app filter, flat front-on image.

Open this folder in Android Studio (Ladybug or newer), let Gradle sync, run on a phone (API 26+).

First run: Setup tile -> enter the PC's IP -> grant mic/notification prompts -> Notifications tile -> allow Kate access
-> Setup -> "Let Kate run in the background" (needed on Xiaomi/MIUI too: set Autostart on and battery to "No restrictions").

Server: paste server/notify_patch.py into ESP/on-pc/server.py (adapt the queue name).
NFC tag (work in progress): write one NDEF MIME record, type application/vnd.kate.link,
payload `kate;bt=AA:BB:CC:DD:EE:FF;host=192.168.1.50`. Tapping it opens Kate and links over Bluetooth.
ESP32 side: server/esp32_bt_snippet.ino.

## Get the APK without Android Studio
Push this folder to a GitHub repo. Actions tab -> "Build APK" -> latest run -> download "Kate-debug-apk" (a zip containing app-debug.apk). Sideload it on the phone.

Fonts: Poppins (SIL Open Font License 1.1), app/src/main/res/font.
