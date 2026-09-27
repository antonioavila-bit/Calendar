# Continuation — Offline Security Calendar

## Verified starting state
- Android source repository: antonioavila-bit/Calendar (user-approved fork).
- Pinned Fossify baseline commit: 764b24cda492345653bd18c36d2498a01bd8eb76.
- Actual baseline tree: c8f5c22a03e87cfd30587ebe2a09a680d617f8d9. Earlier planning text incorrectly called the commit SHA a tree SHA.
- Planning repository: antonioavila-bit/Offline-Security-Calendar. Its foundation PR contains documentation, not a built Android app.
- No customized APK verified yet at this checkpoint.

## Active work
Branch: security/offline-shifts-v1.
First establish a reproducible fork-local Android build and source archive; then customize identity, offline boundary, launcher artwork, scheduling model/UI, handwriting, sharing and backup. Do not run inherited Fossify publishing/maintenance workflows in this fork.

## Handwriting donor actually inspected
Read-only Inventory-App commit 260194c0766c280f82c76a1b03f19df0770575fb, app/src/main/java/com/inventory/offline/MainActivity.kt, around lines 2900–3220. Uses native EditText, setAutoHandwritingEnabled on API 34+, stylus/eraser pointer detection, InputMethodManager.startStylusHandwriting, and keyboard fallback. This delegates recognition to the installed keyboard, not to a bundled handwriting model. The user reports this approach worked on their S25 and Tab S7; this application's offline device tests remain pending.

## Hardware gates
Galaxy S25 and Galaxy Tab S7: install/start without Internet, correct latest icon, portrait/landscape and large font layout, data entry/relaunch, overnight assignments, gaps/conflicts, keyboard dismissal, actual device handwriting in airplane mode, backup/restore, reminders, safe sharing. Carrier SMS requires a supported modem/SIM/service; tablet copy/file export must remain usable without telephony.
