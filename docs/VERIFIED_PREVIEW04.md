# Preview04 verified build — 2026-10-10

Application source: 9d5bd3e74d41c7e58265908d2d49259aef3f5e85.
Draft PR: https://github.com/antonioavila-bit/Calendar/pull/3
Run: https://github.com/antonioavila-bit/Calendar/actions/runs/38090526843 (number 11).
Version: 0.1.0-preview04 / code103.

Build and both emulator jobs SUCCESS on the first consolidated attempt.
Actual Gradle report: 81 tests, zero failures, zero skipped.
API33 tablet and API35 phone: OK (28 tests), 28 successful method completions each, airplane mode 1.
Coverage includes saved-name/count visibility; hidden auxiliary controls; month-clear review/cancel; persistent undo after Activity recreation and storage reopen; adjacent-month overnight boundaries; people/posts/templates preservation; stale writes; empty reset rejection; undo expiry after later saves; schema1 and schema2 migration. Prior template/SMS/sort/backup/entry regression cases remain.
No real carrier SMS sent. Actual Samsung hardware/large-font acceptance remains pending.

Phone and tablet home screenshots inspected. Tablet shows the calendar and saved agenda; on the small phone the lower weeks and agenda require scrolling. A prior clipboard overlay/toast appears in captured phone evidence, so the screenshot alone is not proof of every rendered row; actual view assertions and persistence tests passed.

APK SHA256: a4e19f319180fcad230b56b2c0f7367df9267e54b4b5e4d93b4270cd52927960
Source ZIP SHA256: 43b8192fd1a277d52d2e5162ef71717c618b67d936f5b545b3064d43917a81ca
CI artifact ZIP SHA256: 340f30af4643efdaf662defc7d1826a350b4a76092fc1cb5ea2582ebfabf7b3b
Artifact ID11683967895, Shift-Calendar-preview-APK-11.
API33 evidence11684575151; API35 evidence11684052805; build diagnostics11684037678.
Downloaded archive digests and contained APK/source checksums verified locally. APK identity, offline permissions, artwork and signature checks passed in CI.

## Installation limitation — confirmed signing mismatch
New certificate SHA256: d27b0c59c59afca24fad84304474abe25ce53a7ee031df54364868561297759c
Installed preview02 report certificate: 494d169ab0bd4bc704af8465b0d6c8bae8b3184c6b6e3a21ec2dee318e56a7b5
Same app ID, different certificates: this APK cannot update preview02 in place.
Keep the existing data-bearing installation. In preview02 use Menu > Settings / backup > Encrypted backup, save the file and keep its password. Validate restoration on a clean device/profile before replacing a data-bearing installation. Do not uninstall merely to force installation. Stable signing remains unresolved.

## Use
Home: month navigation, Add shifts, Settings; saved shifts marked on dates and names/posts/times in the agenda below. Tap a date for its entries, Show whole month to return.
Reset: display October 2026 > Settings > Clear October 2026 > review counts > Clear month. Cancel makes no changes.
Undo clear October 2026 survives restart until the next successful roster change or another reset. Undo is held in app-private storage, not exported in backups. Other months, people, posts and templates remain. Shifts belong to local start dates.

PR remains draft/unmerged; no user device data was modified.
