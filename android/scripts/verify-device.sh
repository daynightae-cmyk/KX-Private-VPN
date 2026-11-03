#!/usr/bin/env bash
# Preserve actual device evidence even when a test fails; propagate the Gradle result.
set -u
bash ./gradlew :app:connectedDebugAndroidTest
test_result=$?
mkdir -p device-screenshots
adb pull /sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence ./device-screenshots/
capture_result=$?
adb logcat -d -s AndroidRuntime > device-screenshots/android-runtime.txt
if [ "$test_result" -ne 0 ]; then exit "$test_result"; fi
exit "$capture_result"
