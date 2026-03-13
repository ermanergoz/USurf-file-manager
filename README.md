# USurf File Manager

[<img src="https://github.com/ermanergoz/USurf/blob/master/resources/google-play-badge.png" width="200">](https://play.google.com/store/apps/details?id=com.erman.usurf)

USurf File Manager is a simple open sourced Android file manager application that lets users browse their files, find their downloads, manage their storage space, move things around,and a lot more. The goal of this application is to provide trustable service to the users without any shady background operations and advertisement-free user experience.

<p float="center">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss1.png" height="500">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss2.png" height="500">
	<img src="https://github.com/ermanergoz/USurf/blob/master/resources/ss3.png" height="500">
</p>

## Features

- Basic features like cut, copy, delete etc.
- File transfer over WI-FI using FTP.
- Device-wide, keyword based file search.
- Support to copy–paste multiple files to multiple places multiple times.
- Access to root files if the device is rooted.
- Integrated file compression feature. Supported formats are: 7z, BZIP2, GZIP, TAR, WIM, XZ, ZIP. (Can extract RAR format but can't create)
- Support to add / remove shortcuts to home screen.
- Navigation drawer for easy navigation.
- Dark theme support.
- No ads or In-app purchases.

## Build & Run

- Clone git repository:

	```sh
	git clone https://github.com/ermanergoz/USurf-file-manager.git
	```

- Import the project into Android Studio:
	- Open Android Studio. After that Click on “Open an existing Android Studio project”. If Android Studio is already open, click on File and then Click on Open.
	- Select the location of the cloned repository and then Click OK.
	- After Gradle operations are finished, the application will be ready to run.

## Application Architecture & Dependencies

- [MVVM architecture](https://developer.android.com/jetpack/guide)
- [Data binding](https://developer.android.com/topic/libraries/data-binding)
- [Navigation component](https://developer.android.com/guide/navigation)
- [Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)
- [Coroutines](https://developer.android.com/kotlin/coroutines)
- [Apache FTP server](https://mina.apache.org/ftpserver-project/)
- [Apache MINA](https://mina.apache.org/)
- [Realm](https://realm.io/)
- [RootTools](https://github.com/Stericson/RootTools)
- [AndroidP7zip](https://github.com/hzy3774/AndroidP7zip)

## Meta

Yusuf Erman ERGÖZ – erman.ergoz@gmail.com

Distributed under the MIT license. See ``LICENSE`` for more information.

[https://github.com/ermanergoz](https://github.com/ermanergoz)

## Acknowledgements

- [Flaticon](https://www.flaticon.com/)

<!--
## Contributing

1. Fork it (<https://github.com/yourname/yourproject/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Commit your changes (`git commit -am 'Add some fooBar'`)
4. Push to the branch (`git push origin feature/fooBar`)
5. Create a new Pull Request
-->
