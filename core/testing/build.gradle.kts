plugins {
    id("dapurjember.android.library")
}

android {
    namespace = "com.leanecorps.dapurjember.core.testing"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:domain"))
    api(project(":core:data"))

    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
    api(libs.junit)
    api(libs.junit.jupiter)
    api(libs.robolectric)
    api(libs.androidx.test.core)
    api(libs.androidx.room.testing)
}
