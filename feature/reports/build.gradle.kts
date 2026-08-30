plugins {
    id("dapurjember.android.feature")
}

android {
    namespace = "com.leanecorps.dapurjember.feature.reports"
}

dependencies {
    // FileProvider, for handing an exported CSV to the share sheet (FR-R3).
    implementation(libs.androidx.core.ktx)
}
