plugins {
    kotlin("multiplatform") version "1.3.50"
}

repositories {
    mavenCentral()
}

kotlin {
    macosX64("snake") {
        sourceSets["snakeMain"].kotlin.srcDir("src")
        sourceSets["snakeTest"].kotlin.srcDir("test")
        binaries {
            executable(buildTypes = setOf(DEBUG)) {
                entryPoint = "main"
            }
        }
        val main by compilations.getting
        val interop by main.cinterops.creating {
            defFile(project.file("sdl.def"))
            packageName("sdl")
            includeDirs(
                "/Library/Frameworks/SDL2.framework/Headers"
            )
        }
    }
}
