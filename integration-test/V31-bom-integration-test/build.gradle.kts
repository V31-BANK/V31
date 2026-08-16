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

import org.gradle.language.base.plugins.LifecycleBasePlugin

plugins {
    java
    id("org.v31bank.integration-test")
}

description = "Resolves V31 from a repository the way a consumer does"

// Every project whose artifacts a consumer can name, listed rather than derived. The
// BOM names the same set by hand; listing it again here is what makes the two
// disagreeing show up as a failing test instead of as a consumer's problem.
val v31Repository = configurations.create("v31Repository")

dependencies {
    for (path in listOf(
        ":platform:V31-dependencies",
        ":apis:V31-cbs-api",
        ":apis:V31-compliance-api",
        ":apis:V31-customer-api",
        ":apis:V31-ledger-api",
        ":apis:V31-notification-api",
        ":apis:V31-risk-api",
        ":apis:V31-transfer-api",
        ":apis:V31-wallet-api",
        ":library:V31-core",
        ":module:V31-data-jpa-spring-boot",
        ":module:V31-data-valkey-spring-boot",
        ":module:V31-grpc-spring-boot",
        ":module:V31-jooq-spring-boot",
        ":module:V31-web-spring-boot",
        ":starter:V31-data-jpa-spring-boot-starter",
        ":starter:V31-data-valkey-spring-boot-starter",
        ":starter:V31-grpc-spring-boot-starter",
        ":starter:V31-jooq-spring-boot-starter",
        ":starter:V31-web-spring-boot-starter",
    )) {
        v31Repository(project(mapOf("path" to path, "configuration" to "mavenRepository")))
    }

	add(
		"implementation",
		project(
			mapOf(
				"path" to ":starter:V31-data-jpa-spring-boot-starter",
				"configuration" to "starterMetadata"
			)
		)
	)

    intTestImplementation("org.junit.jupiter:junit-jupiter")
    intTestImplementation("org.assertj:assertj-core")
    intTestImplementation(gradleTestKit())
}

val v31MavenRepository = tasks.register<Sync>("v31MavenRepository") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Gathers the published V31 artifacts into one repository."
    from(v31Repository)
    into(layout.buildDirectory.dir("v31-maven-repository"))
}

tasks.named<Test>("intTest") {
    inputs.files(v31MavenRepository)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("v31MavenRepository")
    systemProperty("v31.repository", layout.buildDirectory.dir("v31-maven-repository").get().asFile.absolutePath)
    systemProperty("v31.version", project.version.toString())
}
