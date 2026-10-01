import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.autumn.nyaclash"
    compileSdk = 36

    // Pinned so CI installs exactly what AGP uses (see ci.yml).
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.autumn.nyaclash"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        // Only arm64-v8a is targeted (see agent.md). The mihomo core is built for this ABI only.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        // Release signing is injected by CI via keystore.properties (see .github/workflows/release.yml).
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            val keystoreProperties = Properties().apply {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile") ?: "release.keystore")
                storeType = keystoreProperties.getProperty("storeType") ?: "PKCS12"
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Unsigned unless keystore.properties is present.
            signingConfig = signingConfigs.findByName("release")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleViewmodelCompose)
    implementation(libs.androidxActivityCompose)

    implementation(libs.composeRuntime)
    implementation(libs.composeUi)
    implementation(libs.composeUiGraphics)
    implementation(libs.composeUiToolingPreview)
    implementation(libs.composeAnimation)
    implementation(libs.composeFoundation)
    implementation(libs.composeFoundationLayout)
    implementation(libs.composeMaterial3)
    implementation(libs.composeMaterialIconsCore)

    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.okhttp)

    debugImplementation(libs.composeUiTooling)
}
