plugins {
    id("com.android.application")
    // Remove Google Services if not using Firebase features
    // id("com.google.gms.google-services")
}

android {
    namespace = "com.example.languatranslator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.languatranslator"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Fix for conflicting native libraries
    packaging {
        resources {
            pickFirsts += "lib/arm64-v8a/libtranslate_jni.so"
            pickFirsts += "lib/armeabi-v7a/libtranslate_jni.so"
            pickFirsts += "lib/x86/libtranslate_jni.so"
            pickFirsts += "lib/x86_64/libtranslate_jni.so"
            pickFirsts += "**/libc++_shared.so"
            pickFirsts += "**/libjsc.so"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // ONLY use ML Kit Translate (do not mix with Firebase ML)
    implementation("com.google.mlkit:translate:17.0.3")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // REMOVED all Firebase ML dependencies to avoid conflicts:
    // implementation("com.google.firebase:firebase-core:21.1.1")
    // implementation("com.google.firebase:firebase-ml-natural-language-language-id-model:20.0.8")
    // implementation("com.google.firebase:firebase-ml-natural-language-translate-model:20.0.9")
    // implementation("com.google.firebase:firebase-ml-natural-language:22.0.1")
}