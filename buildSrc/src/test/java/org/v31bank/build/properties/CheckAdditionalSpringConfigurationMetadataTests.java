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
 * Tests for {@link CheckAdditionalSpringConfigurationMetadata}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckAdditionalSpringConfigurationMetadataTests {

	@TempDir
	private Path directory;

	@Test
	void passesWhenTheHandWrittenFileIsTidy() {
		MetadataFiles.metadata().property("v31.a", "A").property("v31.b", "B").writeTo(metadataFile());
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenTheEntriesAreOutOfOrder() {
		MetadataFiles.metadata().property("v31.b", "B").property("v31.a", "A").writeTo(metadataFile());
		CheckAdditionalSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report()).content().contains("Wrong order at");
	}

	@Test
	void failsWhenANameIsWrittenTwice() {
		MetadataFiles.metadata().property("v31.a", "A").property("v31.a", "Again").writeTo(metadataFile());
		CheckAdditionalSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report()).content().contains("Duplicate name 'v31.a'");
	}

	@Test
	void failsWhenADeprecationDoesNotSayWhenItStarted() {
		MetadataFiles.metadata().deprecated("v31.gone", "v31.a", null).writeTo(metadataFile());
		CheckAdditionalSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report()).content().contains("deprecated without a 'since' version").contains("- v31.gone");
	}

	/**
	 * The hand-written file exists to add what the processor could not work out, so
	 * demanding a description of every line of it would make the escape hatch harder to
	 * use than the thing it escapes.
	 */
	@Test
	void letsAHandWrittenPropertySayNothing() {
		MetadataFiles.metadata().undescribed("v31.a").writeTo(metadataFile());
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	private CheckAdditionalSpringConfigurationMetadata task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckAdditionalSpringConfigurationMetadata task = project.getTasks()
			.register(ConfigurationPropertiesPlugin.CHECK_ADDITIONAL_METADATA_TASK_NAME,
					CheckAdditionalSpringConfigurationMetadata.class)
			.get();
		task.setSource(project.files(this.directory.resolve("resources").toFile()));
		task.include(ConfigurationPropertiesPlugin.ADDITIONAL_METADATA_FILE);
		task.getReportLocation().set(report().toFile());
		return task;
	}

	private Path metadataFile() {
		return this.directory.resolve("resources").resolve(ConfigurationPropertiesPlugin.ADDITIONAL_METADATA_FILE);
	}

	private Path report() {
		return this.directory.resolve("build/reports/additional-spring-configuration-metadata/check.txt");
	}

}
