# M.Ebanking

Android app skeleton built with Kotlin, Jetpack Compose (Material 3) and
Retrofit/OkHttp.

## Stack

- Kotlin + Jetpack Compose, Material 3
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
- AGP 9 with built-in Kotlin support (no separate `kotlin-android` plugin)
- Retrofit + OkHttp + Gson for networking
- Manual dependency wiring (easy to swap for Hilt/Koin later)

## Project layout

```
app/src/main/java/com/mebanking/app/
├── MainActivity.kt              # single-activity entry point
├── data/
│   ├── model/Account.kt         # API models
│   ├── remote/BankingApi.kt     # Retrofit endpoints
│   ├── remote/NetworkModule.kt  # OkHttp/Retrofit setup
│   └── repository/              # data source abstraction
└── ui/
    ├── MEbankingApp.kt          # app root, add navigation here
    ├── home/                    # Home screen + ViewModel + UiState
    └── theme/                   # colors, typography, Material theme
```

## Build

### JDK setup (Windows and Linux)

This project uses Gradle Wrapper 9.6.0 and Android Gradle Plugin 9.5.1. Use a
64-bit JDK 17 for the Gradle process. The checked-in Gradle daemon criteria also
select JDK 17; if a matching daemon JDK is not installed, Gradle can provision it
from the platform-specific URLs in `gradle/gradle-daemon-jvm.properties`.

Install a JDK 17 distribution such as Eclipse Temurin, then set `JAVA_HOME` to
its installation directory and add its `bin` directory to `PATH`.

**Windows PowerShell (current terminal):**

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
```

For a persistent Windows setup, add `JAVA_HOME` in Windows Environment
Variables, add `%JAVA_HOME%\bin` to `Path`, then restart the terminal or IDE.

**Linux (Bash):**

```bash
export JAVA_HOME="/path/to/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

Set these exports in your shell profile to keep them across sessions. Use the
actual JDK installation path for your distribution; do not add it to project
Gradle files.

**Android Studio:** Open **Settings/Preferences → Build, Execution, Deployment
→ Build Tools → Gradle**, and select a JDK 17 installation for **Gradle JDK**.
On macOS, the same setting is under **Android Studio → Settings**. Confirm the
Gradle JDK is 17 before syncing the project.

The Android SDK path belongs in the machine-local `local.properties` file;
that file is ignored by Git. Do not commit machine-specific JDK or SDK paths.

### Build command

```bash
./gradlew assembleDebug
```

Set the API base URL in `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
```

`API_BASE_URL` must end with `/`. Debug builds use the `.debug` application ID
suffix so debug and release installs can coexist.

## Next steps

- Add navigation (Navigation Compose) in `ui/MEbankingApp.kt`
- Add authentication and attach the token in `NetworkModule`
- Add unit/instrumentation tests (`app/src/test`, `app/src/androidTest`)
