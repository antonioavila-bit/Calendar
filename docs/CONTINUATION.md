# Offline Security Calendar continuation

## Canonical working state
Writable application: antonioavila-bit/Calendar. Development branch: security/offline-shifts-v1; draft PR #1. The older offline-security-calendar branch was requirements/branding-only and is not a competing release branch. Its custom package identity, Shift Calendar name and Windows TXT requirements are incorporated here. Planning/history repository: antonioavila-bit/Offline-Security-Calendar. FossifyOrg/Calendar and antonioavila-bit/Inventory-App remain strictly read/copy-only donors; do not modify them.

## Verified checkpoints
- f8dac11335654ada5b5c163f3fa4ea4a4d2718ba: original calendar-engine baseline, run 36289407385 passed. NOT the customized APK.
- 5646b34ad869ce71b721274b1ff304c2f6e50992: scheduling domain/parser/coverage plus 30 JUnit tests, run 36290022970 passed; the same 30 scenarios also passed in a local JVM harness.
- ad394a1793b6c617e5348010e631b0e27b7d037c: first connected customized APK, run 36290791791. Build, actual APK signature/identity/offline permission/icon checks, and ten instrumented tests on EACH API 33 tablet-layout and API 35 phone emulator passed in airplane mode. Download artifact Shift-Calendar-preview-APK-3, ID 10922655414. APK SHA256 3e458831f23e0d875a83b24cf2145a8069edabd9e5111914fa2d6e52d87e2044.
- The follow-up commit containing this document adds range-compressed SMS, four SMS unit tests, two broader UI/clipboard/orientation tests per emulator and removes unused Commons global-settings/handwriting-font initialization. Its workflow must be checked before preferring its APK. Never claim an in-progress or failed job passed.

## Implemented preview
Native offline shift workspace with normalized SQLite database, revision-checked atomic writes, Personnel/Posts CRUD, multi-person assignments, explicit requirements, month view, schedule board, uncovered/partial coverage, person/post filters, strict conflict rejection and overnight/DST handling, person-first four-line/pipe bulk input with review, finite weekday recurrence and reusable shift patterns, copy day/week/range, native IME handwriting adapter and keyboard fallback, local reminders, manual SMS, person-specific SMS, confirmed opt-in direct SMS batches, generic Android share, clipboard and readable UTF-8/CRLF TXT, authenticated encrypted backup/validated atomic restore, approved navy seahorse icon and user-approved home-screen pinning.

## Limits / release gates
This is a debug-signed hardware-test preview, not production accepted. No physical Galaxy S25 or Tab S7 test has occurred here. Test Samsung keyboard/S Pen offline, phone/tablet portrait/landscape/large-font and keyboard layouts, TXT save through the real document picker and Windows/Word printing, backup/restore on hardware, notification timing/permission denial/reboot, and real SMS only to intended test recipients. Emulator tests do not certify those Samsung/carrier behaviors. Preview signing can change between CI builds; back up before replacing any installed app. Establish protected, stable production signing before operational deployment.

Scheduling data is app-private; it is not sent to the Android/cloud calendar provider. There is no INTERNET permission, telemetry, cloud login or runtime model download. A Wi-Fi-only tablet cannot transmit carrier SMS. The selected external IME, share app or document provider has its own permissions; the installed keyboard must already support offline handwriting.

The first preview is a dedicated shift workspace. Legacy Fossify personal-calendar activities, widgets and ICS workflows are retained as source but are not exposed in this preview, so they cannot bypass staffing/coverage consistency. Recurring requirements generate a selected finite range, not forever. Large-data performance, stable upgrades, legacy interoperability and broader UI polish remain later gates. The default reminder horizon is 90 days and must be re-evaluated for large multi-post schedules before production acceptance.

## Durable assets
Approved launcher bitmap app/src/main/res/drawable-nodpi/shift_brand.webp is committed; SHA256 c62ad97916c00194c5f1c82d313e9884b5f5c5454fcf85924084301b01550cc7. It is resized from the exact latest navy image; the superseded blue image is not used. Original PNG/provenance hashes are documented in THIRD_PARTY_NOTICES.md. No need to ask for the icon again for this preview.

Each APK artifact includes the exact source archive, commit, checksum file, permissions/manifest/signature reports and INSTALL_PREVIEW.txt. Device evidence is a separate artifact including airplane-mode flag, instrumentation output, logcat and screenshots. Read the actual run/artifacts before supplying links. Keep PR #1 draft until release gates are satisfied; do not modify upstream or the handwriting donor.
