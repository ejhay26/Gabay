#!/usr/bin/env python3
"""Packages Gabay's compiled Rust cdylib into a signed Android APK.
Requires Android SDK build-tools (aapt2, zipalign, apksigner).
"""
import os
import shutil
import subprocess
import sys
import zipfile

def run_cmd(cmd, check=True):
    print("Running:", " ".join(cmd))
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0 and check:
        print("STDOUT:", res.stdout)
        print("STDERR:", res.stderr)
        raise RuntimeError(f"Command failed with exit code {res.returncode}")
    return res

def main():
    root = r"C:\Users\Administrator\Documents\git\Gabay"
    build_tools = r"C:\Android\android-sdk\build-tools\34.0.0"
    android_jar = r"C:\Android\android-sdk\platforms\android-35\android.jar"
    debug_ks = r"C:\Users\Administrator\.android\debug.keystore"
    
    so_path = r"G:\gabay_builds\ui_target\aarch64-linux-android\release\libgabay_app.so"
    if not os.path.exists(so_path):
        so_path = r"G:\gabay_builds\ui_target\aarch64-linux-android\debug\libgabay_app.so"
    if not os.path.exists(so_path):
        sys.exit(f"Native library not found at {so_path}")

    print(f"Using native library: {so_path} ({os.path.getsize(so_path) / 1024 / 1024:.2f} MB)")

    out_dir = r"G:\gabay_builds\apk_staging"
    os.makedirs(out_dir, exist_ok=True)

    manifest_path = os.path.join(root, "android", "AndroidManifest.xml")
    res_dir = os.path.join(root, "android", "res")

    # 1. Compile resources with aapt2
    compiled_res = os.path.join(out_dir, "resources.zip")
    if os.path.exists(compiled_res):
        os.remove(compiled_res)
    
    aapt2 = os.path.join(build_tools, "aapt2.exe")
    run_cmd([aapt2, "compile", "--dir", res_dir, "-o", compiled_res])

    # 2. Link APK
    unaligned_apk = os.path.join(out_dir, "unaligned.apk")
    if os.path.exists(unaligned_apk):
        os.remove(unaligned_apk)

    link_cmd = [
        aapt2, "link",
        "-I", android_jar,
        "--manifest", manifest_path,
        "-o", unaligned_apk,
        compiled_res,
        "--auto-add-overlay"
    ]
    run_cmd(link_cmd)

    # 3. Add libgabay_app.so into lib/arm64-v8a/
    print("Adding native shared library into APK...")
    with zipfile.ZipFile(unaligned_apk, "a", compression=zipfile.ZIP_DEFLATED) as zf:
        zf.write(so_path, "lib/arm64-v8a/libgabay_app.so")

    # 4. zipalign 4
    zipalign = os.path.join(build_tools, "zipalign.exe")
    aligned_apk = os.path.join(out_dir, "aligned.apk")
    if os.path.exists(aligned_apk):
        os.remove(aligned_apk)

    run_cmd([zipalign, "-p", "-f", "4", unaligned_apk, aligned_apk])

    # 5. Sign with apksigner
    final_apk = r"G:\gabay_builds\gabay-debug.apk"
    if os.path.exists(final_apk):
        os.remove(final_apk)

    apksigner = os.path.join(build_tools, "apksigner.bat")
    run_cmd([
        apksigner, "sign",
        "--ks", debug_ks,
        "--ks-pass", "pass:android",
        "--key-pass", "pass:android",
        "--out", final_apk,
        aligned_apk
    ])

    print("Verifying APK signature...")
    run_cmd([apksigner, "verify", final_apk])

    size_mb = os.path.getsize(final_apk) / 1024 / 1024
    print(f"\n[SUCCESS] Produced signed Android APK at {final_apk} ({size_mb:.2f} MB)")

if __name__ == "__main__":
    main()
