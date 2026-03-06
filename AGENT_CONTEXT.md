# SB-courselist Agent Context

Last updated: 2026-03-06
Workspace: `E:\MyCode\SB-courselist`

## Goal
- Deliver a Chinese-first Android timetable app with:
  - PDF import + course extraction
  - weekly timetable view (show only courses in selected week)
  - strong mobile usability and polished visual design
  - manual editing and playful easter-egg interactions
- Keep parser robustness high across different timetable PDF layouts.

## Current State
- Build status: successful.
  - `:app:compileDebugKotlin` pass
  - `:app:assembleDebug` pass
  - `:app:testDebugUnitTest --tests com.sb.courselist.parser.TemplateRuleEngineTest` pass (9 tests, 0 failures)
- Install status: currently no connected ADB device (`adb devices` empty).
- Version state:
  - `versionName = "2.0"`
  - `versionCode = 3`
  - latest debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Parser state:
  - Fixed a real regression on headerless continuation pages (Yangxiao PDF):
    - previously dropped top-of-page detail line due to inherited `headerBottom` filtering
    - now keeps continuation-page top courses when early period-detail line is detected
- Timetable UI state:
  - compact mode card title alignment updated so 4-period cards are centered like 2/3-period cards (`span <= 4`).

## Latest Completed Work
1. Parser fix for missing second `自然语言处理` entry in `杨逍(2025-2026-2)课表.pdf`
   - root cause: page 2 lacks weekday anchors, but inherited page-1 header cutoff removed the top course block
   - file: `app/src/main/java/com/sb/courselist/parser/TemplateRuleEngine.kt`
2. Added regression unit test for anchorless continuation page top-course retention
   - file: `app/src/test/java/com/sb/courselist/parser/TemplateRuleEngineTest.kt`
3. Updated app version to `2.0` for packaging
   - file: `app/build.gradle.kts`
4. Updated compact card alignment rule (`span <= 3` -> `span <= 4`)
   - file: `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`
5. Security review summary completed (manifest + code path audit)
   - no dangerous runtime permissions requested
   - merged manifest includes `INTERNET` and `ACCESS_NETWORK_STATE` from transitive libs

## Known Risks / Notes
- ADB connectivity remains unstable/intermittent; install verification depends on device availability.
- `OpenDocument` + `takePersistableUriPermission` keeps read permission for selected files until explicitly released.
- Local Room DB is not encrypted (stores parsed course fields including `rawText`).
- Merged manifest includes network permission due to ML Kit/play-services transitive components.

## Important Files (Active Track)
- Parser core:
  - `app/src/main/java/com/sb/courselist/parser/TemplateRuleEngine.kt`
  - `app/src/test/java/com/sb/courselist/parser/TemplateRuleEngineTest.kt`
- UI timetable card rendering:
  - `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`
- Import flow:
  - `app/src/main/java/com/sb/courselist/ui/screen/ImportScreen.kt`
- Build/version config:
  - `app/build.gradle.kts`
- Manifest (source + merged outputs):
  - `app/src/main/AndroidManifest.xml`
  - `app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml`

## Build, Test, Install
```bat
cd E:\MyCode\SB-courselist
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:testDebugUnitTest --tests com.sb.courselist.parser.TemplateRuleEngineTest --console=plain
.\gradlew.bat :app:assembleDebug
"E:\Android\Sdk\platform-tools\adb.exe" devices
"E:\Android\Sdk\platform-tools\adb.exe" install -r "E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk"
"E:\Android\Sdk\platform-tools\adb.exe" shell dumpsys package com.sb.courselist | findstr "versionCode versionName lastUpdateTime"
```

## Next Priority
1. Install `2.0` APK on a connected device and verify:
   - Yangxiao PDF now imports both NLP time blocks
   - 4-period card title alignment appears centered in compact mode
2. Decide whether to do security hardening pass:
   - remove unnecessary network reachability where possible
   - release persisted URI permission after import if no longer needed
   - consider DB encryption if threat model requires it
3. If parser acceptance passes, continue UI polish only.

## Restore Context
- Suggested next prompt:
  - `Read E:\MyCode\SB-courselist\AGENT_CONTEXT.md, install the 2.0 debug APK if device is connected, then verify Yangxiao PDF import and 4-period card alignment on device.`
