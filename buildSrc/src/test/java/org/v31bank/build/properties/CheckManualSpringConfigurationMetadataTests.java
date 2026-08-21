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
 * Tests for {@link CheckManualSpringConfigurationMetadata}.
 * <p>
 * There is no processor behind this file, so unlike the hand-written supplement it is
 * held to all four rules — the descriptions included.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckManualSpringConfigurationMetadataTests {

	@TempDir
	private Path directory;

	@Test
	void passesWhenTheWholeFileIsInOrder() {
		MetadataFiles.metadata().property("v31.a", "A").property("v31.b", "B").writeTo(metadataFile());
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenAPropertySaysNothing() {
		MetadataFiles.metadata().undescribed("v31.a").writeTo(metadataFile());
		CheckManualSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining("manual Spring configuration metadata");
		assertThat(report()).content().contains("The following properties have no description:");
	}

	@Test
	void findsEveryKindOfProblemInOneRun() {
		MetadataFiles.metadata()
			.property("v31.b", "B")
			.property("v31.a", "A")
			.property("v31.a", "Again")
			.undescribed("v31.c")
			.deprecated("v31.gone", "v31.a", null)
			.writeTo(metadataFile());
		CheckManualSpringConfigurationMetadata task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report()).content()
			.contains("Wrong order at")
			.contains("Duplicate name 'v31.a'")
			.contains("The following properties have no description:")
			.contains("deprecated without a 'since' version");
	}

	private CheckManualSpringConfigurationMetadata task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckManualSpringConfigurationMetadata task = project.getTasks()
			.register(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME,
					CheckManualSpringConfigurationMetadata.class)
			.get();
		task.getMetadataLocation().set(metadataFile().toFile());
		task.getReportLocation().set(report().toFile());
		return task;
	}

	private Path metadataFile() {
		return this.directory.resolve("resources").resolve(ConfigurationPropertiesPlugin.METADATA_FILE);
	}

	private Path report() {
		return this.directory.resolve("build/reports/manual-spring-configuration-metadata/check.txt");
	}

}
