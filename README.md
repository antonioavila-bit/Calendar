# Shift Calendar — Offline Security Calendar

![Approved navy seahorse calendar icon](app/src/main/res/drawable-nodpi/shift_brand.webp)

An independent GPLv3 customization of Fossify Calendar for offline Android personnel/post scheduling. The active application branch is **security/offline-shifts-v1**, draft PR #1. The APK is a **hardware-test preview**, not yet a production-accepted release.

## Main workflow
Add Personnel and Posts, create the required shifts and staffing counts, then assign personnel directly or enter successive person/date/time/post blocks. Review before saving. The calendar and schedule board automatically place assignments, flag conflicts and show unfilled or partly filled shifts. A blank calendar is not proof of coverage: requirements must be entered first.

Several people can cover the same shift, and several posts can run at the same time. Required-shift patterns support selected weekdays and finite date ranges. Existing patterns can be reused, and days/weeks/ranges can be copied. Overnight shifts belong to their START dates and display their actual end dates and time zones.

## Offline input, sharing and backup
- Native keyboard input and supported installed-device handwriting, adapted from the owner's expressly authorized read/copy-only Inventory-App pattern. The app does not download a recognition model or contact a cloud handwriting API.
- Manual SMS/copy, person-specific SMS composer, confirmed opt-in direct SMS batches, and the Android share sheet. Consecutive matching shifts are compressed to readable date ranges in SMS. A Wi-Fi-only tablet can export/copy but needs a separate messaging-capable phone for carrier SMS.
- Readable UTF-8 .txt export with Windows line endings, date/person/post filters and date/post/person grouping. Save locally, transfer to Windows 11 by USB, and open in Notepad or paste into Word to print.
- Application-private local database with validated, revision-checked atomic changes. Password-protected encrypted backups and validated restore. Keep the password safe; it cannot be recovered.
- Local reminders and the approved navy seahorse/calendar/clock launcher icon. Home-screen pinning requires the launcher's confirmation.

## Build and installation
Use the **Offline Calendar APK** workflow on this branch. The deliverable artifact is named **Shift-Calendar-preview-APK-<run number>**. Install only the contained `Shift-Calendar-0.1.0-preview01.apk`, not the separate instrumented-test APK.

See [installation and quick-start instructions](docs/INSTALL_PREVIEW.md), [verified checkpoints and remaining gates](docs/CONTINUATION.md), and [source/artwork provenance](docs/THIRD_PARTY_NOTICES.md).

The APK has its own application ID, separate from official Fossify Calendar. Android 8/API 26 or newer is the technical minimum. Galaxy S25 and Galaxy Tab S7 are required physical acceptance targets; emulator checks do not replace testing those actual devices, Samsung handwriting, SMS transport or reminders. Debug signing is for previews; stable production signing remains a release gate. Back up before changing installed builds.

Build/dependency downloads occur on the CI/build machine, not the offline phone/tablet. The installed app has no INTERNET or OS calendar-provider permissions and disables automatic Android app-data backup. External SMS, share, document and keyboard apps remain under their own permissions.

## Current preview scope
The first preview uses a dedicated normalized shift workspace. Inherited personal-calendar editors, widgets and ICS workflows are not exposed in this preview; retaining a single consistent staffing model takes precedence over editing those separate event records. Legacy source remains for future adaptation. There is no claim of complete Fossify feature parity or completed production acceptance.

## License and repository boundaries
The original [GNU GPLv3 license](LICENSE) and copyright notices are retained. Corresponding source and build scripts accompany the APK artifact. This is not an official Fossify release. Only this application repository and the separately authorized planning repository are writable for this project; **FossifyOrg/Calendar** and **antonioavila-bit/Inventory-App** are read/copy-only donors.
