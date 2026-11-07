@echo off
REM Gabay UI build environment: keeps every cache and build output on G:
if not exist G:\ (
    echo [ERROR] Drive G: is not connected.
    exit /b 1
)
set CARGO_HOME=G:\sine_builds\.cargo
set CARGO_TARGET_DIR=G:\gabay_builds\ui_target
set GRADLE_USER_HOME=G:\sine_builds\.gradle
set TEMP=G:\gabay_builds\temp
set TMP=G:\gabay_builds\temp
if not exist G:\gabay_builds\temp mkdir G:\gabay_builds\temp
REM Uncomment and adjust if rustup toolchains also live on G:
REM set RUSTUP_HOME=G:\rustup
set ANDROID_HOME=C:\Android\android-sdk
set ANDROID_NDK_HOME=C:\Android\android-sdk\ndk\28.2.13676358
set NDK_BIN=%ANDROID_NDK_HOME%\toolchains\llvm\prebuilt\windows-x86_64\bin
set CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=%NDK_BIN%\aarch64-linux-android26-clang.cmd
set CC_aarch64_linux_android=%NDK_BIN%\aarch64-linux-android26-clang.cmd
set CXX_aarch64_linux_android=%NDK_BIN%\aarch64-linux-android26-clang++.cmd
set AR_aarch64_linux_android=%NDK_BIN%\llvm-ar.exe
echo [OK] Gabay UI environment set. Target dir: %CARGO_TARGET_DIR%
