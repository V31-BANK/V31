subprojects {
    when {
        name.endsWith("-service") -> {
            apply(plugin = "java")
        }

        name.endsWith("-contract") -> {
            apply(plugin = "java-library")
        }
    }

    plugins.withId("org.springframework.boot") {
        dependencies {
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }
    }
}
