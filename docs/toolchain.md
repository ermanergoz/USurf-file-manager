# Toolchain — USurf

Every version the build pins, one row each. Versions move as a ladder, top down: Gradle, the Android Gradle plugin,
Kotlin, the libraries, the quality tools, the test libraries; each rung takes the newest stable release the rungs above
it support. The ladder is capped by Realm 10.19.0, the last release of Realm Java, which the app keeps for its
favourites.

Resolved as, one of three:
- `latest stable`: the newest release with no pre-release marker on the day it was checked;
- `held: <reason>`: a stable release kept below the newest, because something above it does not support the newer one;
- `pre-release: <reason>`: no stable release works with the rungs above it (none today).

Lint lists newer releases of the Android plugin and of the libraries as information, so a release elsewhere never
fails the build; the versions here move on purpose, on a branch of their own.

## Build

| Dependency | Version | Resolved as | Checked on |
|---|---|---|---|
| Gradle (wrapper) | 9.9.0 | latest stable | 2026-11-08 |
| Android Gradle plugin (`com.android.tools.build:gradle`) | 9.5.1 | latest stable; run with `android.builtInKotlin=false` and `android.newDsl=false`, because Realm's plugin finds Kotlin through the Kotlin Android plugin and reads the classic `android` extension. A later plugin that drops both options is held until the favourites leave Realm Java. The plugin prints both options as deprecated on every build, beside the Kotlin Android plugin's own deprecation notice and the obsolete variant API warnings of Realm's and Safe Args' plugins; those warnings are this hold's trigger and are never silenced: the first plugin release that removes either option, or breaks this path, is held here with its reason | 2026-11-08 |
| Kotlin (`org.jetbrains.kotlin:kotlin-gradle-plugin`, with kapt and parcelize) | 2.4.21 | latest stable | 2026-11-08 |
| KSP (`com.google.devtools.ksp:symbol-processing-gradle-plugin`) | 2.3.13 | latest stable | 2026-11-08 |
| Realm (`io.realm:realm-gradle-plugin`, `realm-annotations-processor`) | 10.19.0 | latest stable; the last Realm Java release | 2026-11-08 |
| Navigation Safe Args (`androidx.navigation:navigation-safe-args-gradle-plugin`) | 2.9.8 | held: 2.10 requires minimum API 24, the app's minimum is 23; kept with the Navigation libraries | 2026-11-08 |
| compileSdk | 37 | latest stable platform | 2026-11-08 |
| targetSdk | 36 | held: the level Google Play requires; a newer target changes how the app runs and moves on its own branch with a device check | 2026-11-08 |
| minSdk | 23 | the product's minimum (Android 6.0) | 2026-11-08 |
| NDK | 29.0.14206865 | held: 30.0.16248370 is the newest stable; the 7-Zip native library is built and checked on 29 for all four ABIs, so the NDK moves on a branch of its own with that build and a device check | 2026-11-08 |
| CMake | 3.22.1 | held: 4.1.2 is the newest stable; the native build list asks for 3.22.1 at least and is checked on it, so CMake moves with the NDK, on the same branch | 2026-11-08 |
| 7-Zip (`app/src/main/cpp/7zip`, ip7z/7zip) | 26.06 (`d3296e3`) | latest stable tag; never below 26.02 | 2026-11-08 |
| JDK | 17 | the build machine's; Java and Kotlin compile for JVM 11 | 2026-11-08 |

## Libraries

| Dependency | Version | Resolved as | Checked on |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.19.1 | latest stable | 2026-11-08 |
| `androidx.appcompat:appcompat` | 1.8.0 | latest stable | 2026-11-08 |
| `com.google.android.material:material` | 1.14.0 | latest stable | 2026-11-08 |
| `androidx.constraintlayout:constraintlayout` | 2.2.2 | latest stable | 2026-11-08 |
| `androidx.recyclerview:recyclerview` | 1.4.0 | latest stable | 2026-11-08 |
| `androidx.swiperefreshlayout:swiperefreshlayout` | 1.2.0 | latest stable | 2026-11-08 |
| `androidx.navigation:navigation-fragment-ktx`, `navigation-runtime-ktx`, `navigation-ui-ktx` | 2.9.8 | held: 2.10 requires minimum API 24, the app's minimum is 23 | 2026-11-08 |
| `androidx.viewpager:viewpager` | 1.1.0 | latest stable | 2026-11-08 |
| `androidx.documentfile:documentfile` | 1.1.0 | latest stable | 2026-11-08 |
| `androidx.lifecycle:lifecycle-livedata-ktx` | 2.11.0 | latest stable | 2026-11-08 |
| `androidx.preference:preference-ktx` | 1.2.1 | latest stable | 2026-11-08 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | latest stable | 2026-11-08 |
| `org.apache.mina:mina-core` | 2.2.10 | latest stable | 2026-11-08 |
| `org.apache.ftpserver:ftplet-api`, `ftpserver-core` | 1.2.1 | latest stable | 2026-11-08 |
| `com.github.bumptech.glide:glide`, `ksp` | 5.0.9 | latest stable | 2026-11-08 |
| `io.insert-koin:koin-android` | 4.2.2 | latest stable | 2026-11-08 |
| `com.github.Stericson:RootTools` (JitPack) | 5.0 | latest stable | 2026-11-08 |
| RootShell (`app/libs/RootShell.aar`) | 1.6 | held: JitPack lists `com.github.Stericson:RootShell:1.6` but serves no AAR or JAR for it, so the build keeps the 1.6 AAR in the repository (SHA-256 `02d1a560fcaa1f803a6179da8be4b0fab3f8ea5bbcf168998f0f305b48cadc45`), which equals the GitHub release asset `RootShell.aar` of RootShell 1.6, 2017-11-26 | 2026-11-08 |

## Quality tools

| Dependency | Version | Resolved as | Checked on |
|---|---|---|---|
| detekt (`io.gitlab.arturbosch.detekt`) | 1.23.8 | latest stable; detekt 2 has no stable release | 2026-11-08 |
| ktlint Gradle plugin (`org.jlleitschuh.gradle.ktlint`) | 14.2.0 | latest stable | 2026-11-08 |
| ktlint | 1.8.0 | latest stable | 2026-11-08 |

## Test libraries

| Dependency | Version | Resolved as | Checked on |
|---|---|---|---|
| `junit:junit` | 4.13.2 | latest stable | 2026-11-08 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | latest stable | 2026-11-08 |
| `org.robolectric:robolectric` | 4.17 | latest stable; its default test API is 35, the newest it runs on JDK 17 (36 and 37 need JDK 21) | 2026-11-08 |
| `androidx.arch.core:core-testing` | 2.2.0 | latest stable | 2026-11-08 |
| `androidx.test.ext:junit` | 1.3.0 | latest stable | 2026-11-08 |
| `androidx.test.espresso:espresso-core` | 3.7.0 | latest stable | 2026-11-08 |
