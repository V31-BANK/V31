plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))

    // Spring Boot 4.1.0 pins gRPC at 1.80.0, but the Spring gRPC 1.1.0 it ships
    // with is built against 1.81.0 and pulls that in for the artifacts it depends
    // on. Anything else — grpc-inprocess, grpc-stub — would stay on 1.80.0 and
    // fail at runtime against the newer core, since the internal transport
    // interfaces changed between the two. Importing the gRPC BOM settles every
    // artifact on one version.
    api(platform("io.grpc:grpc-bom:1.81.0"))

    constraints {
        api("com.google.guava:guava:33.6.0-jre")
    }
}
