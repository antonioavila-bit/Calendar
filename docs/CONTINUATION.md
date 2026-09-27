# Offline Security Calendar continuation

Application repository: antonioavila-bit/Calendar. Canonical development branch: security/offline-shifts-v1, draft PR #1. The separate offline-security-calendar branch's branding/text-export requirements are being consolidated here; do not develop competing releases on both branches. The planning repository remains antonioavila-bit/Offline-Security-Calendar.

Baseline f8dac11335654ada5b5c163f3fa4ea4a4d2718ba passed Android build/unit-test run 36289407385. It is the inherited calendar engine, NOT the customized scheduling deliverable. The inherited upstream image-minimizer job failed separately; it is removed on this branch, but pull_request_target still reads main until the PR is merged.

Scheduling core now implements independent requirements, multiple personnel/post assignments, coverage, overlap rejection, strict overnight/timezone/DST behavior, review-only bulk parsing, idempotent duplicates, and Unicode printable text. Thirty core scenarios passed locally using kotlinc/JVM; equivalent JUnit tests are checked in. This is not yet a finished APK/UI.

Next: connect normalized local persistence, required-shift/person/post entry, month/board/uncovered views, native IME handwriting, TXT/clipboard/SMS/share, encrypted backup/restore, launcher artwork, offline manifest and APK verification. Preserve GPL notices and both donors read-only. Device acceptance is pending on Galaxy S25 and Tab S7. Never assume a Wi-Fi tablet can transmit carrier SMS or that the device keyboard is a bundled recognizer. Preserve keyboard fallback.

The latest navy seahorse/calendar/clock artwork supersedes the blue calendar image. Phone/tablet APK must not request INTERNET or sync to the OS calendar provider. Local text export uses UTF-8 and CRLF and can be transferred by USB to Windows 11 for Word/Notepad printing.
