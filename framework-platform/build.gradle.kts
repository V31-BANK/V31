plugins {
    id("java-platform")
}

dependencies {
    constraints {
        api("org.springframework:spring-core:6.1.10")
        api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
        api("org.apache.commons:commons-lang3:3.14.0")
    }
}
