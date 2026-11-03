#!/usr/bin/env bash
# Preserve actual device evidence even when a test fails; propagate the Gradle result.
set -u
adb_cmd() { timeout --signal=TERM --kill-after=3s 20s adb "$@"; }
mkdir -p device-screenshots
gradle_flags=(--no-daemon --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process')
free -m > device-screenshots/host-memory-before.txt
# A fresh CI emulator provides the true first launch, before tests mark the intro seen.
timeout --kill-after=15s 6m bash ./gradlew "${gradle_flags[@]}" :app:installDebug
install_result=$?
if [ "$install_result" -ne 0 ]; then exit "$install_result"; fi
adb_cmd shell wm density 320
adb_cmd shell dumpsys gfxinfo com.knoux.kxprivatevpn reset > /dev/null
# Some software renderers can stall the recorder after the last animated frame.
# Bound the host command as well as Android's requested duration.
timeout --signal=INT --kill-after=3s 12s adb shell screenrecord --time-limit 6 /sdcard/kx-launch.mp4 &
recorder=$!
adb_cmd shell am start -W -n com.knoux.kxprivatevpn/.MainActivity > device-screenshots/cold-start.txt
sleep 6
wait "$recorder" || true
adb_cmd pull /sdcard/kx-launch.mp4 device-screenshots/kx-launch.mp4 || true
adb_cmd shell dumpsys gfxinfo com.knoux.kxprivatevpn framestats > device-screenshots/launch-frames.txt
adb_cmd exec-out screencap -p > device-screenshots/launcher-welcome.png
adb_cmd shell am force-stop com.knoux.kxprivatevpn
adb_cmd shell am start -W -n com.knoux.kxprivatevpn/.MainActivity > device-screenshots/repeat-start.txt
adb_cmd shell wm size > device-screenshots/display-360.txt
adb_cmd shell wm density >> device-screenshots/display-360.txt
# 720px / (280dpi / 160) = 411.4dp; preserve real display evidence.
adb_cmd shell wm density 280
adb_cmd shell wm size > device-screenshots/display-411.txt
adb_cmd shell wm density >> device-screenshots/display-411.txt
timeout --kill-after=15s 8m bash ./gradlew "${gradle_flags[@]}" :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
test_result=$?
free -m > device-screenshots/host-memory-after.txt
sudo dmesg > device-screenshots/host-kernel.txt 2>&1 || true
adb_cmd pull /sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence ./device-screenshots/
capture_result=$?
adb_cmd logcat -d > device-screenshots/logcat.txt
adb_cmd shell dumpsys activity > device-screenshots/activity.txt
if [ "$test_result" -ne 0 ]; then exit "$test_result"; fi
exit "$capture_result"
