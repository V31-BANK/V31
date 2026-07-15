plugins {
    id("org.springframework.boot") version "4.1.0" apply false
}

allprojects {
    group = "org.v31bank"
    version = "1.0-SNAPSHOT"
    description = "V31 Crypto Bank"
}

subprojects {
    plugins.withType<JavaPlugin> {
        dependencies {
            add("implementation", platform(project(":framework-platform")))
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
