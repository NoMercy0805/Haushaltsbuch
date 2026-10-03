# Native beta: HTML parity status

The reference is `app/src/main/assets/index.html`. This native beta is **not yet accepted as 1:1 identical**. Previous audits and a successful APK build are not proof of screen, state or performance parity.

Work stays on `native-compose`. `main` and `backup/webview-2026-10-02` must remain unchanged. Bundle changes and tests into one final build; do not push and build after each individual correction.

## Current corrections

- Household income/expense totals in the header, including the empty 50/50 state and the HTML's 20% label threshold.
- Widget and ongoing notification balances stop at the end of the real current month rather than including future bookings.
- Recurring generation refreshes the screen and persistence; new entries switch to their own month.
- Native legacy `income`/`expense` entry types are normalized to HTML-compatible `einnahme`/`ausgabe`.
- Hidden titles are read as strings; suggestions use newest bookings first and do not re-sort the complete database on each keystroke.
- Beta entry/edit forms use the reference panel colors, input borders, date selection, cent formatting, recurrence period selection and close controls.
- CSV and legacy JSON restoration preserve valid data/settings, with the replacement confirmation from the HTML flow. CSV loan defaults match the reference.
- Tab, gear, sub-navigation, settings, recurrence and filter icons use SVG geometry from the reference rather than approximations.
- Dark mode colors, day headers, row separators, navigation spacing and the repayment-plan FAB state were corrected.

## Performance changes

- No full JSON stringification or stringify/parse clone during ordinary recomposition. A repository revision invalidates derived screen data.
- Month totals/grouping are cached until data, month or filter changes.
- Beta day headers and individual bookings are separate lazy items, including imports with many bookings on the same date.
- Recurring generation indexes dates once, using sets only for actual template IDs. It no longer scans the entire database separately for each template.
- Initial database load, file reads, CSV parsing, restoration and export IO run off the UI thread.
- SQLite writes are ordered on one background executor. Debounced automatic backups use a separate executor, so a slow document/cloud provider cannot block SQLite writes.
- Settings are cached consistently across repository instances. Currency formatters are reused per thread.

## Verification

`./gradlew testReleaseUnitTest assembleRelease` runs the regression tests and builds the APK in one workflow. The tests include 50,000 bookings and 200 recurring templates; this is a JVM data-processing check, not a device FPS or UI latency measurement.

Reference JavaScript was executed directly for recurrence periods, month-end clipping, cent/milli formatting, amount parsing, loan defaults, recurrence end dates and idempotence.

## Remaining acceptance work

- Compare actual Android screenshots and interaction flows against the HTML for all reachable screens and dialogs, with identical fixtures and viewport/font settings.
- Exercise the complete user import on Android: initial load, scrolling, input, editing, app restart, backup/restore and widget/notification updates.
- Complete the comparison of other add/edit forms, expanded histories, animations, gradients/glass effects and keyboard states. These have not been visually accepted.
- Measure real device performance after the complete import. No quantified device speedup is claimed yet.

An Android runtime/emulator is not available in the current execution environment. Passing unit tests and an APK build do not close these remaining acceptance items.
