pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DapurJember"

include(":app")

include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:printing")
include(":core:testing")

include(":feature:auth")
include(":feature:menu")
include(":feature:floor")
include(":feature:order")
include(":feature:payment")
include(":feature:shift")
include(":feature:reports")
include(":feature:inventory")
include(":feature:settings")
