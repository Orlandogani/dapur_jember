import com.leanecorps.dapurjember.buildlogic.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.jvm")
            apply("java-library")
            apply("dapurjember.quality")
        }

        configureKotlinJvm()

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
