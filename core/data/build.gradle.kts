plugins {
    id("dapurjember.android.library")
    id("dapurjember.android.hilt")
    id("dapurjember.android.room")
}

android {
    namespace = "com.leanecorps.dapurjember.core.data"

    testOptions.unitTests.isIncludeAndroidResources = true // Robolectric

    // Exported Room schemas, so MigrationTestHelper can validate migrations under Robolectric.
    sourceSets.getByName("test").assets.srcDir(layout.projectDirectory.dir("schemas"))
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    implementation(libs.bcrypt)

    // Nightly backup worker (FR-D3).
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(project(":core:testing"))
}
