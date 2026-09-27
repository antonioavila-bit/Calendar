# Offline Security Calendar customization

This branch is the writable application codebase for the Offline Security Calendar project.

## Product requirements

- Fully offline Android scheduling application.
- No Internet dependency, cloud account, telemetry, online licensing, or remote API for normal operation.
- Production acceptance targets include Samsung Galaxy S25 and Samsung Galaxy Tab S7.
- Tablet and phone responsive layouts.
- Samsung S Pen / supported Android stylus handwriting-to-text with keyboard fallback.
- Personnel-first bulk schedule entry that automatically sorts assignments by date, shift and post.
- Multiple personnel per shift and multiple simultaneous posts.
- Required staffing counts and coverage states: UNFILLED, PARTIALLY FILLED, FILLED, OVERSTAFFED.
- Dedicated uncovered-shifts view.
- Conflict detection.
- Repeating shifts, templates and copy day/week/range.
- Personnel and post/location records.
- Manual SMS text generation and copy.
- SMS composer handoff for individual personnel, selected shift/group, or complete schedule.
- Android share sheet and plain-text export.
- Local backup/restore.
- Latest supplied navy seahorse/calendar/clock Shift Calendar artwork is the required launcher icon; it supersedes earlier artwork.

## Windows 11 / printable text export

The application must provide an **Export Schedule as Text (.txt)** option.

The export must:
- work entirely offline;
- use UTF-8 plain text;
- be saved through Android's document/save interface so the user can choose a local folder or removable/offline-accessible storage;
- be transferable to a Windows 11 PC by USB, removable media, or any user-selected offline transfer method;
- open cleanly in Windows Notepad and be suitable for copying directly into Microsoft Word;
- use a human-readable printable layout rather than JSON/CSV or internal database formatting;
- allow date-range selection;
- optionally include all personnel or selected personnel;
- optionally group by date, post/location, or person;
- include shift date, start/end time, post/location, assigned personnel and coverage status;
- include an optional generated-at timestamp and notes;
- avoid relying on fixed-width formatting that breaks when pasted into Word.

Example:

OFFLINE SECURITY CALENDAR
Schedule: October 2026
Generated: October 1, 2026

THURSDAY, OCTOBER 1
Hotel
Night Shift — 6:00 PM to 6:00 AM
Assigned: Tate; Brett Raftery
Coverage: FILLED

FRIDAY, OCTOBER 2
Hotel
Night Shift — 6:00 PM to 6:00 AM
Assigned: Tate
Coverage: FILLED

SATURDAY, OCTOBER 3
Hotel
Night Shift — 6:00 PM to 6:00 AM
Assigned: UNASSIGNED
Coverage: UNFILLED

The same formatter should be reusable for clipboard copy and manual SMS where appropriate, with a compact SMS format kept separate from the printable text format.

## Repository boundaries

Writable application repository:
- antonioavila-bit/Calendar, branch offline-security-calendar

Read-only donor/reference:
- FossifyOrg/Calendar (GPLv3 upstream)
- antonioavila-bit/Inventory-App (strict read/copy-only donor for proven offline stylus handwriting behavior)

Do not modify either donor repository.
