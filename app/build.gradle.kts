import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Read release-signing credentials from local.properties (gitignored).
// See playstore/SIGNING.md for the keys expected here.
val localProps = Properties().also { props ->
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { props.load(it) }
}

android {
    namespace = "com.ninersudoku"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ninersudoku"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.0.4"
    }

    signingConfigs {
        create("release") {
            val storeFileProp = localProps.getProperty("RELEASE_STORE_FILE")
            if (!storeFileProp.isNullOrBlank()) {
                storeFile = file(storeFileProp)
                storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Ship native debug symbols so Play Console can symbolicate crashes/ANRs from
            // the one transitive native lib (libandroidx.graphics.path.so). FULL level is
            // the highest Play Console accepts; SYMBOL_TABLE produced no output because the
            // AndroidX .so ships pre-stripped. Small AAB size bump (usually <1 MB).
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation("com.google.android.material:material:1.12.0")
    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
