# Build, Lint & Release Verification Report (Phase 5 — Task 5.7)

**Project:** `m.ebanking`  
**Package:** `com.mebanking.app`  
**Build Tool:** Gradle 9.6.0 + Android Gradle Plugin 9.3.0 + Kotlin 2.4.21 + KSP 2.3.14  
**Audit Date:** 2026-11-23  
**Status:** **BUILD VERIFIED (Debug and Release APKs Generated Successfully)**

---

## 1. Build Verification Matrix

| Target Task | Command | Duration | Output Artifact | Status |
| :--- | :--- | :---: | :--- | :---: |
| **Unit Test Suite** | `./gradlew :app:testDebugUnitTest` | 1m 37s | 495 tests passed | **PASS** |
| **AndroidTest Sources** | `./gradlew :app:compileDebugAndroidTestSources` | 2s | Test APK sources compiled | **PASS** |
| **Debug Packaging** | `./gradlew :app:assembleDebug` | 54s | `app-debug.apk` (14.38 MB) | **PASS** |
| **Release Packaging** | `./gradlew :app:assembleRelease` | 1m 49s | `app-release-unsigned.apk` (10.26 MB) | **PASS** |
| **Release Lint Vital** | Task `:app:lintVitalRelease` | Inline | Zero fatal lint violations | **PASS** |

---

## 2. Artifact Details

### A. Debug Build Artifact
- **Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **Size:** 14,383,222 bytes (~14.38 MB)
- **Application ID:** `com.mebanking.app.debug`
- **Version Code:** 1
- **Version Name:** `1.0.0-debug`
- **Signing:** Default Android debug keystore

### B. Release Build Artifact
- **Path:** `app/build/outputs/apk/release/app-release-unsigned.apk`
- **Size:** 10,261,448 bytes (~10.26 MB)
- **Application ID:** `com.mebanking.app`
- **Version Code:** 1
- **Version Name:** `1.0.0`
- **Min SDK:** 24 (Android 7.0 Nougat)
- **Target SDK:** 37 (Android 15+)
- **Compile SDK:** 37
- **Proguard / R8:** Cleanly compiled, unsigned release ready for production signing pipeline

---

## 3. Manifest & Permissions Audit

### A. Declared Permissions ([AndroidManifest.xml](file:///home/chocolat/Documents/Project/m.ebanking/app/src/main/AndroidManifest.xml)):
- `android.permission.USE_BIOMETRIC`: Required for AndroidX BiometricPrompt transaction authentication.
- `android.permission.CAMERA`: Required for eKYC face frame capture.
- `android.permission.ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Required for nearest bank branch distance computation.

### B. Security Flags:
- `android:allowBackup="false"` (Excludes database from cloud backup / adb backup).
- `android:fullBackupContent="false"`.
- Uses app-private sandboxed directories for database (`databases/mebanking.db`) and encrypted biometric templates (`files/biometrics/`).

---

## 4. Release Readiness Checklist

- [x] All 495 automated unit/integration tests passing.
- [x] Instrumented test sources compile cleanly (`compileDebugAndroidTestSources`).
- [x] Database migration scripts v1->v2 and v2->v3 validated.
- [x] Debug APK builds and installs cleanly.
- [x] Release APK builds with zero fatal lint violations (`lintVitalRelease`).
- [x] No plaintext passwords, keys, or private tokens stored in code.
- [x] Pure offline runtime invariant preserved.

**Verdict: PASS.**
