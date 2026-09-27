# README conformance audit — 2026-09-27

## Exact requirement and delivered build inspected
The requirement source is `antonioavila-bit/Offline-Security-Calendar`, `README.md` on main, blob `aae0c7b90e1f13c986705bf0cb45592523ce7e06`. The original functional requirements have not been weakened to fit the implementation. The later user-approved move to `antonioavila-bit/Calendar` supplies the writable application boundary; the old planning README's single-repository wording is historical.

The delivered build inspected was `0.1.0-preview01`, commit `7f365e20988f7964c9759a5d64ce42b002414fec`, source tree `8e014c758a01dcbcdc85f83fc815f2f0318b3340`, APK SHA256 `4a02faf4a8d478cbfc678386a8561a4fcaaffc3d57cda96f5bc6cb09f3a76bb8`. Its actual source was extracted from the verified run-5 source archive, not inferred from its marketing text or an unrelated repository.

**Overall finding: preview01 was partially conformant, not a completed implementation of every README requirement or production acceptance gate.** The core personnel-first workflow worked, but error review and some filters fell short. This audit's corrective source is version `0.1.0-preview02`; that source must pass its own APK and emulator workflow before being called a verified replacement.

## Failed GitHub runs are not failed APK builds
The reported latest failures `36292508835` and `36292640273` were inherited **Image Minimizer** workflows. Their GitHub App token step failed because `client-id` / `app-id` was empty. The original APK workflow `36292143974` succeeded: 34 JUnit tests and 12 instrumented tests on each of API33 tablet-layout and API35 phone emulators.

Removing inherited workflows only on the application branch did not stop default-branch `issue_comment` and `pull_request_target` automation. CI-only PR #2 removed those upstream-only workflows from this fork's main branch and added a read-only fork policy check. It was merged as `17d6fb339f3b3d67b7eecb5064231f1eb2cd086c` after the policy check passed. It did not merge application preview PR #1 or disable its APK/unit/device tests. Historical failed runs are retained. No upstream credentials or donor changes are needed.

## Exact fast-entry results
Using saved personnel Mark, Bill and Tate, saved post Hotel, explicit year **2026**, time zone **America/New_York**, and either pre-existing requirements or the explicitly enabled missing-requirements option:

| Person | README date expression | Assignments |
|---|---|---:|
| Mark | Oct 14-25 | 12 |
| Bill | Oct 26-Nov 8 | 14 |
| Tate | Sep 30, Oct 1, Oct 2 | 3 |
| Total | All three entries, in any of six person orders | 29 |

The plain four-line input and pipe syntax passed even in preview01. Every assignment begins at 18:00 on its start date and ends at 06:00 the following date. Bill's last shift starts November 8 and ends November 9. Required shifts already in the roster keep their staffing count.

With Hotel requiring **one person every night October 1-31** and only these three example schedules entered, the remaining October gaps are **October 3-13 inclusive: 11 required shifts / 11 open positions**. This is a controlled example, not an assertion about any fuller operational roster containing other people.

## Fast-entry gaps and corrections
| Requirement | preview01 finding | Corrective preview02 source |
|---|---|---|
| Successive personnel blocks in arbitrary order | Implemented for plain four-line and pipe input | Preserved; exact examples tested in all six orders |
| Paste the README's literal bullets | Leading `-` was treated as part of the name and rejected | Strips supported leading list markers without changing date-range hyphens |
| Parsed person/date/time/post before saving | Shown in a generic valid-result roster preview | Explicit per-entry fields, original expression, normalized dates, time and post |
| Number of assignments | Aggregate new-assignment count | Aggregate and per-entry new assignments, new requirements and duplicates skipped |
| All conflicts | First validation failure interrupted processing | Collects overlapping person/shift pairs, including dates, times, zones and both posts |
| Ambiguous or missing fields | First error dialog; later issues were not shown | Collects issues across entries and individual fields; later valid entries remain visible |
| No save before review | Implemented | Preserved; invalid report has no Save all action, cancel leaves database/revision unchanged |
| Deterministic and offline | Implemented | Pure local parser/report; no AI service, fuzzy name guesses or network calls |

Blank-separated incomplete blocks are reported instead of silently taking a field from the next person. A year is explicitly selected for month/day input; unknown names/posts require correction, not automatic guessing. Four-line or pipe syntax is supported, not unrestricted free-form prose. Recurrence and bulk batches are bounded; a review accepts at most 500 changed shifts.

## Other README requirements
| Area | Conformance finding |
|---|---|
| Offline scheduling / storage | Implemented in app-private storage; actual APK permission checks prohibit Internet and OS calendar-provider/contact access. External keyboard/share apps have separate permissions. |
| Multiple personnel per shift and simultaneous posts | Implemented, with unique assignments, staffing counts, overlap rejection and transactional writes. |
| Personnel / posts | Reusable editable records implemented. Names/posts are set up before bulk entry. |
| Coverage states and Uncovered screen | UNFILLED, PARTIALLY FILLED, FILLED, OVERSTAFFED; required-versus-assigned and open-position counts implemented. Missing requirements cannot be inferred from a blank calendar. |
| Schedule Board filters | preview01 had month, person, post and uncovered only. Corrective source adds arbitrary from/through dates, day/night/custom label/time patterns, each coverage status, conflicts and text search. Saved overlaps are normally absent because invalid assignments are rejected. |
| Sorting | Automatic chronological order implemented. A user-selectable column/sort-order control is not yet implemented; the README's sortable-view wording is not fully met. |
| Repeating schedules / copy | Finite selected-weekday requirements, copying days/weeks/ranges and reusing existing shift patterns implemented. No forever recurrence. |
| Templates | Existing saved shift patterns can be reused; a separate template record library / dedicated Templates manager is not implemented. Partial against the proposed screen/model. |
| Manual / personal / shift / full-roster SMS | Text generation, manual composer, single-person composer, selected-shift share and confirmed opt-in direct individual batches implemented. No automatic message on editing a schedule. Actual carrier transport is unverified. |
| Select one or more specific personnel for SMS | Single-person or all matched personnel supported; an arbitrary multi-person subset picker is not implemented. Partial. |
| Android Share / clipboard / TXT | Implemented. TXT uses readable UTF-8 with Windows line endings and grouping. Actual Samsung document-picker to USB to Windows Word print workflow still needs hardware acceptance. |
| Backup / restore | Authenticated encrypted local backup and validated atomic restore implemented; general ICS or arbitrary calendar-data import is not exposed. |
| S25 / Tab S7 and handwriting | API-compatible phone/tablet UI and authorized donor's native input pattern implemented. Actual S25/Tab S7, S Pen and installed Samsung IME offline acceptance remain unverified; no recognizer is bundled. |
| Approved icon and launch | Navy seahorse bitmap and launcher wiring implemented; home-screen pinning requires launcher approval. Physical Samsung launcher acceptance pending. |
| Reminders | Local reminders implemented with a 90-day scheduling horizon. Real hardware reboot, timing and permission-denial tests remain pending. |
| Production release | Debug-signed preview only. Protected stable signing, data-bearing upgrades/migration qualification, scale/performance and physical-device acceptance remain open. |

Inherited Fossify personal-calendar editors/widgets/ICS workflows remain intentionally unexposed in this dedicated shift workspace. This is not a claim of full Fossify feature parity. The original GPL license/notices remain included. Both donor repositories remain unchanged.

## Regression evidence and acceptance gates
The corrective commit adds 21 JUnit tests in `ReadmeAcceptanceTest.kt` to the previous 34: **55 expected JVM tests**. All 55 methods passed locally in a Kotlin/JVM assertion harness before submission; local harness success is not a substitute for the real CI JUnit results.

Four additional Android tests in `ReadmeAcceptanceUiTest.kt` exercise the actual entry UI, literal bullets, complete preview fields, explicit save and coverage, cancellation without writes, multiple errors without a Save button, and actual board filter controls. Together with the previous tests the device script requires **16 instrumented tests on each API33 and API35 emulator in airplane mode**. It fails rather than hiding failures or skips. No real SMS is sent by these tests.

The workflow for the corrective commit must establish compilation, JUnit and instrumentation success and verify the actual packaged APK. Record its accepted run, exact commit, artifact and SHA256 in the PR verification comment after completion. Physical Samsung acceptance and the open feature gaps above remain separate even if CI passes.
