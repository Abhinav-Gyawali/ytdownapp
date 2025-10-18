.PHONY: help setup build clean install

help:
	@echo "Android Development Commands:"
	@echo "  make setup    - Setup Android SDK and environment"
	@echo "  make build    - Build the Android app"
	@echo "  make clean    - Clean build files"
	@echo "  make install  - Install app on connected device"
	@echo "  make list-devices - List connected devices"

setup:
	@bash setup-android-sdk.sh

build:
	@./gradlew assembleDebug

clean:
	@./gradlew clean

install:
	@./gradlew installDebug

list-devices:
	@adb devices

run:
	@./gradlew installDebug && adb shell am start -n com.mvdown/.MainActivity