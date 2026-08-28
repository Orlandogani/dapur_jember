import com.leanecorps.dapurjember.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("dapurjember.android.library")
            apply("dapurjember.android.library.compose")
            apply("dapurjember.android.hilt")
        }

        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:common"))
            add("implementation", project(":core:designsystem"))
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())

            // :core:testing api-exposes JUnit5, Turbine, MockK, coroutines-test, Robolectric.
            add("testImplementation", project(":core:testing"))
            add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
