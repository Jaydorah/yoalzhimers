plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jaydorah.alzhimers"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jaydorah.alzhimers"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (streak dates) needs desugaring below API 26; minSdk is 24.
        isCoreLibraryDesugaringEnabled = true
    }
    // The in-app language picker can select languages the device doesn't have installed,
    // so every locale's resources must ship in the base APK instead of Play language splits.
    bundle {
        language {
            enableSplit = false
        }
    }
    buildFeatures {
        compose = true
    }
}

// Android's java.util.Locale maps "he" to the legacy code "iw" on many versions, so a device or
// in-app setting of Hebrew can look up values-iw, miss values-he, and fall back to English while
// still laying out RTL. Generate values-iw from values-he so both codes resolve to the same strings.
abstract class HebrewLegacyResTask : DefaultTask() {
    @get:InputDirectory
    abstract val hebrewDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile.resolve("values-iw")
        out.deleteRecursively()
        hebrewDir.get().asFile.copyRecursively(out)
    }
}

val hebrewLegacyRes = tasks.register<HebrewLegacyResTask>("generateHebrewLegacyRes") {
    hebrewDir.set(layout.projectDirectory.dir("src/main/res/values-he"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(hebrewLegacyRes, HebrewLegacyResTask::outputDir)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}