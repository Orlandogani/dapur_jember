plugins {
    id("dapurjember.android.library.compose")
}

android {
    namespace = "com.leanecorps.dapurjember.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
