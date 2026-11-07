#!/usr/bin/env bash
# CI emulator gate (android-emulator-runner). Not for physical devices.
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
# API35 hosted software-renderer sometimes drops the entire ADB emulator after
# recording + screencap, BEFORE instrumentation begins. Keep the genuine full
# instrumented suite on API35, but isolate its risky pretest media capture.
sdk="$(adb_cmd shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ ! "$sdk" =~ ^[0-9]+$ ]]; then
    echo "Cannot establish emulator API level" >&2
    exit 2
fi
if (( sdk >= 35 )); then
    # The hosted AVD boots a 720x1280 skin at 480dpi = 240dp wide, below the owner's
    # 360dp design minimum, so below-fold content becomes unreachable and the
    # instrumented suite fails on layout, not behavior. Both API levels must run the
    # tests at the same 411dp reference width (720px / 1.75 = 411.4dp).
    adb_cmd shell wm size 720x1280
    adb_cmd shell wm density 280
    printf '%s\n' "API${sdk}: headless screenrecord/framebuffer capture skipped (renderer instability), NOT visual PASS" > device-screenshots/capture-scope.txt
    adb_cmd shell am start -W -n com.knoux.kxprivatevpn/.MainActivity > device-screenshots/cold-start.txt
    adb_cmd shell wm size > device-screenshots/display-api35.txt
    adb_cmd shell wm density >> device-screenshots/display-api35.txt
else
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
fi
# Never let an incorrectly sized emulator masquerade as a product regression.
display_size="$(adb_cmd shell wm size | tr -d '\r' | awk -F'[:x]' '/:/{gsub(/ /,"",$2); gsub(/ /,"",$3); print $2" "$3}' | tail -n 1)"
display_density="$(adb_cmd shell wm density | tr -d '\r' | awk -F':' '/:/{gsub(/ /,"",$2); print $2}' | tail -n 1)"
read -r display_width display_height <<<"$display_size"
logical_width="$(awk -v px="${display_width:-0}" -v dpi="${display_density:-1}" 'BEGIN { printf "%d", px * 160 / dpi }')"
{
    printf 'width=%s height=%s density=%s logical_width=%sdp\n' \
        "${display_width:-unknown}" "${display_height:-unknown}" "${display_density:-unknown}" "$logical_width"
} | tee device-screenshots/display-contract.txt
if (( logical_width < 360 )); then
    echo "Emulator logical width ${logical_width}dp is below the 360dp design minimum" >&2
    exit 3
fi
# The hosted API35 AVD opens the soft keyboard when a test field takes focus. It covers
# controls below the field and makes coordinate taps race the IME animation; runs
# 42141963135 and 42143462462 failed that way in SplitTunnelScreenTest and ClientFlowTest
# while the same tests passed on API26 and the physical phone. performTextInput drives
# Compose semantics, not the system keyboard, so disable every IME for this run.
# Emulator-only: never mutate input settings on an owner's physical device.
is_emulator=false
if [ "$(adb_cmd shell getprop ro.kernel.qemu | tr -d '\r')" = "1" ]; then is_emulator=true; fi
case "$(adb_cmd shell getprop ro.hardware | tr -d '\r')" in
    goldfish*|ranchu*) is_emulator=true ;;
esac
if [ "$is_emulator" = true ]; then
    adb_cmd shell settings put secure show_ime_with_hard_keyboard 0 || true
    while IFS= read -r ime; do
        ime="${ime%$'\r'}"
        [ -n "$ime" ] || continue
        adb_cmd shell ime disable "$ime" >/dev/null 2>&1 || true
    done < <(adb_cmd shell ime list -s 2>/dev/null || true)
    {
        printf 'ime_disable_applied=true\nenabled_imes_after_disable:\n'
        adb_cmd shell ime list -s 2>&1 || true
        printf 'default_input_method_after_disable: '
        adb_cmd shell settings get secure default_input_method 2>&1 || true
    } > device-screenshots/ime-disabled.txt
else
    printf 'ime_disable_applied=false\nreason=physical_device\n' > device-screenshots/ime-disabled.txt
fi
# The suite is 210 instrumented tests. Run 42414269579 on API35 reached 201/210 with
# 0 failures in 7m22s and was killed by the old 8m bound one second after the evidence
# pull, so the bound no longer covered a healthy hosted run. 12m still bounds a real
# hang without turning it into a passing run.
timeout --kill-after=15s 12m bash ./gradlew "${gradle_flags[@]}" :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true
test_result=$?
free -m > device-screenshots/host-memory-after.txt
sudo dmesg > device-screenshots/host-kernel.txt 2>&1 || true
# A lost emulator produces an empty <failure> element and a truncated test count.
# Record the loss explicitly so it can never be read as a product assertion failure.
if ! adb_cmd get-state > /dev/null 2>&1; then
    {
        printf 'emulator_lost_during_test=true\n'
        printf 'gradle_exit_code=%s\n' "$test_result"
        adb devices -l 2>&1 || true
        for utp in app/build/outputs/androidTest-results/connected/debug/*/utp.0.log; do
            [ -f "$utp" ] || continue
            printf '\n--- %s ---\n' "$utp"
            tail -n 15 "$utp"
        done
    } > device-screenshots/emulator-loss.txt
fi
adb_cmd pull /sdcard/Android/data/com.knoux.kxprivatevpn/files/identity-evidence ./device-screenshots/
capture_result=$?
adb_cmd logcat -d > device-screenshots/logcat.txt
adb_cmd shell dumpsys activity > device-screenshots/activity.txt
if [ "$test_result" -ne 0 ]; then exit "$test_result"; fi
exit "$capture_result"
