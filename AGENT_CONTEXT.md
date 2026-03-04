# SB-courselist Agent Context

Last updated: 2026-03-04
Workspace: `E:\MyCode\SB-courselist`

## Goal
- Android app that imports a timetable PDF and generates a semester schedule.
- UI style: cartoon + fresh.
- Required fields per course card: name, room, teacher.

## Locked Decisions
- Stack: native Android (Kotlin + Compose).
- Parsing: text-first, OCR fallback.
- Scope v1: high-accuracy for current template family first.
- Navigation: two tabs (`Timetable`, `Import`).
- Editing: manual correction required.

## Implemented
- Full Android project scaffold with Compose + Room + parser pipeline.
- Parsers:
  - `PdfTextParser` (PDFBox tokens with coordinates)
  - `TemplateRuleEngine` (grid mapping and field extraction)
  - `PdfOcrParser` (ML Kit Chinese OCR fallback)
  - `CompositeScheduleParser` (text then OCR)
- Data layer:
  - Room entities/DAO/repository complete
- UI:
  - Import screen with parse state + editable preview + save
  - Timetable weekly grid
  - Direct edit for persisted courses from timetable cells
- Parse robustness upgrade for Xiaomi sample PDF:
  - Extended weekday recognition (`周一/星期一/礼拜一/Mon...`)
  - Synthetic weekday column anchors when headers are not detected
  - Failure now auto-enters editable manual fallback preview (no dead-end error)
- Added parser diagnostics logs (`ScheduleParser` tag) for on-device troubleshooting
- Build/network hardening:
  - Added Aliyun Maven mirrors in `settings.gradle.kts`
  - Added IPv4 preference in `gradle.properties`
  - Added SDK path setup script `scripts/setup_android_sdk.ps1`

## Build/Test Status (Current)
- `:app:assembleDebug` passed.
- `:app:testDebugUnitTest` passed.
- APK generated:
  - `E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk`

## Android SDK (Current machine)
- SDK root: `E:\Android\Sdk`
- Installed offline:
  - `platform-tools`
  - `platforms\android-35`
  - `build-tools\34.0.0`
  - `build-tools\35.0.0`

## Important Files
- Entry:
  - `app/src/main/java/com/sb/courselist/MainActivity.kt`
  - `app/src/main/java/com/sb/courselist/CourseListApplication.kt`
- Parser:
  - `app/src/main/java/com/sb/courselist/parser/TemplateRuleEngine.kt`
  - `app/src/main/java/com/sb/courselist/parser/CompositeScheduleParser.kt`
  - `app/src/main/java/com/sb/courselist/parser/PdfTextParser.kt`
  - `app/src/main/java/com/sb/courselist/parser/PdfOcrParser.kt`
- UI:
  - `app/src/main/java/com/sb/courselist/ui/CourseListApp.kt`
  - `app/src/main/java/com/sb/courselist/ui/screen/ImportScreen.kt`
  - `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`

## Resume Commands
1. Build debug APK:
```powershell
cd E:\MyCode\SB-courselist
.\gradle-8.7-bin\gradle-8.7\bin\gradle.bat :app:assembleDebug --no-daemon --console=plain
```

2. Run unit tests:
```powershell
.\gradle-8.7-bin\gradle-8.7\bin\gradle.bat :app:testDebugUnitTest --no-daemon --console=plain
```

3. Install to device (USB debugging enabled):
```powershell
E:\Android\Sdk\platform-tools\adb.exe devices
E:\Android\Sdk\platform-tools\adb.exe install -r E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk
```

## Next Work
1. Device validation:
  - import real PDF
  - verify parse accuracy
  - verify edit and persistence flow
2. Parser tuning against your real template data.
3. Optional localization pass (current UI text is mainly English to avoid encoding corruption).

## Restore Context
- Prompt next agent with:
  - `Read E:\MyCode\SB-courselist\AGENT_CONTEXT.md first, then continue implementation.`
