from pathlib import Path

workflow = """name: Build Android APK

on:
push:
branches: [ main ]
workflow_dispatch:

jobs:
build:
runs-on: ubuntu-latest
steps:
- name: Checkout Code
uses: actions/checkout@v4

  - name: Set up Java 17
    uses: actions/setup-java@v4
    with:
      java-version: '17'
      distribution: 'temurin'

  - name: Set up Android SDK
    uses: android-actions/setup-android@v3

  - name: Install Android SDK components
    run: |
      sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

  - name: Set up Gradle
    uses: gradle/actions/setup-gradle@v3

  - name: Ensure Gradle wrapper is executable
    run: chmod +x gradlew

  - name: Build Debug APK
    run: ./gradlew :composeApp:assembleDebug --stacktrace

  - name: Upload APK Artifact
    uses: actions/upload-artifact@v4
    with:
      name: school-management-debug-apk
      path: composeApp/build/outputs/apk/debug/*.apk
      if-no-files-found: error
      retention-days: 7

"""

path = Path(".github/workflows/build-android.yml")
path.write_text(workflow, encoding="utf-8")
print("Workflow rewritten successfully.")
