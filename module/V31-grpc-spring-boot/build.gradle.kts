/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    `java-library`
    id("org.v31bank.auto-configuration")
    //id("org.v31bank.configuration-properties")
}

description = "V31 gRPC auto-configuration"

dependencies {
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
    testImplementation("org.junit.jupiter:junit-jupiter")
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
