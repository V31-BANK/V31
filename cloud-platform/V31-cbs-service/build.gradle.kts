plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation(project(":library:V31-core"))
    implementation(project(":starter:V31-data-valkey-spring-boot-starter"))
}
