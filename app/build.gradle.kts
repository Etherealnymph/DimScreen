plugins {
    id("com.android.application")
}

android {
    namespace = "com.etherealnymph.dim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.etherealnymph.dim"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// 这个应用刻意不引入任何第三方运行时依赖：锁屏、磁贴、悬浮球全部基于
// Android framework，安装包保持在几十 KB，也方便离线构建。
