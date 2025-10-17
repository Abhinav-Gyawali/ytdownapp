#!/bin/bash
set -e

# Define SDK root
ANDROID_SDK_ROOT="/opt/android-sdk"
mkdir -p "$ANDROID_SDK_ROOT"

# Download Android Command Line Tools
cd /tmp
CMDLINE_VERSION="11076708_latest"  # Update version as needed
SDK_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_VERSION}.zip"

echo "Downloading Android command line tools..."
curl -O $SDK_URL
unzip commandlinetools-linux-${CMDLINE_VERSION}.zip
mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools
mv cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest

# Add SDK tools to PATH
export PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:$PATH"

# Install SDK components
yes | sdkmanager --licenses

sdkmanager "platform-tools" \
           "platforms;android-34" \
                      "build-tools;34.0.0" \
                                 "ndk;25.2.9519653" \
                                            "cmake;3.22.1"

                                            # Set permissions
                                            chown -R vscode:vscode "$ANDROID_SDK_ROOT"
                                            