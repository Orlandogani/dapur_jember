plugins {
    id("dapurjember.jvm.library")
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:common"))
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.mockk)
    api(libs.junit.jupiter)
}
