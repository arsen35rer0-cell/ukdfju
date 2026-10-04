plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

fun env(name: String, default: String): String {
    return System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
}

val generatedIconDir = layout.buildDirectory.dir("generated/launcher-icon/res")

android {
    namespace = "com.yaroslav.p2p.messenger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yaroslav.p2p.messenger"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val keystoreFile = rootProject.file("keystore/release.keystore")
    val hasReleaseKeystore = keystoreFile.exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = keystoreFile
                storePassword = env("KEYSTORE_PASSWORD", "android")
                keyAlias = env("KEY_ALIAS", "release")
                keyPassword = env("KEY_PASSWORD", "android")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.srcDir(generatedIconDir)
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/license.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/notice.txt"
            excludes += "/META-INF/INDEX.LIST"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.paho.mqtt)

    debugImplementation(libs.compose.ui.tooling)
}

val generateLauncherIcon by tasks.registering {
    outputs.dir(generatedIconDir)

    doLast {
        val resDir = generatedIconDir.get().asFile
        if (resDir.exists()) {
            resDir.deleteRecursively()
        }

        val valuesDir = resDir.resolve("values").apply { mkdirs() }
        val drawableDir = resDir.resolve("drawable").apply { mkdirs() }
        val mipmapAnyDpiDir = resDir.resolve("mipmap-anydpi-v26").apply { mkdirs() }

        valuesDir.resolve("ic_launcher_generated.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="ic_launcher_background">#0B1220</color>
            </resources>
            """.trimIndent(),
            Charsets.UTF_8
        )

        drawableDir.resolve("ic_launcher_foreground.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp"
                android:height="108dp"
                android:viewportWidth="108"
                android:viewportHeight="108">
                <path
                    android:fillColor="#7DD3FC"
                    android:pathData="M30,34 h48 a8,8 0 0 1 8,8 v18 a8,8 0 0 1 -8,8 h-28 l-14,12 v-12 h-6 a8,8 0 0 1 -8,-8 v-18 a8,8 0 0 1 8,-8 z" />
                <path
                    android:fillColor="#0B1220"
                    android:pathData="M42,48 h24 v4 h-24 z M42,56 h16 v4 h-16 z" />
            </vector>
            """.trimIndent(),
            Charsets.UTF_8
        )

        val adaptiveIcon = """
            <?xml version="1.0" encoding="utf-8"?>
            <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                <background android:drawable="@color/ic_launcher_background" />
                <foreground android:drawable="@drawable/ic_launcher_foreground" />
            </adaptive-icon>
        """.trimIndent()

        mipmapAnyDpiDir.resolve("ic_launcher.xml").writeText(adaptiveIcon, Charsets.UTF_8)
        mipmapAnyDpiDir.resolve("ic_launcher_round.xml").writeText(adaptiveIcon, Charsets.UTF_8)
    }
}

tasks.configureEach {
    if (name.contains("merge", true) && name.contains("resources", true)) {
        dependsOn(generateLauncherIcon)
    }
}
