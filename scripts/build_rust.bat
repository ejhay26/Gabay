@echo off
if not exist G:\ (
    echo [ERROR] Drive G: is not connected! Please connect your G: drive to ensure builds do not consume C: drive space.
    exit /b 1
)
set CARGO_HOME=G:\sine_builds\.cargo
set CARGO_TARGET_DIR=G:\cargo_target
set TEMP=G:\gabay_builds\temp
set TMP=G:\gabay_builds\temp
if not exist G:\gabay_builds\temp mkdir G:\gabay_builds\temp
set NDK_BIN=C:\Android\android-sdk\ndk\28.2.13676358\toolchains\llvm\prebuilt\windows-x86_64\bin
set CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER=%NDK_BIN%\aarch64-linux-android26-clang.cmd
set CC_aarch64_linux_android=%NDK_BIN%\aarch64-linux-android26-clang.cmd
set CXX_aarch64_linux_android=%NDK_BIN%\aarch64-linux-android26-clang++.cmd
set AR_aarch64_linux_android=%NDK_BIN%\llvm-ar.exe
cd /d C:\Users\Administrator\Documents\git\Gabay\rust_core
cargo build --target aarch64-linux-android --release
if %ERRORLEVEL% EQU 0 (
    echo [OK] Copying libgabay_core.so to jniLibs...
    copy /Y "G:\cargo_target\aarch64-linux-android\release\libgabay_core.so" "C:\Users\Administrator\Documents\git\Gabay\android_app\app\src\main\jniLibs\arm64-v8a\libgabay_core.so"
)
