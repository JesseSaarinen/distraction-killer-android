plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.jessesaarinen.distractionkiller"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.jessesaarinen.distractionkiller"
        minSdk = 26
        targetSdk = 35
        // GitHub Actions sets GITHUB_RUN_NUMBER, so every CI build has a higher
        // version than the one before and installs over it.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "0.1." + (System.getenv("GITHUB_RUN_NUMBER") ?: "dev")
    }

    signingConfigs {
        // One shared key for every build, so new APKs install over old ones
        // without losing the log or the Accessibility setting.
        // The key is public on purpose: this app is a sideloaded course demo.
        create("demo") {
            storeFile = file("demo.keystore")
            storePassword = "distractionkiller"
            keyAlias = "demo"
            keyPassword = "distractionkiller"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("demo")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("demo")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // CI runs lint. The findings go to the CI log, so they can be read without Android Studio.
        textReport = true
        textOutput = file("stdout")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Unit tests for the plain Kotlin logic (detection, timer, stats). They run on the computer, not the phone.
    testImplementation(libs.junit)
}
