description = "V31 Data Valkey auto-configuration"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    api("org.springframework.boot:spring-boot")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-data-redis")
    api("org.springframework.data:spring-data-redis")
    api("org.springframework.boot:spring-boot-cache")
    api("tools.jackson.core:jackson-databind")
    api(project(":library:V31-core"))

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
}
