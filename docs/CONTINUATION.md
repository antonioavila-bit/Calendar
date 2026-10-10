# Preview04 continuation — calendar-first home and month reset

Authorized application repository: antonioavila-bit/Calendar only. Development branch security/simple-calendar-preview04, based on accepted preview03 dc3a1e3d00ca1e974ed2555a8a1f3c8045d99228. User's installed app is preview02/code101. Preserve preview03 features. Keep the existing application PR and this follow-on PR draft/unmerged.

Implemented: Settings hides auxiliary navigation; Add shifts on the home page; filled dates show saved-shift counts; whole-month agenda shows names/posts/times; date selection and Show whole month; remember displayed month; first launch chooses a month with saved data nearest today. Save reviewed shifts returns to their calendar month.

Settings > Clear [month/year] reviews counts, supports cancel, removes only shifts starting in that local month and associated assignments. People, posts, templates and other months remain. A single app-private SQLite recovery snapshot is committed atomically with reset and survives restart. Undo is revision-checked and expires at the next successful roster write; failure/cancel preserves undo. Schema3 transactionally migrates schema1 and schema2. Backups remain JSON2, accepting legacy JSON1. Recovery snapshot is not exported in backups.

Verification added: three instrumentation cases for visible saved names/counts and clear/cancel/undo across Activity recreation; cross-month overnight boundaries, templates, empty clear, stale revision, reopening storage, undo expiry; schema2 migration. Existing schema1 test retained. Navigation test selectors changed only for intentional Menu -> Settings / home Add shifts changes. Require all 28 device tests on each API33 tablet/API35 phone. Existing 81 JVM tests retained.

Local Gradle verification was attempted but blocked downloading the Gradle distribution (network unavailable). git diff --check passed. No local APK/emulator success is claimed. Use one consolidated CI build with both device jobs; inspect results before delivery. Update this document with the verified outcome.

Signing: preview02 certificate 494d169ab0bd4bc704af8465b0d6c8bae8b3184c6b6e3a21ec2dee318e56a7b5; stable protected signing remains unresolved. Do not uninstall a data-bearing app. Preserve encrypted backup/password; compare actual built certificate. Samsung physical layouts/large fonts/stylus and carrier SMS remain unverified.
