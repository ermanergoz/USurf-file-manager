# USurf File Manager

[<img src="https://github.com/ermanergoz/USurf/blob/master/resources/google-play-badge.png" width="200">](https://play.google.com/store/apps/details?id=com.erman.usurf)

A lightweight, open-source Android file manager with built-in archive compression, root explorer, and wireless file transfer — no ads, no tracking, no in-app purchases.

<p float="center">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss1.png" height="500">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss2.png" height="500">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss3.png" height="500">
</p>

## Why USurf?

**Powerful file compression** — Compress and extract archives directly on your device. Supports 7z, BZIP2, GZIP, TAR, WIM, XZ, and ZIP formats. Can also extract RAR. Powered by 7-Zip built from source via NDK.

**Root file explorer** — Browse and manage root directories on rooted devices. One of the few open-source file managers that still supports root access.

**Wireless file transfer** — Share your phone's storage with your computer over Wi-Fi using the built-in FTP server. No cables, no third-party apps needed. Sign in with the username and password shown on the FTP screen; a random password is generated on first launch and anonymous login is not allowed. The share is read-only until you turn on **Allow uploads**, which lets the computer add, change and delete files.

**100% free and open source** — No ads, no analytics, no in-app purchases, and no data collection. The entire source code is available for anyone to audit, build, and contribute to.

## Features

- Copy, move, rename, delete, and share files
- Compress and extract archives (7z, BZIP2, GZIP, TAR, WIM, XZ, ZIP; RAR extract only)
- Root file explorer for rooted devices
- Built-in FTP server for wireless file transfer, password protected and read-only unless uploads are allowed
- Device-wide keyword-based file search
- Copy and paste files to multiple destinations
- Favorite folders for quick access
- Image and video thumbnail previews
- Dark theme support
- SD card support

## Build & Run

Clone and open in Android Studio:

```sh
git clone https://github.com/ermanergoz/USurf-file-manager.git
```

Open the project in Android Studio, wait for Gradle sync to complete, and run.

### 7-Zip version

The archive engine is the upstream [7-Zip source](https://github.com/ip7z/7zip), included as a git submodule at `app/src/main/cpp/7zip` and pinned to tag `26.06` (run `git submodule update --init` after cloning). The pin must stay at 26.02 or later: 25.01 changed how symbolic links are handled during extraction (CVE-2025-55188), and 26.02 fixed a heap buffer overflow in XZ decompression (CVE-2026-14266). USurf also extracts with the `-snl-` switch, so no archive can create a symbolic link on the device, whatever it contains, and with the `-spod` switch (added in 26.01), so a `*` in an archive's name or folder is kept as it is.

## Architecture & Dependencies

- [MVVM architecture](https://developer.android.com/jetpack/guide)
- [Data binding](https://developer.android.com/topic/libraries/data-binding)
- [Navigation component](https://developer.android.com/guide/navigation)
- [Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)
- [Coroutines](https://developer.android.com/kotlin/coroutines)
- [Apache FTP server](https://mina.apache.org/ftpserver-project/)
- [Apache MINA](https://mina.apache.org/)
- [Realm](https://realm.io/)
- [RootTools](https://github.com/Stericson/RootTools)
- [7-Zip](https://www.7-zip.org/) (built from source via NDK)
- [Glide](https://github.com/bumptech/glide)
- [Koin](https://insert-koin.io/)

## License

Distributed under the MIT license. See `LICENSE` for more information.

USurf includes third-party software under its own licenses, listed in `THIRD_PARTY.md` and on the app's Information
screen. The archive engine is 7-Zip by Igor Pavlov, under the GNU LGPL 2.1 or later (`app/src/main/cpp/7zip/DOC/License.txt`);
since the whole app is open source, it can be rebuilt with a modified 7-Zip. Its RAR decoder was developed using source
code of the unRAR program by Alexander Roshal, whose license does not allow that code to be used to develop a RAR
(WinRAR) compatible archiver.

## Author

Yusuf Erman ERGÖZ — erman.ergoz@gmail.com

[github.com/ermanergoz](https://github.com/ermanergoz)

## Acknowledgements

- [Flaticon](https://www.flaticon.com/)
