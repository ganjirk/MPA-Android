#!/usr/bin/env bash

# MPA Android - Build, test, install and launch
# Repository: D:/MPA-Android

set -Eeuo pipefail

# Java configuration
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Android/Android Studio/jbr}"

# Android SDK configuration
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/c/Users/LENOVO/AppData/Local/Android/Sdk}"
export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"

# Make Java and Android tools available in this shell
export PATH="$JAVA_HOME/bin:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$PATH"

cd /d/MPA-Android

echo "========================================"
echo " MPA Android - Automated Build"
echo "========================================"

# 1. Check required tools
for tool in java adb; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "ERROR: Required tool '$tool' was not found in PATH."
        exit 1
    fi
done

if [[ ! -f "./gradlew" ]]; then
    echo "ERROR: Gradle wrapper not found."
    exit 1
fi

if [[ ! -f "app/build.gradle.kts" ]]; then
    echo "ERROR: Android app Gradle configuration not found."
    exit 1
fi

echo
echo "Java version:"
java -version

echo
echo "ADB version:"
adb version

echo
echo "Gradle version:"
./gradlew --version

# 2. Build the APK
echo
echo "Cleaning and building the application..."
./gradlew clean assembleDebug

# 3. Run automated tests
echo
echo "Running Gradle tests..."
./gradlew test

APK="app/build/outputs/apk/debug/app-debug.apk"

if [[ ! -f "$APK" ]]; then
    echo "ERROR: APK was not created: $APK"
    exit 1
fi

echo
echo "APK created:"
ls -lh "$APK"

# 4. Check the emulator
echo
echo "Checking connected Android devices..."
adb devices

if ! adb get-state >/dev/null 2>&1; then
    echo
    echo "Build and tests succeeded, but ADB has no active device."
    echo "Start the emulator and run this script again."
    exit 0
fi

# Ensure the device has completed booting
BOOT_STATUS="$(adb shell getprop sys.boot_completed | tr -d '\r')"

if [[ "$BOOT_STATUS" != "1" ]]; then
    echo "ERROR: The emulator has not finished booting."
    exit 1
fi

# 5. Install with a timeout
echo
echo "Installing the application (120-second timeout)..."

if ! timeout 120s adb install -r "$APK"; then
    echo
    echo "ERROR: APK installation failed or timed out."
    echo "APK: $APK"
    echo
    echo "Check ADB with:"
    echo "  adb devices"
    echo "  adb shell getprop sys.boot_completed"
    echo "  adb logcat -d -t 300"
    exit 1
fi

# 6. Launch only after a successful installation
echo
echo "Launching MPA Android..."
adb shell am force-stop com.grk.mpa.app
adb shell monkey -p com.grk.mpa.app 1

echo
echo "SUCCESS: Build, tests, installation and launch completed."
echo
echo "Finished."
