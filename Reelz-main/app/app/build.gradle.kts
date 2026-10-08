import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.donk.app"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.donk.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 26
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val buildStamp = SimpleDateFormat("yyMMdd_HHmm").format(Date())

afterEvaluate {
    tasks.named("assembleDebug") {
        doLast {
            val dir = layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            val src = File(dir, "app-debug.apk")

            if (src.exists()) {
                val verCode = android.defaultConfig.versionCode
                src.copyTo(File(dir, "Reelz_ver${verCode}.apk"), overwrite = true)
                src.copyTo(File(dir, "Reelz_ver${verCode}_$buildStamp.apk"), overwrite = true)
            }
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)

    // Media3 / ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Glide for image and video thumbnails
    implementation(libs.glide)
}




