plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun keychainPassword(account: String): String? = runCatching {
    val proc = ProcessBuilder(
        "security",
        "find-generic-password",
        "-a",
        account,
        "-w",
    ).redirectErrorStream(false).start()
    val output = proc.inputStream.bufferedReader().readText().trim()
    if (proc.waitFor() == 0 && output.isNotEmpty()) output else null
}.getOrNull()

android {
    namespace = "express.pickup"
    compileSdk = 35

    defaultConfig {
        applicationId = "express.pickup"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val envStore = System.getenv("RELEASE_STORE_FILE")
            val defaultStore = file("${System.getProperty("user.home")}/keys")
                .listFiles { f -> f.isFile && f.extension == "jks" }
                ?.firstOrNull { keychainPassword("KEY_STORE_PASSWORD__${it.absolutePath}") != null }
            val resolvedStore = when {
                !envStore.isNullOrBlank() -> file(envStore)
                defaultStore?.isFile == true -> defaultStore
                else -> null
            }
            if (resolvedStore != null && resolvedStore.isFile) {
                val path = resolvedStore.absolutePath
                val alias = System.getenv("RELEASE_KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "key0"
                val sPass = System.getenv("RELEASE_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: keychainPassword("KEY_STORE_PASSWORD__$path")
                val kPass = System.getenv("RELEASE_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: keychainPassword("KEY_PASSWORD__${path}__$alias")
                if (!sPass.isNullOrEmpty() && !kPass.isNullOrEmpty()) {
                    storeFile = resolvedStore
                    storePassword = sPass
                    keyAlias = alias
                    keyPassword = kPass
                }
            }
        }
    }

    buildTypes {
        debug {
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        release {
            isMinifyEnabled = false
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.10.1")

    testImplementation("junit:junit:4.13.2")
}
