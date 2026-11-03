#!/usr/bin/env bash
# Preserve actual device evidence even when a test fails; propagate the Gradle result.
set -u
mkdir -p device-screenshots
# A fresh CI emulator provides the true first launch, before tests mark the intro seen.
bash ./gradlew :app:installDebug
install_result=$?
if [ "$install_result" -ne 0 ]; then exit "$install_result"; fi
adb shell dumpsys gfxinfo com.knoux.kxprivatevpn reset > /dev/null
adb shell screenrecord --time-limit 6 /sdcard/kx-launch.mp4 &
recorder=$!
adb shell am start -W -n com.knoux.kxprivatevpn/.MainActivity > device-screenshots/cold-start.txt
sleep 6
wait "$recorder" || true
adb pull /sdcard/kx-launch.mp4 device-screenshots/kx-launch.mp4 || true
adb shell dumpsys gfxinfo com.knoux.kxprivatevpn framestats > device-screenshots/launch-frames.txt
adb exec-out screencap -p > device-screenshots/launcher-welcome.png
adb shell am force-stop com.knoux.kxprivatevpn
adb shell am start -W -n com.knoux.kxprivatevpn/.MainActivity > device-screenshots/repeat-start.txt
bash ./gradlew :app:connectedDebugAndroidTest
test_result=$?
adb pull /sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence ./device-screenshots/
capture_result=$?
adb logcat -d -s AndroidRuntime > device-screenshots/android-runtime.txt
if [ "$test_result" -ne 0 ]; then exit "$test_result"; fi
exit "$capture_result"
