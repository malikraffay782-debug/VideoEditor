pluginManagement {
    repositories {
        google()
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

rootProject.name = "VideoEditor"

include(
    ":app",
    ":core",
    ":core-ui",
    ":media-engine",
    ":timeline",
    ":effects",
    ":audio-engine",
    ":3d-engine",
    ":ai-engine",
    ":ai-providers",
    ":project-manager",
    ":export-engine"
)
