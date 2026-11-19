# Baseline Build Report

- Task: 0.2 - Baseline Build & Test Audit
- Source snapshot inspected: `master` at `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Execution evidence: terminal output supplied by the user on 2026-11-19.
- Status: **CONDITIONAL PASS for build; task discovery failed; test execution details incomplete.**

## Execution environment (user-reported)

- OS: Linux 7.3.4-1-cachyos amd64
- Launcher JVM: 27.0.1.0.3
- Daemon JVM: /usr/lib/jvm/java-27-openjdk
- Gradle Wrapper: 9.6.0
- Gradle-reported embedded Kotlin: 2.3.21

The Gradle-reported embedded Kotlin version is distinct from the project's Kotlin Compose plugin declaration (2.4.21 in the inspected version catalog); these are not necessarily the same Kotlin component.

## Exact commands and results

The user supplied outputs in the same order as the command sequence requested in the previous audit response. The command-to-output mapping below follows that order.

| Command | Exit code | Observed result | Interpretation |
|---|---:|---|---|
| `./gradlew --version` | 0 | Printed Gradle 9.6.0, JVM and OS details | Wrapper starts and reports its runtime |
| `./gradlew tasks --all` | 1 | `:tasks FAILED`; could not create `:app:generateDebugComposePreviewRunfiles` | Task listing/configuration fails during Compose Preview task creation |
| `./gradlew assembleDebug` | 0 | `BUILD SUCCESSFUL`; 38 actionable tasks, all up-to-date | Debug assembly task graph succeeds |
| `./gradlew testDebugUnitTest` | 0 | `BUILD SUCCESSFUL`; 24 actionable tasks, all up-to-date | Unit-test Gradle task invocation succeeds; test count/report not included |
| `./gradlew connectedDebugAndroidTest` | 0 | `BUILD SUCCESSFUL`; 70 actionable tasks, 26 executed, 5 from cache, 39 up-to-date | Connected-test task invocation succeeds; output does not establish whether instrumentation tests actually ran or how many |
| `./gradlew lintDebug` | 0 | `BUILD SUCCESSFUL`; 29 actionable tasks, 1 executed, 28 up-to-date | Debug lint task succeeds |

No source/configuration workaround was applied as part of this audit. The task listing failure was preserved rather than fixed.

## Task discovery failure details

Observed failure:

```text
Execution failed for task ':tasks' (registered by plugin 'org.gradle.help-tasks').
> Could not create task ':app:generateDebugComposePreviewRunfiles'.
   > Compose Preview failed for ':app' (variant 'debug'): Android resources are excluded from unit tests ('testOptions.unitTests.isIncludeAndroidResources = false' in finalizeDsl). Compose Preview requires compiled Android resources to resolve layout XML and theme attributes.
```

Gradle suggested `--stacktrace`, `--info`, `--debug` and `--scan`. None of those diagnostics were included in the supplied output. The failure occurs while creating the Compose Preview runfiles task during task listing. It is not evidence that `assembleDebug` itself failed: the separately invoked assemble task completed successfully.

## Static build configuration observations

- Gradle Wrapper distribution: 9.6.0.
- Android Gradle Plugin: 9.5.1 (declared in version catalog).
- Kotlin Compose plugin: 2.4.21 (declared in version catalog).
- compileSdk/targetSdk: 37.
- minSdk: 24.
- Java compatibility: 17.
- User's local launcher/daemon JVM: Java 27.

The user's successful assemble result is runtime evidence that the current local environment completed that task, despite the declared Java compatibility setting. This report does not claim that Java 27 is the project's intended or officially supported toolchain.

## Remaining evidence gaps

- No detailed Gradle test summary (tests run, passed, failed, skipped) was included.
- No XML/HTML test report contents were supplied.
- The successful `connectedDebugAndroidTest` lifecycle does not alone prove an emulator/device executed any instrumentation tests; Gradle may have skipped/no-op'd the relevant test work.
- No full lint output or warning count was included.
- No stacktrace for `tasks --all` was included.

Suggested local evidence collection (read-only):

```bash
find app/build/test-results -type f -name '*.xml' -print
find app/build/reports -maxdepth 4 -type f \( -name 'index.html' -o -name '*.html' \) -print
grep -R -h '<testsuite\|<testsuites' app/build/test-results 2>/dev/null
```

To diagnose task discovery without changing files:

```bash
./gradlew tasks --all --stacktrace
```

**Task 0.2: CONDITIONAL PASS.** Debug assembly, unit-test task invocation, connected-test task invocation and lint task all returned success according to the supplied terminal output. Task discovery failed, and test execution counts/outcomes are still unverified. Do not report a passing test suite until reports show actual test counts and outcomes.
