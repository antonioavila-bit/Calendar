# Offline Security Calendar continuation — README audit correction

## Repository boundaries and working branch
User-approved application repository: antonioavila-bit/Calendar. Branch security/offline-shifts-v1; application PR #1 must stay draft. Planning/specification repository: antonioavila-bit/Offline-Security-Calendar. Both FossifyOrg/Calendar and antonioavila-bit/Inventory-App remain strictly read/copy-only. The old offline-security-calendar branch is not a second release line.

## Previously verified deliverable
Preview01 commit 7f365e20988f7964c9759a5d64ce42b002414fec, tree 8e014c758a01dcbcdc85f83fc815f2f0318b3340, Offline Calendar APK run 36292143974 (#5): 34 JUnit tests, 12 instrumented tests per API33 tablet-layout and API35 phone emulator in airplane mode, actual APK checks passed. Artifact 10922308542 / Shift-Calendar-preview-APK-5. APK SHA256 4a02faf4a8d478cbfc678386a8561a4fcaaffc3d57cda96f5bc6cb09f3a76bb8. Source ZIP SHA256 1ac23c836a80d46beffd2142b9984b7d47406426ab93ede882ee6aba086d52bd.

## Current correction and its verification rule
The user reported failed latest runs and asked for a strict comparison with the planning README. See README_CONFORMANCE_AUDIT.md, which pins the exact requirement blob and distinguishes already-working functions, corrected gaps and remaining work. Preview01 was not fully conformant: bullet paste, comprehensive invalid-entry review and board filters needed correction.

This commit changes version to 0.1.0-preview02 / code101. It adds pure BulkScheduleReview and ScheduleFilters, connects a full review before atomic revision-checked saving, accepts README bullets, reports all missing fields/conflicts and disables Save for invalid input. Board filters gain date ranges, shift/time, all coverage states, conflicts and search. Old ScheduleEntry.bulk delegates to the same review engine.

21 new JVM tests join the original34 (55 expected); four real Activity/dialog/database/filter tests join the original12 (16 per emulator expected). All55 methods passed in a local JVM assertion harness before submission. The new commit's actual GitHub JUnit, APK and both emulator jobs are NOT assumed passed: fetch their results and artifact checksums before replacing preview01. Record the accepted commit/run/artifact/hash in the PR checkpoint after completion. No real SMS is sent during tests.

## CI failure cause and repair
Reported runs 36292508835 and 36292640273 were inherited Image Minimizer failures due to empty GitHub App client-id/app-id, not APK compilation/test failures. The workflow remained in main despite removal on the app branch. CI-only PR2 merged as 17d6fb339f3b3d67b7eecb5064231f1eb2cd086c, removing upstream-only default-branch automation and adding fork-local policy checks. The policy check passed before merge (36292956425). Main does not contain the app-preview code. Historical failures stay visible. Do not request upstream secrets or rerun those obsolete jobs to fabricate green history.

## Current functionality / open requirements
Normalized app-private personnel/posts/shifts/assignments; required staffing/coverage; local bulk preview/commit; finite recurrence/pattern reuse/copies; keyboard/device handwriting; manual/personal/selected-shift share and opt-in direct batches; UTF-8 CRLF TXT; encrypted backups/atomic restore; local reminders; approved launcher assets.

Open feature gaps: independent template records/manager, arbitrary multi-person subset selection for SMS, selectable board sort order. Autochronological order and existing pattern reuse are not full substitutes. Legacy personal-calendar widgets/editors/ICS not exposed. Finite date-range generation and 90-day reminders remain documented limits.

Open release gates: actual Galaxy S25/Tab S7 and Samsung IME/S Pen offline acceptance, physical layouts/large-fonts, carrier SMS/default SIM, document-picker/USB/Windows Word printing, reminder timing/reboot/permission denial, large-roster performance and data-bearing upgrades, protected stable production signing. Debug certificates can differ; never uninstall a data-bearing preview without a restorable backup.

## Durable evidence
Approved icon app/src/main/res/drawable-nodpi/shift_brand.webp remains unchanged, SHA256 c62ad97916c00194c5f1c82d313e9884b5f5c5454fcf85924084301b01550cc7. APK artifacts contain exact source/commit/checksums and signature/manifest/permission reports. Device-evidence artifacts contain instrumentation output, airplane-mode flag, logcat and screenshots. The repository and verified CI artifacts, not a transcript claim, are the build source of truth.
