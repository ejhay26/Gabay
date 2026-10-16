# 04 — PHASED EXECUTION STATE MACHINE & VISUAL QA PROTOCOL

You are strictly forbidden from coding multiple phases at once or skipping verification steps. You must execute the **6-Step Phase Gate Loop** below for each single phase in `05_TASK_TRACKER.md`.

---

## 1. THE 6-STEP PHASE GATE LOOP

For the current active Phase `N`:

### STEP 1 — READ & SCOPE
- Read `05_TASK_TRACKER.md` to identify the exact deliverables for Phase `N`.
- Cross-check all architectural and UI constraints in `01_ANTI_SLOP_AND_UI_DESIGN.md` and `02_ARCHITECTURE_AND_STACK.md`.

### STEP 2 — IMPLEMENT
- Write complete, production-grade code for Phase `N` only.
- **Zero Stubs Rule:** Never write `todo!()`, `unimplemented!()`, dummy return values, or mock timers pretending to be real engines.

### STEP 3 — COMPILE & UNIT TEST GATE
- Run host Rust checks:
  ~~~bash
  cargo test --workspace
  ~~~
- Run Android compilation (for Phase 3+):
  ~~~bash
  cargo ndk -t arm64-v8a -o android_app/app/src/main/jniLibs build --release -p rust_core
  cd android_app && ./gradlew assembleDebug
  ~~~
- If any compiler error, linker failure, or test failure occurs, fix it immediately before advancing to Step 4.

### STEP 4 — ADB DEPLOY & RUNTIME LOGCAT GATE
- Install and launch the debug build on the connected Android device/emulator:
  ~~~bash
  adb install -r android_app/app/build/outputs/apk/debug/app-debug.apk
  adb logcat -c
  adb shell am start -n dev.gabay.app/.MainActivity
  sleep 4
  adb logcat -d | grep -iE "AndroidRuntime|Fatal|UnsatisfiedLinkError|Panic|gabay" || true
  ~~~
- If any crash, Rust panic, `UnsatisfiedLinkError`, or permission error appears in `logcat`, **LOOP BACK TO STEP 2** and fix the root cause.

### STEP 5 — AUTOMATED UI GEOMETRY & MULTIMODAL SCREENSHOT INSPECTION
- Mandatory for all UI phases and visual updates. Execute both **Check A (XML Bounds)** and **Check B (Live Screenshot Inspection)**.
- **Multimodal Visual Inspection:** Do not blindly dump screenshot files. You MUST inspect the captured `.png` image using visual tools to verify real map rendering, contrast, and layout alignment.
- Copy verified screenshots to `docs/screenshots/` and update `README.md` and `docs/` references.
- If the layout fails any check in Section 2, **LOOP BACK TO STEP 2** and refine the UI/MapLibre code.

### STEP 6 — COMMIT & PUSH TO REMOTE
- Mark `[x]` on the completed tasks inside `05_TASK_TRACKER.md` with measured performance metrics.
- **Immediate Git Push Gate:** Stage all completed changes, commit with a concise message, and immediately run:
  ~~~bash
  git add .
  git commit -m "feat(phase-N): <description of deliverable or fix>"
  git push origin main
  ~~~
- Only after the commit is pushed to remote and `05_TASK_TRACKER.md` is updated may you advance to Phase `N + 1`.

---

## 2. DUAL VISUAL VERIFICATION PROTOCOL (HOW TO INSPECT THE LIVE UI)

Never assume Jetpack Compose or MapLibre code looks right just because it compiled. You must inspect the live rendered output on the device using both structural XML bounds and visual screenshots:

### Check A: Structural XML Hierarchy Audit (`uiautomator`)
Run the following commands to dump the exact screen coordinates of every visible UI element:

~~~bash
mkdir -p .agent_qa
adb shell uiautomator dump /sdcard/window_dump.xml
adb pull /sdcard/window_dump.xml .agent_qa/phase_${PHASE}_layout.xml
~~~

Inspect `.agent_qa/phase_${PHASE}_layout.xml` and verify:
1. **Zero Overlapping Controls:** Confirm the bounding boxes `[x1,y1][x2,y2]` of the Top Maneuver Banner, Mode Selector Strip, and Bottom Cockpit Dock do not overlap each other.
2. **System Inset Clearance:** Confirm the top banner `y1` sits below the Android status bar / camera cutout, and the bottom dock `y2` sits above the gesture navigation bar.
3. **Glove-Friendly Touch Targets:** Confirm every interactive button has a minimum height `(y2 - y1)` corresponding to `>= 48dp`.
4. **Anti-Slop Copy Check:** Confirm zero emojis and zero placeholder text exist in any `text="..."` or `content-desc="..."` attribute.

### Check B: Live Visual Screenshot Audit (`screencap`)
Capture the actual device frame buffer:

~~~bash
adb exec-out screencap -p > .agent_qa/phase_${PHASE}_screen.png
~~~

Open `.agent_qa/phase_${PHASE}_screen.png` using your multimodal vision/image reader tool and grade it against this strict **5-Point Production Visual Rubric**:

1. **MapLibre Canvas Integrity:**
   - Does the map background clearly show rendered vector roads, building footprints/3D extrusions, and labels from `/sdcard/Documents/Gabay/philippines_vector.pmtiles`?
   - **AUTOMATIC FAIL:** If the map area is solid black, solid gray, beige, or shows missing tile grids, the PMTiles file path or MapLibre style JSON is broken. Fix it before proceeding.
2. **Tactical Cockpit Aesthetic (Anti-Slop Compliance):**
   - Are all HUD panels rendered in solid `#0A0D10` / `#12161C` obsidian dark with crisp `1.dp` `#262E38` hairline borders and sharp `4.dp–6.dp` corners?
   - **AUTOMATIC FAIL:** Any default Material 3 purple/pastel backgrounds, rounded pill cards (`24.dp+`), gradients, or floating circular action buttons.
3. **3-Column Telemetry Dock Alignment:**
   - Are Speed (`KM/H`), Remaining Distance (`KM`), Rolling Pace (`~XX-YY MIN`), and the `[GABAY VOICE]` trigger cleanly aligned in the bottom dock with zero line-wrapping or clipped digits?
4. **Route & Pin Contrast:**
   - When a pin or route is active, is the `#10B981` emerald route line clearly visible with a dark casing border over both default vector mode and hybrid satellite mode?
5. **Visual Hierarchy & Proportions:**
   - Does the MapLibre canvas remain the dominant visual element (occupying > 70% of the unobstructed screen area) while the top and bottom HUD bars sit compactly at the edges?

---

## 3. SELF-HEALING & ANTI-RATIONALIZATION MANDATE

- You are strictly forbidden from excusing a broken screenshot with phrases like *"The map tiles will load once the user configures them"* or *"The layout is basic for now."*
- If `.agent_qa/phase_${PHASE}_screen.png` fails ANY item in the 5-Point Production Visual Rubric, you must immediately diagnose the bug, edit the source files, recompile, redeploy via ADB, and capture a new screenshot until all 5 points pass.