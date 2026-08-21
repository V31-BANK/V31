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
 * Tests for {@link CheckSpringConfigurationMetadata}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckSpringConfigurationMetadataTests {

	@TempDir
	private Path directory;

	@Test
	void passesWhenEveryPropertySaysWhatItIsFor() {
		MetadataFiles.metadata().property("v31.a", "What a is for.").writeTo(metadataFile());
		CheckSpringConfigurationMetadata task = task();
		assertThatCode(task::check).doesNotThrowAnyException();
		assertThat(report()).content().contains("No problems found.");
	}

	@Test
	void failsWhenAPropertySaysNothing() {
		MetadataFiles.metadata().undescribed("v31.a").writeTo(metadataFile());
		CheckSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining("Spring configuration metadata")
			.withMessageContaining("check.txt");
		assertThat(report()).content().contains("The following properties have no description:").contains("- v31.a");
	}

	@Test
	void letsAnExcludedPropertySayNothing() {
		MetadataFiles.metadata().undescribed("v31.internal.a").writeTo(metadataFile());
		CheckSpringConfigurationMetadata task = task();
		task.getExclusions().add("v31.internal.*");
		assertThatCode(task::check).doesNotThrowAnyException();
	}

	/**
	 * Order and duplicates are the processor's business here, not a person's, so this
	 * check does not ask about them.
	 */
	@Test
	void saysNothingAboutOrderOrDuplicates() {
		MetadataFiles.metadata()
			.property("v31.b", "B")
			.property("v31.a", "A")
			.property("v31.a", "A")
			.writeTo(metadataFile());
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	private CheckSpringConfigurationMetadata task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckSpringConfigurationMetadata task = project.getTasks()
			.register(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME, CheckSpringConfigurationMetadata.class)
			.get();
		task.getMetadataLocation().set(metadataFile().toFile());
		task.getReportLocation().set(report().toFile());
		return task;
	}

	private Path metadataFile() {
		return this.directory.resolve("classes/META-INF/spring-configuration-metadata.json");
	}

	private Path report() {
		return this.directory.resolve("build/reports/spring-configuration-metadata/check.txt");
	}

}
