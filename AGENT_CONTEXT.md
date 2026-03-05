# SB-courselist Agent Context

Last updated: 2026-03-04 23:51
Workspace: `E:\MyCode\SB-courselist`

## Goal
- Build an Android timetable app that imports PDF and extracts:
  - course name
  - room/location
  - teacher
  - day/period/weeks
- Priority: extraction accuracy first.

## Current State
- App builds and installs.
- Parser stack:
  - `PdfTextParser` (PDF text coordinates)
  - `TemplateRuleEngine` (rule extraction)
  - `PdfOcrParser` fallback
- Connected device installs succeeded with:
  - `versionCode = 2`
  - `versionName = 1.0.1`

## Latest Completed Work
1. Parser upgraded to `cn-campus-v2` in
   - `app/src/main/java/com/sb/courselist/parser/TemplateRuleEngine.kt`
2. Better text token chunking in
   - `app/src/main/java/com/sb/courselist/parser/PdfTextParser.kt`
3. Name cleanup improvements:
   - balances broken brackets like `...（慕课` -> `...（慕课）`
4. Added parser unit tests in
   - `app/src/test/java/com/sb/courselist/parser/TemplateRuleEngineTest.kt`
5. Offline parser tool maintained in
   - `scripts/parse_timetable_pdf.py`
   - exports JSON/CSV in `parse_outputs_final/`

## User-Reported Problem Under Investigation
- User reported: weekend courses look duplicated with Thu/Fri.
- User confirmed exported `json/csv` also looks wrong.
- Current diagnosis status:
  - at least part of weekend entries are present in source weekend columns,
  - but behavior still needs stricter validation and potential dedup policy.

## Important Files
- Parser core:
  - `app/src/main/java/com/sb/courselist/parser/TemplateRuleEngine.kt`
  - `app/src/main/java/com/sb/courselist/parser/PdfTextParser.kt`
- Parser tests:
  - `app/src/test/java/com/sb/courselist/parser/TemplateRuleEngineTest.kt`
- Offline verification:
  - `scripts/parse_timetable_pdf.py`
  - `parse_outputs_final/*.parsed.json`
  - `parse_outputs_final/*.parsed.csv`
- Build config:
  - `app/build.gradle.kts`

## Build and Install (CMD-safe)
```bat
cd E:\MyCode\SB-courselist
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
"E:\Android\Sdk\platform-tools\adb.exe" install -r "E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk"
"E:\Android\Sdk\platform-tools\adb.exe" shell dumpsys package com.sb.courselist | findstr "versionCode versionName lastUpdateTime"
```

## Next Priority
1. Reproduce and isolate day-column mis-assignment for weekend duplication claim.
2. Add deterministic policy switch if needed:
   - `strict_raw_mode`: keep all parsed entries
   - `merged_week_mode`: merge same `name+teacher+room+period` across days when week ranges imply split schedule.

## Restore Context
- Next agent prompt:
  - `Read E:\MyCode\SB-courselist\AGENT_CONTEXT.md, then continue weekend-duplicate parser diagnosis.`
