pluginManagement {
    plugins {
        kotlin("jvm") version "1.9.0"
    }

}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "intellij-autocomplete-plugin"

includeBuild("libs/completion")