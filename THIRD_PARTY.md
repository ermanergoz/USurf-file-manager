# Third-party software

USurf is distributed under the MIT license (`LICENSE`). It includes the software below, each under its own license.
The app lists the bundled components on its Information screen. Versions are the ones `docs/toolchain.md` records.

## Bundled in the app

| Component | Coordinates | Version | License | Notes |
|---|---|---|---|---|
| 7-Zip | `app/src/main/cpp/7zip` (github.com/ip7z/7zip) | 26.06 | GNU LGPL 2.1 or later; the RAR decoder also under the unRAR license restriction; the LZFSE and Zstandard decoders under BSD 3-Clause; xxHash64 under BSD 2-Clause | Built from source into `libp7zip.so`. The app's whole source is public, so the library can be rebuilt and relinked. Notice below. |
| AndroidX Core | `androidx.core:core-ktx` | 1.19.1 | Apache-2.0 | |
| AndroidX AppCompat | `androidx.appcompat:appcompat` | 1.8.0 | Apache-2.0 | |
| AndroidX Activity | `androidx.activity:activity` | 1.12.4 | Apache-2.0 | Called directly for edge-to-edge drawing and Back; the version AppCompat and Material resolve it to. |
| AndroidX Fragment | `androidx.fragment:fragment` | 1.9.1 | Apache-2.0 | Called directly for the dialogs and the Fragment Result API. |
| AndroidX ConstraintLayout | `androidx.constraintlayout:constraintlayout` | 2.2.2 | Apache-2.0 | |
| AndroidX RecyclerView | `androidx.recyclerview:recyclerview` | 1.4.0 | Apache-2.0 | |
| AndroidX SwipeRefreshLayout | `androidx.swiperefreshlayout:swiperefreshlayout` | 1.2.0 | Apache-2.0 | |
| AndroidX Navigation | `androidx.navigation:navigation-fragment-ktx`, `androidx.navigation:navigation-runtime-ktx`, `androidx.navigation:navigation-ui-ktx` | 2.9.8 | Apache-2.0 | |
| AndroidX ViewPager | `androidx.viewpager:viewpager` | 1.1.0 | Apache-2.0 | |
| AndroidX DocumentFile | `androidx.documentfile:documentfile` | 1.1.0 | Apache-2.0 | |
| AndroidX Lifecycle | `androidx.lifecycle:lifecycle-livedata-ktx` | 2.11.0 | Apache-2.0 | |
| AndroidX Preference | `androidx.preference:preference-ktx` | 1.2.1 | Apache-2.0 | |
| Material Components for Android | `com.google.android.material:material` | 1.14.0 | Apache-2.0 | |
| Kotlin standard library | `org.jetbrains.kotlin:kotlin-stdlib` | 2.4.21 | Apache-2.0 | Added by the Kotlin plugin. |
| Kotlin coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.11.0 | Apache-2.0 | |
| Apache MINA | `org.apache.mina:mina-core` | 2.2.10 | Apache-2.0 | |
| Apache FtpServer | `org.apache.ftpserver:ftplet-api`, `org.apache.ftpserver:ftpserver-core` | 1.2.1 | Apache-2.0 | |
| Realm Java | `io.realm:realm-android-library`, `io.realm:realm-annotations-processor` | 10.19.0 | Apache-2.0 | The library is added by the Realm Gradle plugin; the annotation processor runs at build time only. |
| Glide | `com.github.bumptech.glide:glide`, `com.github.bumptech.glide:ksp` | 5.0.9 | BSD 2-Clause; parts under Apache-2.0 and MIT | The KSP processor runs at build time only. |
| Koin | `io.insert-koin:koin-android` | 4.2.2 | Apache-2.0 | |
| RootTools | `com.github.Stericson:RootTools` | 5.0 | Apache-2.0 or GPL-2.0, at the user's choice; used under Apache-2.0 | From JitPack. |
| RootShell | `RootShell.aar` (`app/libs`) | 1.6 | Apache-2.0 or GPL-2.0, at the user's choice, as RootTools; used under Apache-2.0 | The 1.6 build of github.com/Stericson/RootShell, kept in the repository because JitPack serves no artifact for 1.6. SHA-256 `02d1a560fcaa1f803a6179da8be4b0fab3f8ea5bbcf168998f0f305b48cadc45`, which equals the GitHub release asset `RootShell.aar` of RootShell 1.6, 2017-11-26. |
| Material icons | `app/src/main/res/drawable` (`ic_add`, `ic_close`) | — | Apache-2.0 | The add and close glyphs of Google's Material icons, drawn as vector paths. |
| Flaticon icons | `app/src/main/res/drawable` | — | Flaticon license, free with attribution | Icons by Pixel perfect, Freepik, iconixar, Smashicons, Roundicons, Those Icons, Kiranshastry and xnimrodx; credited on the Information screen. |

## Used to build and test only

| Component | Coordinates | Version | License |
|---|---|---|---|
| JUnit 4 | `junit:junit` | 4.13.2 | EPL-1.0 |
| Kotlin coroutines test | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | Apache-2.0 |
| Robolectric | `org.robolectric:robolectric` | 4.17 | MIT |
| AndroidX Arch Core testing | `androidx.arch.core:core-testing` | 2.2.0 | Apache-2.0 |
| AndroidX Test JUnit extensions | `androidx.test.ext:junit` | 1.3.0 | Apache-2.0 |
| Espresso | `androidx.test.espresso:espresso-core` | 3.7.0 | Apache-2.0 |
| Gradle | wrapper | 9.9.0 | Apache-2.0 |
| Android Gradle plugin | `com.android.tools.build:gradle` | 9.5.1 | Apache-2.0 |
| Kotlin Gradle plugin | `org.jetbrains.kotlin:kotlin-gradle-plugin` | 2.4.21 | Apache-2.0 |
| KSP | `com.google.devtools.ksp:symbol-processing-gradle-plugin` | 2.3.13 | Apache-2.0 |
| Realm Gradle plugin | `io.realm:realm-gradle-plugin` | 10.19.0 | Apache-2.0 |
| Navigation Safe Args | `androidx.navigation:navigation-safe-args-gradle-plugin` | 2.9.8 | Apache-2.0 |
| detekt | `io.gitlab.arturbosch.detekt` | 1.23.8 | Apache-2.0 |
| ktlint and its Gradle plugin | `org.jlleitschuh.gradle.ktlint` | 1.8.0, plugin 14.2.0 | MIT |

## 7-Zip notice

7-Zip Copyright (C) 1999-2026 Igor Pavlov. 7-Zip is free software: it can be redistributed and modified under the
terms of the GNU Lesser General Public License, version 2.1 or (at your option) any later version
(https://www.gnu.org/licenses/lgpl-2.1.html). It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. Its full license is
`app/src/main/cpp/7zip/DOC/License.txt` (https://github.com/ip7z/7zip/blob/main/DOC/License.txt), and its source is at
https://github.com/ip7z/7zip.

The decompression engine for RAR archives was developed using source code of the unRAR program; all copyrights to the
original unRAR code are owned by Alexander Roshal. The license for the original unRAR code has the following
restriction:

> The unRAR sources cannot be used to re-create the RAR compression algorithm, which is proprietary. Distribution of
> modified unRAR sources in separate form or as a part of other software is permitted, provided that it is clearly
> stated in the documentation and source comments that the code may not be used to develop a RAR (WinRAR) compatible
> archiver.

The code may not be used to develop a RAR (WinRAR) compatible archiver.
