plugins {
    kotlin("multiplatform") version "1.3.21"
}

repositories {
    mavenCentral()
}

kotlin {
    macosX64("snake") {
        compilations.getByName("main") {
            val ncursesInterop by cinterops.creating {
                defFile("ncurses.def")
                packageName("ncurses")
            }
        }
        binaries {
            executable {
                entryPoint = "snake.main"
            }
        }
    }
}
