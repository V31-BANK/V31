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
    id("org.v31bank.deployed")
}

description = "V31 Data JPA auto-configuration"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    api("org.springframework.boot:spring-boot")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.data:spring-data-jpa")
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.hibernate.orm:hibernate-core")

    // Required by `@EnableJpaAuditing`, whose registrar installs a bean-configurer
    // aspect. Declared here rather than left to the starter: an auto-configuration
    // that cannot start on the dependencies its own module declares is one that
    // fails for whoever depends on the module directly.
    api("org.springframework:spring-aspects")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // A real persistence unit, so the auditing this module configures is asserted
    // by what reaches a row rather than by which beans exist.
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testRuntimeOnly("com.h2database:h2")
}
