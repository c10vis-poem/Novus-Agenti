Source: Drive file ID `1mR5NYnVkdCKCkwozyjPm4nsXQ_sEu_ZQ` — a Google Developer Library article/README page (dev-library tutorial) documenting the `amirisback/automated-build-android-app-with-github-action` GitHub repository.

# Automated Build Android Using GitHub Action

**Repository:** [amirisback/automated-build-android-app-with-github-action](https://devlibrary.withgoogle.com/products/android/repos/amirisback-automated-build-android-app-with-github-action) (Google Dev Library — Android)
**Author:** Faisal Amir (Muhammad Faisal Amir)

> Note on transcription: the source PDF is a printed capture of a GitHub README rendered on the Google Dev Library site. Several YAML code lines are cut off at the page margin in the original document (e.g. mid-expression on `awk`, `env.main_project_module`, etc.). Those lines are transcribed exactly as they appear in the source, including the truncation — nothing has been invented to complete them.

## Overview

This project demonstrates how to automate building Android APKs and App Bundles (AAB) using GitHub Actions workflows. It covers:

- Project GitHub Action Script (YAML)
- Using GitHub Workflows
- Automated Build of AAB (release)
- Automated Build of APK (release and debug)
- Using Bundletool
- Downloading artifacts
- Uploading artifacts
- Clearing (cleaning up) artifacts by naming convention

## Version / Release

Latest release: `version_release = 2.2.7`

**What's New:**

- Update Target SDK 36
- Update Action Script
- Update Android Studio Latest Version
- Update Gradle Latest Version
- Update Kotlin Latest Version
- Update GitHub Action Script
- Add Bash and Bat Script

**Article sources / guide sources (GitHub Action):**

- Sample artifact naming: `${date_today} - ${repository_name} - ${playstore_name} - APK(s)`
- Tested on a private repository (App Bundle(s) and APK generated successfully)
- Can also be run locally with a `.run` configuration in a local machine

Full code for the GitHub Actions workflow: [generate-apk-aab-debug-release.yml](https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/generate-apk-aab-debug-release.yml)

Further reading referenced by the article:

- [How To Securely Build and Sign Your Android App With GitHub Actions](https://proandroiddev.com/how-to-securely-build-and-sign-your-android-app-with-github-actions-ad5323452ce)
- [How to Use GitHub Actions to Automate Android App Development](https://www.freecodecamp.org/news/use-github-actions-to-automate-android-development/)
- [Update Java Checkout Version CI (`actions/setup-java`)](https://github.com/actions/setup-java)

## How to Use Workflows (Push-Triggered Build)

**Step 1.** Upload your project to GitHub.

**Step 2.** Create the workflow file. The project must be an Android Studio project using Gradle. Create a file named `generate-apk-aab-debug-release.yml` inside the folder `.github/workflows/`, so the final path is:

```
.github/workflows/generate-apk-aab-debug-release.yml
```

**Step 3.** Populate the workflow with the following content (as captured from the source; some lines are truncated in the original document where noted):

```yaml
name: Generated APK AAB (Push Github - Create Artifact To Github Action)

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID
  # The output folder for build results
  build_output_path: buildActionResult

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release

      # ======================================================
      # Copy Build Outputs & Push to GitHub
      # ======================================================

      # Delete build/outputs directory if it already exists
      - name: Clean up build/outputs directory
        run: rm -rf ${{ env.build_output_path }}

      # Create build/outputs directory if it doesn't exist
      - name: Create build/outputs directory
        run: mkdir -p ${{ env.build_output_path }}

      # Copy APK Debug to build/outputs
      - name: Copy APK Debug to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/apk/debug/* ${  # [line truncated in source]

      # Copy APK Release to build/outputs
      - name: Copy APK Release to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/apk/release/*  # [line truncated in source]

      # Copy AAB Release to build/outputs
      - name: Copy AAB Release to build/outputs
        run: |
          cp -r ${{ env.main_project_module }}/build/outputs/bundle/release  # [line truncated in source]

      # List copied files for verification
      - name: List build/outputs contents
        run: ls -la ${{ env.build_output_path }}/

      # Commit and Push to GitHub
      - name: Commit & Push build outputs to GitHub
        run: |
          git config user.name '${{ github.actor }}'
          git config user.email '${{ github.actor }}@users.noreply.github.c  # [line truncated in source]
          git add ${{ env.build_output_path }}/ -f
          git diff --cached --quiet && echo "No changes to commit" || (git  # [line truncated in source]
```

This workflow was reported as tested successfully against a private repository (App Bundle(s) and APK generated successfully).

## How to Use Workflows (Manual / Upload-Triggered Build)

**Step 1.** Upload your project to GitHub.

**Step 2.** Create the workflow file at `.github/workflows/generate-apk-aab-debug-release.yml` (same location as above). The project must be an Android Studio project using Gradle.

**Step 3.** Use the following workflow content:

```yaml
name: Generated APK AAB (Upload - Create Artifact To Github Action)

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release
```

**Step 4.** The build runs automatically on the Actions tab of the GitHub repository.

**Step 5.** Download the generated artifact.

Related tooling referenced:

- [Download Artifact From GitHub Action](https://github.com/actions/download-artifact)
- [Upload Artifact From GitHub Action](https://github.com/actions/upload-artifact)
- [Remove Artifact (`c-hive/gha-remove-artifacts`)](https://github.com/c-hive/gha-remove-artifacts)

### Results (Private Repository, Build Proven Successful)

The article includes screenshots showing:

- APK(s) debug generated
- APK(s) release generated
- App bundle(s) release generated

## Clean Up Artifact Workflow

A separate scheduled workflow removes old artifacts automatically:

```yaml
name: Generated APK AAB (Clean)

on:
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:
  schedule:
    # Every day at 1am
    - cron: '0 1 * * *'

jobs:
  remove-old-artifacts:
    runs-on: ubuntu-latest
    timeout-minutes: 10
    steps:
      - name: Clean all artifacts
        uses: c-hive/gha-remove-artifacts@v4
        with:
          age: '60 seconds'  # '<number> <unit>', e.g. 5 days, 2 years, 90 s  # [line truncated in source]
          # Optional inputs
          # skip-tags: true
          # skip-recent: 5
```

## Using Bundletool

**Step 1.** Prepare Bundletool. Check for the tool at `.github/lib/bundletool.jar` — [download the latest release here](https://github.com/google/bundletool/releases).

**Step 2.** Use the workflow code in [android-ci-generate-apk-aab-upload-3.yml](https://github.com/amirisback/automated-build-android-app-with-github-action/blob/master/.github/workflows/android-ci-generate-apk-aab-upload-3.yml):

```yaml
name: Generated APK AAB 2 Bundle Tool (Upload - Create Artifact To Github A  # [line truncated in source]

env:
  # The name of the main module repository
  main_project_module: app
  # The name of the Play Store
  playstore_name: Frogobox ID
  # Keystore Path
  ks_path: frogoboxdev.jks
  # Keystore Password
  ks_store_pass: cronoclez
  # Keystore Alias
  ks_alias: frogobox
  # Keystore Alias Password
  ks_alias_pass: xeonranger

on:
  push:
    branches:
      - 'release/**'
  # Allows you to run this workflow manually from the Actions tab
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # Set Current Date As Env Variable
      - name: Set current date as env variable
        run: echo "date_today=$(date +'%Y-%m-%d')" >> $GITHUB_ENV

      # Set Repository Name As Env Variable
      - name: Set repository name as env variable
        run: echo "repository_name=$(echo '${{ github.repository }}' | awk   # [line truncated in source]

      - name: Set Up JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'zulu'  # See 'Supported distributions' for availabl  # [line truncated in source]
          java-version: '17'
          cache: 'gradle'

      - name: Change wrapper permissions
        run: chmod +x ./gradlew

      # Run Tests Build
      - name: Run gradle tests
        run: ./gradlew test

      # Run Build Project
      - name: Build gradle project
        run: ./gradlew build

      # Create APK Debug
      - name: Build apk debug project (APK) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew assembleDebug

      # Create APK Release
      - name: Build apk release project (APK) - ${{ env.main_project_module }}  # [line truncated in source]
        run: ./gradlew assemble

      # Create Bundle AAB Release
      # Noted for main module build [main_project_module]:bundleRelease
      - name: Build app bundle release (AAB) - ${{ env.main_project_module }}   # [line truncated in source]
        run: ./gradlew ${{ env.main_project_module }}:bundleRelease

      # - name: Build APK(s) Debug from bundle using bundletool
      # run: java -jar ".github/lib/bundletool.jar" build-apks --bundle=$   # [line truncated in source, commented-out step]

      - name: Set Env Artifact name from generated aab
        run: |
          cd ${{ env.main_project_module }}/build/outputs/bundle/release/
          files=(*)
          echo "generated_name_aab=${files[0]%.*}" >> $GITHUB_ENV

      # Build APK From Bundle Using Bundletool
      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Build APK(s) Release from bundle using bundletool (Path same   # [line truncated in source]
        run: java -jar ".github/lib/bundletool.jar" build-apks --bundle=${{  # [line truncated in source]

      # Duplicate APK(s) Release to zip file and extract
      - name: Duplicate APK(s) Release to zip file and extract
        run: |
          cd ${{ env.main_project_module }}/build/outputs/bundle/release/
          unzip -p ${{ env.generated_name_aab }}.apks universal.apk > ${{ e  # [line truncated in source]

      # Upload Artifact Build
      # Noted For Output [main_project_module]/build/outputs/apk/debug/
      - name: Upload APK Debug - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/debug/

      # Noted For Output [main_project_module]/build/outputs/apk/release/
      - name: Upload APK Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/apk/release/

      # Noted For Output [main_project_module]/build/outputs/bundle/release
      - name: Upload AAB (App Bundle) Release - ${{ env.repository_name }}
        uses: actions/upload-artifact@v4
        with:
          name: ${{ env.date_today }} - ${{ env.playstore_name }} - ${{ env  # [line truncated in source]
          path: ${{ env.main_project_module }}/build/outputs/bundle/release
```

**Step 3.** Run the action.

**Step 4.** Wait for the action to run — check periodically in case of errors.

**Step 5.** Download the artifact (AAB artifact) and extract it.

## Run Using Gradle Configuration (`.run` Configuration)

An alternative for developers who don't have (or don't want to use) GitHub Actions: run builds locally through an IntelliJ/Android Studio `.run` configuration.

**Step 1.** Create a folder named `.run` at the root of the project directory. This lets you run builds on your local machine.

**Step 2.** Create a file named `[name-config].run.xml` inside it:

```xml
<component name="ProjectRunConfigurationManager">
  <!-- Add Name Configuration Here -->
  <configuration default="false" name="${your-config-name}" type="GradleRun...">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <!-- TODO : add your task here -->
          <option value=":app:assembleDebug" />
        </list>
      </option>
      <option name="vmOptions" />
    </ExternalSystemSettings>
    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
    <DebugAllEnabled>false</DebugAllEnabled>
    <RunAsTest>false</RunAsTest>
    <method v="2" />
  </configuration>
</component>
```

**Step 3.** The configuration will appear in the run-configuration menu in the IDE.

**Step 4.** Running it executes the configured task(s), producing build output directly on the local machine.

### Sample Configuration: `signingreport`

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="signingreport" type="GradleRunConfig...">
    <ExternalSystemSettings>
      <option name="executionName" />
      <option name="externalProjectPath" value="$PROJECT_DIR$" />
      <option name="externalSystemIdString" value="GRADLE" />
      <option name="scriptParameters" value="" />
      <option name="taskDescriptions">
        <list />
      </option>
      <option name="taskNames">
        <list>
          <option value="signingreport" />
        </list>
      </option>
      <option name="vmOptions" />
    </ExternalSystemSettings>
    <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
    <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
    <DebugAllEnabled>false</DebugAllEnabled>
    <RunAsTest>false</RunAsTest>
    <method v="2" />
  </configuration>
</component>
```

This configuration runs Gradle's `signingreport` task, which prints the SHA-1/SHA-256 signing fingerprints for each build variant — useful for registering a debug/release keystore fingerprint with services like Firebase or Google Play.

## Run Using Script File

### Batch script (Windows)

```bat
@echo off
setlocal

:: Navigate to the project root directory
cd /d "%~dp0.."

echo ======================================
echo Starting Android Build Process
echo ======================================

echo [1/6] Cleaning project...
call gradlew clean

echo [2/6] Running tests...
call gradlew test

echo [3/6] Building project...
call gradlew build

echo [4/6] Assembling Debug APK...
call gradlew assembleDebug

echo [5/6] Assembling Release APK...
call gradlew assemble

echo [6/6] Building Release App Bundle (AAB)...
call gradlew app:bundleRelease

echo ======================================
echo Build completed successfully!
echo ======================================
pause
```

### Shell script (macOS/Linux)

```bash
#!/bin/bash
echo "======================================"
echo " Starting Android Build Process "
echo "======================================"

echo "[1/6] Cleaning project..."
./gradlew clean

echo "[2/6] Running tests..."
./gradlew test

echo "[3/6] Building project..."
./gradlew build

echo "[4/6] Assembling Debug APK..."
./gradlew assembleDebug

echo "[5/6] Assembling Release APK..."
./gradlew assemble

echo "[6/6] Building Release App Bundle (AAB)..."
./gradlew app:bundleRelease

echo "======================================"
echo " Build completed successfully! "
echo "======================================"
```

## Collaborator / Contributing

The project is open to contributions. Contributors are credited in the README; contact is made by email.

- **Mail to:** `faisalamircs@gmail.com`
- **Subject format:** `Github_[Github-Username-Account]_[Language]_[Repository-Name]`
- **Example:** `Github_amirisback_kotlin_admob-helper-implementation`

Maintainer: Muhammad Faisal Amir.

A waiting list for contributors is present in the README (unpopulated at time of capture).

## AI Agent Skill

This project includes an AI Agent Skill designed to help AI coding assistants (such as Antigravity) understand and manage the GitHub Workflows in this repository.

**How to use:** if you are using an AI assistant, you can ask it to use the skill located at:

```
skills/github-workflows/SKILL.md
```

**Example commands:**

- "Explain how the APK upload workflow works using the github-workflows skill."
- "Use the github-workflows skill to add a new environment variable to all CI scripts."
- "Help me troubleshoot a failed build using the github-workflows skill."

## Related Content (Google Dev Library — Android)

The article page also surfaces related Android repositories on Google Dev Library:

- [Chucker](https://devlibrary.withgoogle.com/products/android/repos/ChuckerTeam-chucker) by [ChuckerTeam](https://devlibrary.withgoogle.com/authors/vbuberen) — an HTTP inspector for Android & OkHttp (like Charles Proxy but on-device).
- [detekt](https://devlibrary.withgoogle.com/products/android/repos/detekt-detekt) by [detekt](https://devlibrary.withgoogle.com/authors/cortinico) — a static code analysis tool for Kotlin, operating on the abstract syntax tree provided by the Kotlin compiler.
- [AboutLibraries](https://devlibrary.withgoogle.com/products/android/repos/mikepenz-AboutLibraries) by [mikepenz](https://devlibrary.withgoogle.com/authors/mikepenz) — automatically collects all dependencies and licenses of a Gradle project (including Kotlin Multiplatform) and provides easy-to-integrate UI components for Android.
- [PermissionX](https://devlibrary.withgoogle.com/products/android/repos/guolindev-PermissionX) by [guolindev](https://devlibrary.withgoogle.com/authors/guolindev) — an open-source Android library that makes handling runtime permissions extremely easy.
- [LitePal](https://devlibrary.withgoogle.com/products/android/repos/guolindev-LitePal) by [guolindev](https://devlibrary.withgoogle.com/authors/guolindev) — an Android library that makes using a SQLite database extremely easy.

Page footer: [About](https://devlibrary.withgoogle.com/about) · Terms · Privacy · Feedback

## Relevance to this project

This document is directly relevant to Novus Agenti / Omni Claw's eventual APK build pipeline. It provides a ready-made, field-tested GitHub Actions workflow template for building both debug/release APKs and release AABs from an Android Studio + Gradle project, plus a Bundletool-based path for extracting a universal APK from an AAB (useful for sideloading/testing builds without going through the Play Store). The scheduled artifact-cleanup workflow and the local `.run`-configuration / shell-script alternatives are useful fallback options for CI setup once the project needs repeatable, automated builds of the on-device assistant APK.
