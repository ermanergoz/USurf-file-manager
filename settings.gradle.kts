pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            // Realm publishes its Gradle plugin without a plugin marker, so the plugin id is mapped to its artifact here.
            if (requested.id.id == "realm-android") {
                useModule("io.realm:realm-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") {
            // RootTools is the only dependency served from JitPack.
            content {
                includeGroup("com.github.Stericson")
            }
        }
    }
}

rootProject.name = "Usurf"
include(":app")
