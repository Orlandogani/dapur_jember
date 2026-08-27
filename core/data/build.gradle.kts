plugins {
    id("dapurjember.android.library")
    id("dapurjember.android.hilt")
    id("dapurjember.android.room")
}

android {
    namespace = "com.leanecorps.dapurjember.core.data"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)
}
