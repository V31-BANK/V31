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

description = "V31 jOOQ auto-configuration"

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    api("org.springframework.boot:spring-boot")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-jooq")
    api("org.jooq:jooq")
    api(project(":library:V31-core"))

    // Not referenced in code: javac reads the JAXB annotations on
    // `org.jooq.conf.Settings` when resolving the `DefaultConfiguration.set(...)`
    // the tests call, and jOOQ leaves that API optional so it never arrives
    // transitively. An enum constant whose type is absent is a warning, fatal
    // under -Werror. Test compile only: nothing here binds XML at runtime.
    testCompileOnly("jakarta.xml.bind:jakarta.xml.bind-api")

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
