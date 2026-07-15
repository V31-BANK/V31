plugins {
    id("org.springframework.boot") version "3.3.2" apply false
}

allprojects {
    group = "org.v31bank"
    version = "1.0-SNAPSHOT"
    description = "V31 Crypto Bank"
}

subprojects {
    plugins.withType<JavaPlugin> {
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
