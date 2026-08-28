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

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
