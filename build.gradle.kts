plugins {
    id("org.springframework.boot") version "4.1.0" apply false
}

description = "V31 Crypto Bank"

allprojects {
    group = "org.v31bank"
}

subprojects {
    plugins.withType<JavaPlugin> {
        dependencies {
            add("annotationProcessor", platform(project(":framework-platform")))
            add("implementation", platform(project(":framework-platform")))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }

        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(21)
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            options.release = 21
        }
    }
}
