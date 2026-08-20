import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
}

android {

    namespace = "com.example.filesystemv2"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.filesystemv2"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date())
        val fileName = "${rootProject.name}_v${versionName}_${versionCode}_${formattedDate}"
        base.archivesName.set(fileName)
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_23
        targetCompatibility = JavaVersion.VERSION_23
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}