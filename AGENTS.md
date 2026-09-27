# Offline Security Calendar — development contract

The user authorized this fork as the Android application codebase on 2026-09-26.

## Boundaries
- Application writes: antonioavila-bit/Calendar only.
- Planning/handoff documentation: antonioavila-bit/Offline-Security-Calendar.
- FossifyOrg/Calendar is read-only upstream. Never open upstream PRs/issues or change it.
- antonioavila-bit/Inventory-App is strictly read-and-copy-only. Do not create branches, commits, PRs, issues, settings changes, or other writes there.
- Retain GPLv3 and original copyright notices. Do not publish personnel data, phone numbers, credentials, or signing keys.

## Product contract
One fully offline Android APK for Galaxy S25 and Galaxy Tab S7. Dedicated multi-person shifts, reusable people/posts, staffing requirements, uncovered/partial coverage, overlapping-assignment detection, personnel-first bulk entry with review, calendar/board views, SMS/copy/share, local backups, and stylus input with keyboard fallback. Use the latest user-supplied navy seahorse/calendar/clock artwork, not the superseded blue icon.

Scheduling must use local data, no INTERNET permission, online account, telemetry, cloud sync, or runtime model download. Device keyboard handwriting is a separate IME capability; never describe it as a bundled recognizer or claim offline hardware acceptance without a real airplane-mode test. Wi-Fi-only tablets cannot send carrier SMS. Explicit user-confirmed sharing is allowed; never claim an SMS composer handoff means delivered. Optional direct SMS must be opt-in and permission-gated.

A required shift exists independently of its assignments; otherwise gaps cannot be calculated. Shift dates refer to the START date. Overnight end dates, cross-month/year ranges, DST, duplicate input, cancellation, and database transactions need tests. Do not silently guess missing people/posts/times/years.

## Verification
Work on a development branch and PR in THIS fork, not upstream. Read current files/checks before edits. Distinguish source changes, successful builds, installable artifacts, emulator checks, and actual S25/Tab S7 acceptance. Never publish a docs-only or unsigned build as a tested application. Keep docs/CONTINUATION.md current at meaningful checkpoints.
