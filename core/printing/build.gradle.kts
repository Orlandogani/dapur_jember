plugins {
    id("dapurjember.android.library")
    id("dapurjember.android.hilt")
}

android {
    namespace = "com.leanecorps.dapurjember.core.printing"

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(project(":core:testing"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
