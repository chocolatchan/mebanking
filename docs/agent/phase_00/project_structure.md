# Project Structure

- Inspected ref: `master`
- Commit: `efc15e138cb0bc4f3420e8b2caedb29fdfb36299`
- Gate contribution: Task 0.1, **CONDITIONAL PASS**

## Tracked repository layout

```text
.
├── .agent/
│   └── work.md
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/mebanking/app/
│           │   ├── MainActivity.kt
│           │   ├── data/
│           │   │   ├── model/Account.kt
│           │   │   ├── remote/
│           │   │   │   ├── BankingApi.kt
│           │   │   │   └── NetworkModule.kt
│           │   │   └── repository/AccountRepository.kt
│           │   └── ui/
│           │       ├── MEbankingApp.kt
│           │       ├── home/
│           │       │   ├── HomeScreen.kt
│           │       │   ├── HomeUiState.kt
│           │       │   └── HomeViewModel.kt
│           │       └── theme/
│           │           ├── Color.kt
│           │           ├── Theme.kt
│           │           └── Type.kt
│           └── res/
│               ├── drawable/
│               ├── mipmap/
│               ├── mipmap-anydpi-v26/
│               ├── values/
│               ├── values-night/
│               └── xml/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── README.md
└── settings.gradle.kts
```

## Module and source ownership map

| Area | Current path | Responsibility observed |
|---|---|---|
| Android entry | `app/src/main/java/com/mebanking/app/MainActivity.kt` | Creates Compose content and applies theme |
| App root | `ui/MEbankingApp.kt` | Hosts root surface and Home screen |
| Home UI | `ui/home/HomeScreen.kt` | Loading, error, empty, balance, account-list UI |
| Home state | `ui/home/HomeUiState.kt` | Account list, loading/error, derived total |
| Home ViewModel | `ui/home/HomeViewModel.kt` | Loads account list and exposes StateFlow |
| Account model | `data/model/Account.kt` | API account DTO |
| Remote contract | `data/remote/BankingApi.kt` | GET account collection and GET account by ID |
| Network setup | `data/remote/NetworkModule.kt` | OkHttp and Retrofit instances |
| Repository | `data/repository/AccountRepository.kt` | Delegates account reads to remote API |
| Theme | `ui/theme/` | Compose colors, typography, Material theme |

## Build structure

- Root project name: `m.ebanking`.
- Included Gradle project: `:app`.
- Root and app build scripts use Kotlin DSL.
- Dependency versions are centralized in `gradle/libs.versions.toml`.
- Gradle Wrapper distribution: Gradle 9.6.0.
- Android Gradle Plugin version declared in catalog: 9.5.1.
- Kotlin version declared in catalog: 2.4.21.
- Java compatibility: 17.

## Absent paths in the inspected Git tree

No tracked files were found under:
- `app/src/test/`
- `app/src/androidTest/`
- `app/src/main/java/.../data/local/`
- `app/src/main/java/.../domain/`
- `app/src/main/java/.../navigation/`
- `app/src/main/java/.../di/`

This is an inventory of the inspected commit, not a claim about untracked files on the developer's machine.
