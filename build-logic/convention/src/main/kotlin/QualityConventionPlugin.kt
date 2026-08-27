import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jlleitschuh.gradle.ktlint")
            apply("io.gitlab.arturbosch.detekt")
        }

        extensions.configure<KtlintExtension> {
            android.set(true)
            ignoreFailures.set(false)
        }

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            basePath = rootProject.projectDir.absolutePath
            parallel = true
        }
    }
}
