plugins {
    id("com.android.application")
}

android {
    namespace = "com.aunt13psychot1c.shredder"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aunt13psychot1c.shredder"
        minSdk = 24
        targetSdk = 36
        // The release robot passes these in; local builds fall back to the dev values.
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0-dev"
    }

    // Signed with the permanent key when the four signing secrets are present, so every
    // release installs right over the last one.
    signingConfigs {
        val ksFile = System.getenv("KEYSTORE_FILE")
        val ksPassword = System.getenv("KEYSTORE_PASSWORD")
        val ksAlias = System.getenv("KEY_ALIAS")
        val ksKeyPassword = System.getenv("KEY_PASSWORD")
        if (ksFile != null && ksPassword != null && ksAlias != null && ksKeyPassword != null) {
            create("release") {
                storeFile = file(ksFile)
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
        // Test build for pull requests: installs next to the real one, needs no secrets.
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
