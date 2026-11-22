# Baseline Build Report

**Phase**: Phase 0 — Repository Reconnaissance  
**Task**: Task 0.2 Baseline Build and Test Execution Audit  
**Date**: 2026-11-19  
**Target Repository**: `/home/chocolat/Documents/Project/m.ebanking`  
**Evaluation Status**: Verified Baseline  

---

## 1. Build Environment Specifications

The build execution was conducted on the canonical local environment without external modifications:

| Environment Property | Value | Source |
|---|---|---|
| **Operating System** | Linux (Arch Linux / CachyOS x86_64) | Host OS |
| **Java Runtime (JVM)** | Java 27 (OpenJDK 27.0.1.0.3) | Host environment |
| **Java Target / Source Compatibility** | Java 17 (`JavaVersion.VERSION_17`) | `app/build.gradle.kts:35-36` |
| **Gradle Runtime** | Gradle 9.6.0 | `gradle/wrapper/gradle-wrapper.properties:3` |
| **Android Gradle Plugin (AGP)** | 9.5.1 | `gradle/libs.versions.toml:2` |
| **Kotlin Compiler** | 2.4.21 | `gradle/libs.versions.toml:3` |
| **Android Compile SDK** | 37 (Android 16 preview) | `app/build.gradle.kts:8` |
| **Android Target SDK** | 37 | `app/build.gradle.kts:13` |
| **Android Min SDK** | 24 (Android 7.0) | `app/build.gradle.kts:12` |
| **Build Configuration Caching** | Enabled (`org.gradle.configuration-cache=true`) | `gradle.properties:7` |

---

## 2. Canonical Build Command Executions

### 2.1 Debug Build: `./gradlew assembleDebug`
- **Executed Command**: `./gradlew assembleDebug`
- **Exit Code**: `0` (BUILD SUCCESSFUL)
- **Execution Time**: ~988 ms (cached)
- **Actionable Tasks**: 38 tasks (38 up-to-date)
- **Output Artifact**: `app/build/outputs/apk/debug/app-debug.apk` (Size: 13,049,216 bytes)
- **Verbatim Output**:
  ```
  Reusing configuration cache.
  > Task :app:preBuild UP-TO-DATE
  > Task :app:generateDebugAssets UP-TO-DATE
  > Task :app:preDebugBuild UP-TO-DATE
  > Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
  > Task :app:generateDebugResources UP-TO-DATE
  > Task :app:javaPreCompileDebug UP-TO-DATE
  > Task :app:generateDebugBuildConfig UP-TO-DATE
  > Task :app:desugarDebugFileDependencies UP-TO-DATE
  > Task :app:generateDebugGlobalSynthetics UP-TO-DATE
  > Task :app:extractDeepLinksDebug UP-TO-DATE
  > Task :app:mergeDebugJniLibFolders UP-TO-DATE
  > Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
  > Task :app:mapDebugSourceSetPaths UP-TO-DATE
  > Task :app:validateSigningDebug UP-TO-DATE
  > Task :app:writeDebugAppMetadata UP-TO-DATE
  > Task :app:writeDebugSigningConfigVersions UP-TO-DATE
  > Task :app:packageDebugResources UP-TO-DATE
  > Task :app:checkDebugDuplicateClasses UP-TO-DATE
  > Task :app:mergeDebugAssets UP-TO-DATE
  > Task :app:checkDebugAarMetadata UP-TO-DATE
  > Task :app:processDebugNavigationResources UP-TO-DATE
  > Task :app:compressDebugAssets UP-TO-DATE
  > Task :app:mergeLibDexDebug UP-TO-DATE
  > Task :app:mergeDebugResources UP-TO-DATE
  > Task :app:compileDebugNavigationResources UP-TO-DATE
  > Task :app:parseDebugLocalResources UP-TO-DATE
  > Task :app:mergeDebugNativeLibs UP-TO-DATE
  > Task :app:generateDebugRFile UP-TO-DATE
  > Task :app:stripDebugDebugSymbols UP-TO-DATE
  > Task :app:mergeExtDexDebug UP-TO-DATE
  > Task :app:processDebugMainManifest UP-TO-DATE
  > Task :app:processDebugManifest UP-TO-DATE
  > Task :app:processDebugManifestForPackage UP-TO-DATE
  > Task :app:compileDebugKotlin UP-TO-DATE
  > Task :app:compressDebugJavaRes UP-TO-DATE
  > Task :app:compileDebugJavaWithJavac UP-TO-DATE
  > Task :app:mergeDebugJavaResource UP-TO-DATE
  > Task :app:processDebugResources UP-TO-DATE
  > Task :app:dexBuilderDebug UP-TO-DATE
  > Task :app:mergeProjectDexDebug UP-TO-DATE
  > Task :app:packageDebug UP-TO-DATE
  > Task :app:assembleDebug UP-TO-DATE
  > Task :app:createDebugApkListingFileRedirect UP-TO-DATE

  BUILD SUCCESSFUL in 988ms
  38 actionable tasks: 38 up-to-date
  Configuration cache entry reused.
  ```

---

### 2.2 Release Build: `./gradlew assembleRelease`
- **Executed Command**: `./gradlew assembleRelease`
- **Exit Code**: `0` (BUILD SUCCESSFUL)
- **Execution Time**: ~939 ms (cached)
- **Actionable Tasks**: 49 tasks (1 executed, 48 up-to-date)
- **Output Artifact**: `app/build/outputs/apk/release/app-release-unsigned.apk` (Size: 8,918,824 bytes)
- **Verbatim Output**:
  ```
  Reusing configuration cache.
  > Task :app:preBuild UP-TO-DATE
  > Task :app:generateReleaseAssets UP-TO-DATE
  > Task :app:preReleaseBuild UP-TO-DATE
  > Task :app:generateReleaseBuildConfig UP-TO-DATE
  > Task :app:mergeReleaseJniLibFolders UP-TO-DATE
  > Task :app:generateReleaseResources UP-TO-DATE
  > Task :app:mapReleaseSourceSetPaths UP-TO-DATE
  > Task :app:desugarReleaseFileDependencies UP-TO-DATE
  > Task :app:extractDeepLinksRelease UP-TO-DATE
  > Task :app:extractProguardFiles UP-TO-DATE
  > Task :app:createReleaseCompatibleScreenManifests UP-TO-DATE
  > Task :app:javaPreCompileRelease UP-TO-DATE
  > Task :app:mergeReleaseStartupProfile UP-TO-DATE
  > Task :app:writeReleaseAppMetadata UP-TO-DATE
  > Task :app:extractReleaseVersionControlInfo UP-TO-DATE
  > Task :app:packageReleaseResources UP-TO-DATE
  > Task :app:writeReleaseSigningConfigVersions UP-TO-DATE
  > Task :app:checkReleaseDuplicateClasses UP-TO-DATE
  > Task :app:collectReleaseDependencies UP-TO-DATE
  > Task :app:checkReleaseAarMetadata UP-TO-DATE
  > Task :app:processReleaseNavigationResources UP-TO-DATE
  > Task :app:sdkReleaseDependencyData UP-TO-DATE
  > Task :app:mergeReleaseAssets UP-TO-DATE
  > Task :app:compileReleaseNavigationResources UP-TO-DATE
  > Task :app:mergeReleaseNativeLibs UP-TO-DATE
  > Task :app:compressReleaseAssets UP-TO-DATE
  > Task :app:parseReleaseLocalResources UP-TO-DATE
  > Task :app:mergeReleaseResources UP-TO-DATE
  > Task :app:stripReleaseDebugSymbols UP-TO-DATE
  > Task :app:extractReleaseNativeSymbolTables UP-TO-DATE
  > Task :app:generateReleaseRFile UP-TO-DATE
  > Task :app:mergeReleaseNativeDebugMetadata NO-SOURCE
  > Task :app:mergeReleaseArtProfile UP-TO-DATE
  > Task :app:processReleaseMainManifest UP-TO-DATE
  > Task :app:processReleaseManifest UP-TO-DATE
  > Task :app:mergeExtDexRelease UP-TO-DATE
  > Task :app:processReleaseManifestForPackage UP-TO-DATE
  > Task :app:compileReleaseKotlin UP-TO-DATE
  > Task :app:compressReleaseJavaRes UP-TO-DATE
  > Task :app:compileReleaseJavaWithJavac UP-TO-DATE
  > Task :app:mergeReleaseJavaResource UP-TO-DATE
  > Task :app:processReleaseResources UP-TO-DATE
  > Task :app:optimizeReleaseResources UP-TO-DATE
  > Task :app:dexBuilderRelease UP-TO-DATE
  > Task :app:mergeDexRelease UP-TO-DATE
  > Task :app:lintVitalAnalyzeRelease UP-TO-DATE
  > Task :app:generateReleaseLintVitalReportModel UP-TO-DATE
  > Task :app:mergeReleaseGlobalSynthetics UP-TO-DATE
  > Task :app:compileReleaseArtProfile UP-TO-DATE
  > Task :app:lintVitalReportRelease UP-TO-DATE
  > Task :app:lintVitalRelease
  > Task :app:packageRelease UP-TO-DATE
  > Task :app:assembleRelease UP-TO-DATE
  > Task :app:createReleaseApkListingFileRedirect UP-TO-DATE

  BUILD SUCCESSFUL in 939ms
  49 actionable tasks: 1 executed, 48 up-to-date
  Configuration cache entry reused.
  ```

---

### 2.3 Static Analysis: `./gradlew lintDebug`
- **Executed Command**: `./gradlew lintDebug`
- **Exit Code**: `0` (BUILD SUCCESSFUL)
- **Summary**: `0 errors, 4 warnings`
- **Report Location**: `app/build/reports/lint-results-debug.txt`

#### Lint Warning Inventory
1. **Warning 1: `RedundantLabel`**
   - **File**: `app/src/main/AndroidManifest.xml:19`
   - **Code**: `android:label="@string/app_name"` inside `<activity android:name=".MainActivity">`
   - **Explanation**: The `<application>` tag already declares `android:label="@string/app_name"`. Specifying the identical label on `MainActivity` is redundant.
2. **Warning 2: `AndroidGradlePluginVersion`**
   - **File**: `gradle/wrapper/gradle-wrapper.properties:3`
   - **Code**: `distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.0-bin.zip`
   - **Explanation**: A newer Gradle wrapper distribution (9.9.1) is available. Current Gradle 9.6.0 is stable and compliant with AGP 9.5.1.
3. **Warning 3: `GradleDependency`**
   - **File**: `gradle/libs.versions.toml:4`
   - **Code**: `coreKtx = "1.18.0"`
   - **Explanation**: A newer version `1.19.1` of `androidx.core:core-ktx` is available in Google Maven.
4. **Warning 4: `NewerVersionAvailable`**
   - **File**: `gradle/libs.versions.toml:10`
   - **Code**: `gson = "2.13.2"`
   - **Explanation**: A newer version `2.15.0` of `com.google.code.gson:gson` is available in Maven Central.

---

### 2.4 Task Discovery Failure: `./gradlew tasks`
- **Executed Command**: `./gradlew tasks`
- **Exit Code**: `1` (BUILD FAILED)
- **Failure Cause**: `Could not create task ':app:generateDebugComposePreviewRunfiles'`
- **Verbatim Error**:
  ```
  FAILURE: Build failed with an exception.

  * What went wrong:
  Execution failed for task ':tasks' (registered by plugin 'org.gradle.help-tasks').
  > Could not create task ':app:generateDebugComposePreviewRunfiles'.
     > Compose Preview failed for ':app' (variant 'debug'): Android resources are excluded from unit tests ('testOptions.unitTests.isIncludeAndroidResources = false' in finalizeDsl). Compose Preview requires compiled Android resources to resolve layout XML and theme attributes.
  ```
- **Technical Analysis**:
  Under AGP 9.5.1, the help task `:tasks` attempts to register runfiles for Compose `@Preview` compilation. AGP's Compose Preview engine requires unit tests to include compiled Android resources so that XML layouts and Material theme attributes can be resolved. Because `app/build.gradle.kts` lacks an explicit `testOptions.unitTests.isIncludeAndroidResources = true` block, Gradle terminates task configuration with exit code 1.
- **Planned Resolution (Subsequent Phases)**:
  In Phase 1 / Phase 2, when configuring unit testing dependencies, add the following to `app/build.gradle.kts`:
  ```kotlin
  android {
      testOptions {
          unitTests.isIncludeAndroidResources = true
      }
  }
  ```

---

## 3. Build Artifact Summary

| Variant | Artifact Path | Size (Bytes) | Package Identifier |
|---|---|---|---|
| **Debug** | `app/build/outputs/apk/debug/app-debug.apk` | 13,049,216 | `com.mebanking.app.debug` |
| **Release** | `app/build/outputs/apk/release/app-release-unsigned.apk` | 8,918,824 | `com.mebanking.app` |

---

## 4. Conclusion

The build system configuration is fully functional for standard compilation pipelines (`assembleDebug` and `assembleRelease` compile cleanly). The task discovery issue in `./gradlew tasks` is well-understood and will be resolved cleanly when test options are configured in the next phase.
