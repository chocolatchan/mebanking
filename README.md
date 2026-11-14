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
