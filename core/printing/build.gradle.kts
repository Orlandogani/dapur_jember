plugins {
    id("dapurjember.android.library")
    id("dapurjember.android.hilt")
}

android {
    namespace = "com.leanecorps.dapurjember.core.printing"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)
}
