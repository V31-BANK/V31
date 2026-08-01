description = "V31 gRPC auto-configuration"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    api("org.springframework.boot:spring-boot")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-grpc-server")
    api("org.springframework.boot:spring-boot-grpc-client")
    api("io.grpc:grpc-api")
    api("org.slf4j:slf4j-api")

    // The HTTP entry point is where a request's context first appears, so the
    // filter that reads it lives here. Optional: a service that speaks gRPC alone
    // has no servlet container and the auto-configuration backs off.
    compileOnly("org.springframework:spring-web")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    api(project(":library:V31-core"))

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("io.grpc:grpc-stub")
    testImplementation("io.grpc:grpc-protobuf")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-web")
    testImplementation("jakarta.servlet:jakarta.servlet-api")

    // MDC is a no-op without an SLF4J provider, so the request-id fallback would
    // silently do nothing. Every Spring Boot application has one; the tests need
    // one too for the propagation they assert to be real.
    testRuntimeOnly("ch.qos.logback:logback-classic")
}
