# APK deliverable checkpoint and final navigation adjustment

Application: antonioavila-bit/Calendar; branch security/offline-shifts-v1; draft PR #1. Only this fork and the separately authorized planning repository may be modified. Both upstream FossifyOrg/Calendar and Inventory-App remain read-only donors.

## Verified preview before final navigation polish
Commit 45809fee06c869f2400d2560f81a6a3f8c72cce3, workflow 36291528347 (Offline Calendar APK, run 4), passed:
- Android APK and instrumented-test APK compilation;
- 34 scheduling/SMS unit tests, zero failures/skips;
- Actual APK identity/signature, no Internet/calendar/contact permission, approved icon hash, disabled app-data backup and removal of unused cross-app initializers;
- 12 instrumented tests on API33 tablet-layout AND API35 phone emulators in airplane mode.

Run4 artifact: Shift-Calendar-preview-APK-4, ID 10922168539.
APK SHA256: 2ea6d2a1198d6941d21e555b2e60cfa176b43ddc24036b79871411582e7c950a.
Source ZIP SHA256: a4e528b48b443ac8ed19a2ab70b219470a12712a36ae59899ba948884ac5df61.

## Adjustment in the commit containing this file
Review of actual emulator screenshots found that six permanent navigation buttons and expanded filters pushed the calendar/assignments below the fold on a small phone. The navigation is now behind a clearly labeled Menu button. Calendar month controls use one row, day buttons retain 48dp height, and board filters expand only when requested. Each shift now has a Share this shift action for copy/manual SMS/share or opt-in individual crew SMS. Tests open the menu and assert the destination is shown before clicking. No scheduling-data or parser semantics changed.

This adjustment must pass its OWN workflow before its APK supersedes run4. The final PR checkpoint comment records the accepted commit/run/artifact/hash. Do not substitute an unverified artifact. Version 0.1.0-preview01 is still a first-delivery hardware-test preview, not a production release.

## Using the compact navigation
Tap Menu for Calendar, Schedule, Uncovered, Personnel, Posts, Share / TXT, Settings / backup, and Enter schedules. Tap Filter by person / post on the board to expand filters; active selections remain visible when collapsed. Main calendar date buttons represent shift START dates. Required shifts and Enter schedules are also below the calendar; scroll on a small screen when needed.

## Outstanding physical acceptance
S25 and Tab S7/S Pen, installed Samsung keyboard offline, actual file picker/USB/Word printing, SMS/default SIM/carrier handling, reminders/reboot/permission denial, large-font and large-roster performance, and stable protected production signing. Emulator passes do not prove these. Do not uninstall a data-bearing preview without a restorable backup; CI debug signing may change between runs.

See CONTINUATION.md for architecture and preview limits, INSTALL_PREVIEW.md for installation, and THIRD_PARTY_NOTICES.md for source/artwork provenance. Legacy personal-calendar widgets/ICS workflows are not exposed in this first shift-workspace preview.
