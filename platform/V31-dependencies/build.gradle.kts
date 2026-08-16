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
    `java-platform`
    id("org.v31bank.deployed")
}

description = "V31 Dependencies"

javaPlatform {
    allowDependencies()
}

val springBootVersion = providers.gradleProperty("springBootVersion").get()
// The one thing another build imports. It settles two sets of versions at once: V31's
// own artifacts, and the third-party libraries they were built and tested against. A
// consumer that imports it names an artifact without a version and gets a combination
// this build is known to work with.
//
// Adding a published project means adding it here too. Nothing checks that, so a
// module left out is only noticed when a consumer cannot resolve it.
dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    // Spring Boot 4.1.0 pins gRPC at 1.80.0, but the Spring gRPC 1.1.0 it ships
    // with is built against 1.81.0 and pulls that in for the artifacts it depends
    // on. Anything else — grpc-inprocess, grpc-stub — would stay on 1.80.0 and
    // fail at runtime against the newer core, since the internal transport
    // interfaces changed between the two. Importing the gRPC BOM settles every
    // artifact on one version.
    api(platform("io.grpc:grpc-bom:1.81.0"))

    constraints {
        api("com.google.guava:guava:33.6.0-jre")

        api(project(":apis:V31-cbs-api"))
        api(project(":apis:V31-compliance-api"))
        api(project(":apis:V31-customer-api"))
        api(project(":apis:V31-ledger-api"))
        api(project(":apis:V31-notification-api"))
        api(project(":apis:V31-risk-api"))
        api(project(":apis:V31-transfer-api"))
        api(project(":apis:V31-wallet-api"))

        api(project(":library:V31-core"))

        api(project(":module:V31-data-jpa-spring-boot"))
        api(project(":module:V31-data-valkey-spring-boot"))
        api(project(":module:V31-grpc-spring-boot"))
        api(project(":module:V31-jooq-spring-boot"))
        api(project(":module:V31-web-spring-boot"))

        api(project(":starter:V31-data-jpa-spring-boot-starter"))
        api(project(":starter:V31-data-valkey-spring-boot-starter"))
        api(project(":starter:V31-grpc-spring-boot-starter"))
        api(project(":starter:V31-jooq-spring-boot-starter"))
        api(project(":starter:V31-web-spring-boot-starter"))
    }
}
