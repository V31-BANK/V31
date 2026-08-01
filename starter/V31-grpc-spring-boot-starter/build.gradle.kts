description = "V31 gRPC starter"

dependencies {
    api(project(":module:V31-grpc-spring-boot"))
    api("org.springframework.boot:spring-boot-starter-grpc-server")
    api("org.springframework.boot:spring-boot-starter-grpc-client")
}
