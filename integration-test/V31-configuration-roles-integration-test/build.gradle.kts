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
    java
    id("org.v31bank.integration-test")
}

description = "Runs a real build against the configuration roles V31's plugins declare"

dependencies {
    intTestImplementation("org.junit.jupiter:junit-jupiter")
    intTestImplementation("org.assertj:assertj-core")
    intTestImplementation(gradleTestKit())
}

// The plugins under test live in buildSrc, which is a build of its own. A generated
// consumer includes it the way any build would include a plugin build.
val buildLogic = rootProject.layout.projectDirectory.dir("buildSrc")

tasks.named<Test>("intTest") {
    inputs.dir(buildLogic).withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("buildLogic")
    systemProperty("buildLogic", buildLogic.asFile.absolutePath)
}
