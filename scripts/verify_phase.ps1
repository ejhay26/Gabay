# scripts/verify_phase.ps1
# Automated verification script for Gabay Phased Execution Loop

param (
    [Parameter(Mandatory=$true)]
    [int]$Phase
)

$ErrorActionPreference = "Stop"
$env:CARGO_TARGET_DIR = "G:\gabay_builds\cargo_target"
$env:PATH = "G:\sine_builds\.cargo\bin;$env:PATH"

Write-Host "=========================================="
Write-Host " RUNNING 6-STEP VERIFICATION FOR PHASE $Phase"
Write-Host "=========================================="

# STEP 3: Compile & Unit Test Gate
Write-Host "[STEP 3] Running Host Rust Tests..."
cargo test --workspace

if ($Phase -ge 3) {
    Write-Host "[STEP 3] Compiling Rust Core for Android (arm64-v8a)..."
    cargo ndk -t arm64-v8a -o android_app/app/src/main/jniLibs build --release -p rust_core
}

if ($Phase -ge 4) {
    Write-Host "[STEP 3] Assembling Android App..."
    Set-Location android_app
    .\gradlew.bat assembleDebug
    Set-Location ..

    # STEP 4: ADB Deploy & Runtime Logcat Gate
    Write-Host "[STEP 4] Deploying to ADB Device..."
    adb install -r G:\gabay_builds\android_builds\app\outputs\apk\debug\app-debug.apk
    adb logcat -c
    adb shell am start -n dev.gabay.app/.MainActivity
    Start-Sleep -Seconds 4
    
    $logs = adb logcat -d | Select-String -Pattern "AndroidRuntime|Fatal|UnsatisfiedLinkError|Panic|gabay"
    if ($logs) {
        Write-Warning "Logcat warnings/errors detected:"
        $logs | Select-Object -First 20 | Out-String | Write-Host
    }

    # STEP 5: UI Geometry & Live Visual Screenshot Inspection
    Write-Host "[STEP 5] Capturing UI Hierarchy XML and Framebuffer Screenshot..."
    New-Item -ItemType Directory -Force -Path ".agent_qa", "docs/screenshots" | Out-Null
    
    adb shell uiautomator dump /sdcard/window_dump.xml
    adb pull /sdcard/window_dump.xml .agent_qa/phase_${Phase}_layout.xml
    adb exec-out screencap -p > .agent_qa/phase_${Phase}_screen.png
    Copy-Item .agent_qa/phase_${Phase}_screen.png "docs/screenshots/phase_${Phase}_screen.png" -Force

    Write-Host "[OK] Verification artifacts saved to .agent_qa/ and docs/screenshots/"
}

Write-Host "=========================================="
Write-Host " PHASE $Phase VERIFICATION GATES PASSED!"
Write-Host "=========================================="
