subprojects {
    when {
        name.endsWith("-service") -> {
            apply(plugin = "java")
            apply(plugin = "org.springframework.boot")
        }

        name.endsWith("-contract") -> {
            apply(plugin = "java-library")
        }
    }

    plugins.withId("org.springframework.boot") {
        dependencies {
            add("implementation", "org.springframework.boot:spring-boot-starter-webmvc")
        }
    }
}
