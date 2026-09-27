SHIFT CALENDAR — ANDROID HARDWARE-TEST PREVIEW
Version: 0.1.0-preview01

This is the customized scheduling APK, not the original Fossify Calendar.
The APK is Android debug-signed for testing. It is not a production release.
Do not install an unsigned APK or the separate androidTest APK.

INSTALL
1. Download the Shift-Calendar-preview-APK artifact from the successful Offline Calendar APK workflow in antonioavila-bit/Calendar.
2. Extract the ZIP on your Windows PC. Use Shift-Calendar-0.1.0-preview01.apk.
3. Transfer the APK by USB to the Galaxy S25 or Tab S7. The Android device does not need Internet.
4. Open the APK in My Files and allow installation from that source when Android prompts. Turn that allowance off again afterwards.
5. Open Shift Calendar using the navy seahorse/calendar/clock icon in the app drawer. Add it to the home screen by long-pressing, or use Settings > Add home-screen shortcut and approve the launcher prompt.

FIRST USE
1. Add Personnel and Posts. Phone numbers are optional unless sending SMS.
2. Create Required shifts, including date range, weekdays, post, shift time and number of personnel required. This is what enables gap detection.
3. Assign personnel on a shift, or enter successive four-line blocks under Enter schedules: name / dates / time / post. Names and posts must match saved records. Choose the year and review before saving. The optional create-missing-requirements box is off by default.
4. Use Schedule and Uncovered to view assignments and open positions. Shift dates refer to START dates. Overnight end dates are explicit.
5. Use Share / TXT for a date range, person, post and grouping. Save .txt for Windows, Copy for Word, manual SMS, prepared individual SMS, or Share. Direct SMS requires Settings opt-in, a capable phone/SIM, permission, and a final review before sending. It is not a background schedule-change broadcast.
6. Settings offers encrypted backup/restore. Keep the password safe; it cannot be recovered.

OFFLINE TABLET AND HANDWRITING
All scheduling and file generation are local. Wi-Fi-only tablets cannot send carrier SMS. Export/copy the text and transfer it to a messaging-capable phone.
Handwriting uses the installed keyboard/input method. On the Tab S7, choose Samsung Keyboard handwriting with your S Pen; on supported newer Android devices, write into a native text field. No model is downloaded by this APK. Verify the installed keyboard's offline capability with Internet disabled. Keyboard entry remains available.

TEST STATUS
CI build, actual APK checks, and emulator tests are recorded separately in the workflow. A completed build job alone does not mean device tests passed.
Galaxy S25 and Tab S7 physical-device acceptance is still required. Test portrait/landscape, keyboard open/hidden, stylus input, saved schedule persistence, the home-screen icon, TXT export to Windows, backups and restore, reminders, and SMS on a capable phone. Never test SMS against unintended recipients.
This preview uses a dedicated shift workspace. The inherited personal calendar, widgets, ICS import/export and personal-event sync are not exposed in this preview. Required-shift patterns are generated for finite date ranges; reuse/copy them to extend schedules.

BACKUP BEFORE UPDATING
Preview builds may have different debug signing certificates; Android can reject an in-place update. Make an encrypted backup first. Do not uninstall an existing data-bearing app unless that backup is safe and restorable. Stable production signing is a separate release gate.

SOURCE AND CHECKSUMS
The artifact includes the exact source ZIP, commit.txt, SHA256SUMS.txt, APK identity/permissions/signature reports and this guide. The source retains GPLv3 licensing and third-party notices.
