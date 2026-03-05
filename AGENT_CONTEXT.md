# SB-courselist Agent Context

Last updated: 2026-03-05
Workspace: `E:\MyCode\SB-courselist`

## Goal
- Deliver a Chinese-first Android timetable app with:
  - PDF import + course extraction
  - weekly timetable view (show only courses in selected week)
  - strong mobile usability and polished visual design
  - manual editing and playful easter-egg interactions
- Parser accuracy remains important, but current active track is UI/UX stabilization.

## Current State
- Build status: successful (`:app:compileDebugKotlin`, `:app:assembleDebug` pass).
- Install status: flaky due ADB connection instability; most recent attempts often return `no devices/emulators found`.
- Implemented timetable UX (active):
  - weekly-only display and week switching
  - auto current-week calculation from term start date
  - highlight current weekday when viewing current week
  - merged contiguous periods into one course card
  - mobile compact mode with responsive equal-width weekday columns
  - period-time labels on left axis
  - manual add/edit/delete course in-app
  - delete behavior scoped to current week entry
  - long-press flip card easter-egg actions:
    - `翘了` marks current week as skipped
    - `我爱学习` restores from skipped
  - skipped-state stamp overlay
  - easter-egg achievement toast/banner text effects
  - empty-week page alternating custom messages
- Branding:
  - app name switched to `逃了么`
  - custom launcher icon pipeline already integrated (logo asset-based)

## Latest Completed Work
1. Hero card and top header polish
   - title updated to `选择性出勤协议`
   - subtle hero-card background ornaments added
   - file: `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`

2. Full-week skipped easter egg
   - when all visible courses in selected week are skipped, show:
   - `666,盐都不盐了`
   - file: `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`

3. Skipped stamp + flip button refinements
   - compact button vertical text support for `翘了` and `我爱学习`
   - stamp made smaller, translucent gray, slanted, less intrusive
   - file: `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`

4. Edit dialog layout consistency
   - equalized `星期/开始节/结束节` fields with `Row + weight(1f)`
   - applied in timetable edit dialog and import-flow edit dialog
   - files:
     - `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`
     - `app/src/main/java/com/sb/courselist/ui/screen/ImportScreen.kt`

5. Card typography refresh (latest)
   - card title mapped to clearer cartoon style (`zcool_kuaile`)
   - card meta mapped to `zcool_xiaowei`
   - 3-period compact cards now follow centered alignment rule (`span <= 3`)
   - files:
     - `app/src/main/java/com/sb/courselist/ui/theme/Type.kt`
     - `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`

## Known Blockers / Risks
- ADB frequently disconnects; installation cannot always be completed immediately.
- User currently validating font legibility on real device; may require one more font pass after live check.
- Legacy parser issue (weekend duplication concern) remains tracked but currently deprioritized.

## Important Files (Current UI Track)
- Timetable screen:
  - `app/src/main/java/com/sb/courselist/ui/screen/TimetableScreen.kt`
- Import screen and import-edit dialog:
  - `app/src/main/java/com/sb/courselist/ui/screen/ImportScreen.kt`
- Typography:
  - `app/src/main/java/com/sb/courselist/ui/theme/Type.kt`
- App icon/name related Android config:
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/mipmap-*` and icon source assets in workspace

## Build and Install (CMD-safe)
```bat
cd E:\MyCode\SB-courselist
.\gradlew.bat :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
"E:\Android\Sdk\platform-tools\adb.exe" devices
"E:\Android\Sdk\platform-tools\adb.exe" install -r "E:\MyCode\SB-courselist\app\build\outputs\apk\debug\app-debug.apk"
"E:\Android\Sdk\platform-tools\adb.exe" shell dumpsys package com.sb.courselist | findstr "versionCode versionName lastUpdateTime"
```

## Next Priority
1. Reinstall on a connected device and confirm latest font + card alignment behavior visually.
2. If legibility is still weak, only tweak card title font family/weight, keep layout unchanged.
3. Revisit parser weekend-duplication diagnosis after UI acceptance.

## Restore Context
- Next agent prompt:
  - `Read E:\MyCode\SB-courselist\AGENT_CONTEXT.md, then continue from latest UI validation (font readability + compact card alignment) and install to device if connected.`
