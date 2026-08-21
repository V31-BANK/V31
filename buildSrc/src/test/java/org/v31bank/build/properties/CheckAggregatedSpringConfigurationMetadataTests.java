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

package org.v31bank.build.properties;

import java.nio.file.Path;

import org.gradle.api.Project;
import org.gradle.api.tasks.VerificationException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CheckAggregatedSpringConfigurationMetadata}.
 * <p>
 * The replacement usually lives in another module, which is why no module can ask this
 * question alone and why each test here hands the task more than one file.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckAggregatedSpringConfigurationMetadataTests {

	@TempDir
	private Path directory;

	@Test
	void passesWhenTheReplacementExistsInAnotherModule() {
		MetadataFiles.metadata().deprecated("v31.old", "v31.new", "0.1.0").writeTo(module("one"));
		MetadataFiles.metadata().property("v31.new", "The one to use.").writeTo(module("two"));
		CheckAggregatedSpringConfigurationMetadata task = task();
		assertThatCode(task::check).doesNotThrowAnyException();
		assertThat(report()).content().contains("No problems found.");
	}

	@Test
	void failsWhenTheReplacementExistsNowhere() {
		MetadataFiles.metadata().deprecated("v31.old", "v31.never", "0.1.0").writeTo(module("one"));
		MetadataFiles.metadata().property("v31.new", "The one to use.").writeTo(module("two"));
		CheckAggregatedSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining("aggregated Spring configuration metadata");
		assertThat(report()).content()
			.contains("The following properties have a replacement that does not exist:")
			.contains("v31.old (replacement v31.never)");
	}

	@Test
	void saysNothingAboutADeprecationThatOffersNoReplacement() {
		MetadataFiles.metadata().deprecated("v31.old", null, "0.1.0").writeTo(module("one"));
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	private CheckAggregatedSpringConfigurationMetadata task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckAggregatedSpringConfigurationMetadata task = project.getTasks()
			.register("checkAggregatedSpringConfigurationMetadata", CheckAggregatedSpringConfigurationMetadata.class)
			.get();
		task.getConfigurationPropertyMetadata().from(project.fileTree(this.directory.resolve("modules").toFile()));
		task.getReportLocation().set(report().toFile());
		return task;
	}

	private Path module(String name) {
		return this.directory.resolve("modules").resolve(name + ".json");
	}

	private Path report() {
		return this.directory.resolve("build/reports/aggregated-spring-configuration-metadata/check.txt");
	}

}
