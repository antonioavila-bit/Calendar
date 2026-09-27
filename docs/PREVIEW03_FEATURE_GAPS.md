# Preview03 — template, recipient and sorting implementation

The user requested these three concrete README gaps, not a relaxation of the original requirements. Baseline b9656446e4274aac6568e96233b681679361c449 passed preview02 acceptance. This change is a hardware-test preview, pending its own CI acceptance and physical Samsung tests.

## Independent templates
Menu → Templates → Add template. Save a unique name, shift label, start/end times, time zone, staffing count, chosen weekdays, notes and an optional default post. Templates exist without any required shift. Edit, Duplicate and Delete are available; deleting requires confirmation. Template edits/deletion never rewrite earlier required shifts or assignments. Use selects explicit dates and post, previews requirements, then Save all commits. Cancel does not write. Matching existing requirements keep their staffing and assignments. Missing post, invalid weekdays/times/staffing, duplicate names and ambiguous DST times block changes. At most366 distinct dates per use. Each generated shift is a snapshot, not a live link to the template.

SQLite schema2 adds the templates table within SQLiteOpenHelper's upgrade transaction without deleting v1 rows or resetting revision. All writes retain revision checks, validation and atomic commit. Templates use post references; a referenced post cannot be deleted. Backup JSON version2 includes templates; old version1 backups without templates remain readable. Restoring an old backup intentionally produces zero templates as part of the reviewed full replacement, not a merge. Encryption envelope/algorithm is unchanged.

## Arbitrary SMS subsets
Share / TXT → Choose people for SMS → check names → Use selection. Cancel leaves selection unchanged; Clear selection does not send. Selection uses stable person IDs, persists locally and is separate from the single-person TXT selector. Date/post/uncovered filters still apply. Review selected people's SMS displays each recipient and individual schedule; Copy selected SMS text copies the reviewed individual texts. Prepare SMS one by one offers only selected names and opens the chosen normal composer; the user must press Send there. Send selected automatically requires the existing opt-in, SMS hardware/permission/default SIM and final recipient/message/segment confirmation. No message is sent by selecting people or editing schedules.

Empty selection never falls back to all. Unknown/deleted people or selected people with no matching shifts block the whole batch instead of silently excluding them. Blank/invalid phone numbers allow reviewing/copying but block that recipient's composer/direct send. Direct batches reject shared phone numbers, preserve the1–20-recipient/20-segment-per-person/100-segment batch limits, and never auto-retry. Tests only prepare/copy/review; real carrier transport remains untested. Wi-Fi-only tablets keep all local selection/copy/export paths.

## Selectable board sorting
Schedule / Uncovered → Filters / search → Sort by and Sort direction. Options: Start date/time; Post/location; Shift/time; Assigned personnel; Coverage status; Open positions. Both directions supported. Sort operates on the filtered result and never reintroduces excluded rows or mutates stored data. Coverage ascending: unfilled, partial, filled, overstaffed. Personnel sorts the alphabetized full assigned-name list (unassigned last ascending). Primary ties always resolve by chronological instant, post, shift label and stable ID. Sort choice persists in local preferences; current form filters survive Activity recreation.

## Test contract
26 new pure tests plus previous55 =81. Nine new instrumented tests plus previous16 =25 on each emulator. Tests include real Activity template CRUD/use/review/cancel, recipient picker/cancel/clear/recreation/privacy, actual board order, SQLite1→2 migration preservation, encrypted backup round-trip with templates, old backup decoding, rejection of malformed template input and stale revisions. Existing exact README bulk-entry acceptance remains unchanged. CI does not send SMS or waive assertions.

These three features can be marked verified only after preview03's own build/JUnit/emulator checks succeed. Stable signing and physical S25/Tab S7, handwriting, carrier SMS, Windows export/printing and operational release qualification remain separate.

API references: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper and https://developer.android.com/reference/android/app/AlertDialog.Builder
