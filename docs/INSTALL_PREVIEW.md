SHIFT CALENDAR — ANDROID HARDWARE-TEST PREVIEW
Version: 0.1.0-preview03

This is the customized scheduling APK, not original Fossify Calendar.
It is debug-signed for testing, not a production release. Do not install an unsigned or androidTest APK.

INSTALL / UPDATE SAFELY
1. Before replacing any installed preview, make an encrypted backup and keep its password. Debug signing can change between builds; Android may reject an in-place update. Never uninstall a data-bearing app without a safe, restorable backup.
2. Download Shift-Calendar-preview-APK from a fully successful Offline Calendar APK run in antonioavila-bit/Calendar. Both emulator jobs must pass, not only compilation.
3. Extract the ZIP. Transfer Shift-Calendar-0.1.0-preview03.apk by USB to the Galaxy S25 or Tab S7; the Android device does not need Internet.
4. Open in My Files and follow Android's installation prompt. Turn the temporary install-source allowance off afterwards.
5. Open the navy seahorse Shift Calendar icon. Long-press in the app drawer to add to Home, or use Menu > Settings / backup > Add home-screen shortcut and approve the launcher prompt.

EXACT README FAST-ENTRY TEST
1. Add personnel Mark, Bill and Tate and post Hotel. Names/posts must match saved records.
2. Create Hotel required shifts for Oct 1-31 2026, 6 PM-6 AM, all weekdays, required personnel 1. This tells the app which nights need coverage.
3. Open Menu > Enter schedules. Select year 2026 and the intended time zone, e.g. America/New_York. Enable 'Also create any missing required shifts' for the September/November dates in this example (or create those requirements first).
4. Paste or type these blocks in any order. Leading README bullet markers are also accepted:

Mark
Oct 14-25
6 PM-6 AM
Hotel

Bill
Oct 26-Nov 8
6 PM-6 AM
Hotel

Tate
Sep 30, Oct 1, Oct 2
6 PM-6 AM
Hotel

5. Tap Review and sort schedules. Check Mark 12, Bill 14, Tate 3: 29 new assignments. Review includes each person, original date/range, normalized dates, shift time, post, counts, conflicts and missing/ambiguous fields. Overnight endings are explicit. Nothing has been saved yet.
6. Cancel once and check no assignments were saved; reopen review and Save all. Any invalid batch shows BLOCKED and no Save all button; correct all issues first.
7. Open Uncovered. For October with only this example data, Hotel nights Oct 3-13 remain unfilled (11 shifts, 11 open positions). Other real assignments would change this result.
8. Re-entering the same data must add no duplicate assignments.

FILTERS AND PRINTABLE EXPORT
Menu > Schedule > Filters / search includes from/through start dates, personnel, post, shift/time, coverage and search. Apply filters. Saved conflicts are normally absent because overlapping assignments are blocked.
Menu > Share / TXT includes date/person/post selection and printable grouping. Save .txt locally or Copy for Word. Transfer by USB to Windows 11, open in Notepad, select/copy and paste into Word to print. This actual transfer/printing path still needs device acceptance.

SMS / TABLET / HANDWRITING
Manual SMS opens the installed Messages composer for your review and sending. Direct SMS is off by default, requires opt-in/permission/carrier hardware/default SIM and final recipient review. It never automatically broadcasts schedule changes. Test only intended recipients. A Wi-Fi-only tablet cannot send carrier SMS: export/copy and transfer to a messaging-capable phone.
Handwriting uses the installed Samsung/Android keyboard, not a bundled recognizer. On Tab S7 use the Samsung Keyboard handwriting mode with S Pen; supported newer devices also offer native stylus input. Verify airplane-mode handwriting on each actual device. Typing remains available.

ACCEPTANCE LIMITS
This remains a preview. Actual S25/Tab S7, S Pen/keyboard offline behavior, portrait/landscape/large-font layouts, local picker/USB/Word, carrier SMS, reminder/reboot/permission denial, scale and protected stable signing remain gates. The three previously missing features are implemented here; see PREVIEW03_FEATURE_GAPS.md for their acceptance tests and the earlier README_CONFORMANCE_AUDIT.md for historical context.
Backups are encrypted; their password cannot be recovered. Source/build scripts, commit, checksum file and actual APK reports accompany the CI artifact. Original GPLv3 and third-party notices remain included.


NEW IN PREVIEW03
Menu → Templates manages independent patterns; Use selects dates/post and shows review. Share / TXT → Choose people for SMS selects arbitrary recipients; review/copy/individual composer and opt-in confirmed direct sending remain separate. Schedule → Filters / search includes sort field and ascending/descending controls.

This preview uses schema2 and backup JSON2. Old schema1 records migrate without deleting schedules and old backup JSON1 is accepted. New backups containing templates cannot be opened by old previews. These database/backup tests do not establish Android package upgrade compatibility: this is still debug-signed. Use a clean test device/profile unless the installed signing certificate matches, and keep data-bearing installs/backups intact.
