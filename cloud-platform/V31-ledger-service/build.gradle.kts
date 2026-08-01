plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(project(":library:V31-core"))
    implementation(project(":starter:V31-data-jpa-spring-boot-starter"))
    implementation(project(":starter:V31-grpc-spring-boot-starter"))
    implementation(project(":apis:V31-ledger-api"))
    implementation("org.springframework.boot:spring-boot-flyway")

    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
}
