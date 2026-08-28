plugins {
    id("dapurjember.android.library")
    id("dapurjember.android.hilt")
    id("dapurjember.android.room")
}

android {
    namespace = "com.leanecorps.dapurjember.core.data"

    testOptions.unitTests.isIncludeAndroidResources = true // Robolectric
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)

    testImplementation(project(":core:testing"))
}
