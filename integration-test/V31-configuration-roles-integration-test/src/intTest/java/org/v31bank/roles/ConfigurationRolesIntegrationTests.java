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

package org.v31bank.roles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the roles V31's plugins declare actually refuse.
 * <p>
 * A real build is run rather than the roles being inspected, because a role is only worth
 * declaring if it stops a build that gets it wrong — and that is a thing only a build can
 * demonstrate.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ConfigurationRolesIntegrationTests {

	private static final String BUILD_LOGIC = System.getProperty("buildLogic");

	@TempDir
	private Path consumer;

	/**
	 * {@code optional} is a bucket: declaring into it is the whole point.
	 */
	@Test
	void aDependencyScopeTakesDeclarations() throws IOException {
		BuildResult result = build("""
				dependencies { optional("io.lettuce:lettuce-core:7.5.2.RELEASE") }
				tasks.register("probe") {
				    val declared = configurations.getByName("optional").allDependencies.map { it.name }
				    val onCompile = configurations.getByName("compileClasspath").allDependencies.map { it.name }
				    doLast {
				        println("optional        = " + declared)
				        println("compileClasspath= " + onCompile)
				    }
				}
				""", "probe");
		assertThat(result.getOutput()).contains("optional        = [lettuce-core]")
			.contains("compileClasspath= [lettuce-core]");
	}

	/**
	 * A bucket is not a thing you resolve; the classpaths that extend it are.
	 */
	@Test
	void aDependencyScopeRefusesToBeResolved() throws IOException {
		BuildResult result = buildAndFail("""
				tasks.register("probe") {
				    val files = configurations.getByName("optional").incoming.files
				    doLast { println(files.files) }
				}
				""", "probe");
		assertThat(result.getOutput()).contains("Resolving dependency configuration 'optional' is not allowed")
			.contains("canBeResolved=false");
	}

	/**
	 * The derived classpath resolves, and what it resolves is what the bucket was given.
	 */
	@Test
	void aResolvableTakesWhatTheBucketWasGiven() throws IOException {
		BuildResult result = build("""
				dependencies { optional("io.lettuce:lettuce-core:7.5.2.RELEASE") }
				tasks.register("probe") {
				    val derived = configurations.getByName("autoConfigurationOptionalClasspath")
				        .allDependencies.map { it.name }
				    doLast { println("derived = " + derived) }
				}
				""", "probe");
		assertThat(result.getOutput()).contains("derived = [lettuce-core]");
	}

	/**
	 * Declaring straight into the derived classpath would make the check disagree with
	 * what the module actually compiles against.
	 */
	@Test
	void aResolvableRefusesDeclarations() throws IOException {
		BuildResult result = buildAndFail("""
				dependencies { "autoConfigurationOptionalClasspath"("io.lettuce:lettuce-core:7.5.2.RELEASE") }
				""", "help");
		assertThat(result.getOutput())
			.contains("Dependencies can not be declared against the `autoConfigurationOptionalClasspath` configuration");
	}

	/**
	 * The metadata is offered to another project, which is what being consumable is for.
	 */
	@Test
	void aConsumableIsWhatAnotherProjectTakes() throws IOException {
		BuildResult result = build("""
				dependencies { }
				tasks.register("probe") {
				    val offered = configurations.getByName("autoConfigurationMetadata")
				    val consumable = offered.isCanBeConsumed
				    val resolvable = offered.isCanBeResolved
				    val artifacts = offered.artifacts.map { it.file.name }
				    doLast {
				        println("consumable=" + consumable + " resolvable=" + resolvable)
				        println("offers    =" + artifacts)
				    }
				}
				""", "probe");
		assertThat(result.getOutput()).contains("consumable=true resolvable=false")
			.contains("offers    =[auto-configuration-metadata.properties]");
	}

	/**
	 * The mistake this catches: a file offered outwards quietly growing dependencies that
	 * every collector would then drag in.
	 */
	@Test
	void aConsumableRefusesDeclarations() throws IOException {
		BuildResult result = buildAndFail("""
				dependencies { "autoConfigurationMetadata"("io.lettuce:lettuce-core:7.5.2.RELEASE") }
				""", "help");
		assertThat(result.getOutput())
			.contains("Dependencies can not be declared against the `autoConfigurationMetadata` configuration");
	}

	private BuildResult build(String buildScript, String task) throws IOException {
		return runner(buildScript, task).build();
	}

	private BuildResult buildAndFail(String buildScript, String task) throws IOException {
		return runner(buildScript, task).buildAndFail();
	}

	private GradleRunner runner(String buildScript, String task) throws IOException {
		Files.writeString(this.consumer.resolve("settings.gradle.kts"), """
				pluginManagement {
				    // buildSrc is a reserved build name, so the plugin build is included
				    // under one of its own.
				    includeBuild("%s") { name = "v31-build-logic" }
				}
				rootProject.name = "consumer"
				""".formatted(BUILD_LOGIC.replace("\\", "\\\\")));
		Files.writeString(this.consumer.resolve("build.gradle.kts"), """
				plugins {
				    `java-library`
				    id("org.v31bank.auto-configuration")
				    id("org.v31bank.optional-dependencies")
				}
				""" + buildScript);
		return GradleRunner.create().withProjectDir(this.consumer.toFile()).withArguments(task, "-q", "--stacktrace");
	}

}
