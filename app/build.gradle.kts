plugins {
    id("com.android.application")
}

android {
    namespace = "com.bhakt.hwencodehook"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bhakt.hwencodehook"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    compileOnly(project(":xposed-stub"))
}
