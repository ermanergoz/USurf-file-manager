import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.realm)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Not the android {} accessor: while gradle.properties keeps android.newDsl=false for Realm, that accessor is typed to the Android
// plugin's deprecated classic extension. ApplicationExtension is the plugin's current DSL, and the same object implements it.
configure<ApplicationExtension> {
    namespace = "com.erman.usurf"
    compileSdk = libs.versions.compileSdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        applicationId = "com.erman.usurf"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 15
        versionName = "2.2.0"
        // https://semver.org/spec/v2.0.0.html
        // major.minor.patch
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Vector drawables stay vectors on API 23. Without this the Android plugin writes PNG copies of ic_file, whose evenOdd
        // fill needs API 24, and the app's file icon on API 23 changes.
        vectorDrawables.useSupportLibrary = true
        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
                abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    testOptions {
        // The app's strings and plurals resolve in unit tests as they do on a device.
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
    }
}

kapt {
    correctErrorTypes = true
}

// Javac's unchecked and deprecation warnings, shown for the Java that DataBinding and Realm generate.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

// Lint reads the stubs kapt writes for every variant. In a build that runs both, lint starts once kapt is done, so it
// never reads a stub kapt is still writing.
tasks.matching { it.name.startsWith("lintAnalyze") }.configureEach {
    mustRunAfter(tasks.matching { it.name.startsWith("kapt") })
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    baseline = file("$rootDir/config/detekt/baseline.xml")
    parallel = true
}

ktlint {
    version = libs.versions.ktlint
    android = true
    ignoreFailures = false
    filter {
        exclude { it.file.path.contains("/generated/") }
        exclude { it.file.path.contains("/build/") }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.mina.core)
    implementation(libs.ftpserver.ftplet.api)
    implementation(libs.ftpserver.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.roottools) { isTransitive = false }
    implementation(libs.glide)
    implementation(libs.koin.android)
    implementation(files("libs/RootShell.aar"))
    ksp(libs.glide.ksp)
    kapt(libs.realm.annotations.processor)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
