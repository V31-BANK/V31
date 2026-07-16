plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))

    constraints {
        api("com.google.guava:guava:33.6.0-jre")
    }
}
