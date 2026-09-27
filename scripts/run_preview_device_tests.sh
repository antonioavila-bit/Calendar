#!/usr/bin/env bash
set -euo pipefail
API=${1:?API required}
mkdir -p device-evidence
APP=$(find test-apks -type f -name '*.apk' ! -name '*androidTest*' -print -quit)
TEST=$(find test-apks -type f -name '*androidTest*.apk' -print -quit)
test -n "$APP" && test -n "$TEST"
adb install -r "$APP"
adb install -r "$TEST"
adb shell svc wifi disable
adb shell svc data disable
adb shell cmd connectivity airplane-mode enable || { adb shell settings put global airplane_mode_on 1; }
adb shell settings get global airplane_mode_on | tee device-evidence/airplane-mode.txt
if [ "$API" = '33' ]; then adb shell wm size 1600x2560; adb shell wm density 320; fi
adb logcat -c
set +e
adb shell am instrument -w -r -e package org.fossify.calendar.security com.antonioavila.offlinesecuritycalendar.debug.test/androidx.test.runner.AndroidJUnitRunner | tee device-evidence/instrumentation.txt
RESULT=${PIPESTATUS[0]}
set -e
adb logcat -d > device-evidence/logcat.txt
adb pull /sdcard/Android/data/com.antonioavila.offlinesecuritycalendar.debug/files/ device-evidence/screens/ || true
if [ "$RESULT" != '0' ] || ! grep -q 'OK (16 tests)' device-evidence/instrumentation.txt; then
  echo 'Instrumented acceptance failed; APK is not qualified by this job.'; exit 1
fi
