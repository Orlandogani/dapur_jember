plugins {
    id("dapurjember.android.application")
    id("dapurjember.android.application.compose")
    id("dapurjember.android.hilt")
}

android {
    namespace = "com.leanecorps.dapurjember"

    defaultConfig {
        applicationId = "com.leanecorps.dapurjember"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:menu"))
    implementation(project(":feature:floor"))
    implementation(project(":feature:order"))
    implementation(project(":feature:payment"))
    implementation(project(":feature:shift"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.navigation.compose)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
