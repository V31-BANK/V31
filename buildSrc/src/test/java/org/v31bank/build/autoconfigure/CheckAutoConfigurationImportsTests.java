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

package org.v31bank.build.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
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
 * Tests for {@link CheckAutoConfigurationImports}.
 * <p>
 * The failure tells you where to look and the report tells you what is wrong, so both are
 * asserted: a check that threw without writing down why would pass half of these.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckAutoConfigurationImportsTests {

	private static final String A = "com.example.AaAutoConfiguration";

	private static final String B = "com.example.BbAutoConfiguration";

	@TempDir
	private Path directory;

	@Test
	void passesWhenEveryEntryNamesAnAnnotatedClass() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		ClassFiles.autoConfiguration(B).writeTo(classes());
		writeImports(A, B);
		CheckAutoConfigurationImports task = task();
		assertThatCode(task::check).doesNotThrowAnyException();
		assertThat(report(task)).isEmpty();
	}

	@Test
	void failsWhenAnEntryNamesAClassThatIsNotThere() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		writeImports(A, B);
		CheckAutoConfigurationImports task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining(AutoConfigurationImports.PATH)
			.withMessageContaining(AutoConfigurationImportsTask.FAILURE_REPORT);
		assertThat(report(task)).contains("'%s' was not found".formatted(B));
	}

	@Test
	void failsWhenAnEntryNamesAClassThatLostItsAnnotation() {
		ClassFiles.plainClass(A).writeTo(classes());
		writeImports(A);
		CheckAutoConfigurationImports task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("'%s' is not annotated with @AutoConfiguration".formatted(A));
	}

	/**
	 * The order to use is written out rather than described, so that fixing the file is a
	 * copy rather than a sort by hand.
	 */
	@Test
	void failsWhenTheEntriesAreOutOfOrderAndWritesOutTheOrderToUse() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		ClassFiles.autoConfiguration(B).writeTo(classes());
		writeImports(B, A);
		CheckAutoConfigurationImports task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("sorted alphabetically");
		assertThat(read(outputDirectory(task).resolve("sorted-" + AutoConfigurationImports.FILE_NAME)))
			.containsSubsequence(A, B);
	}

	@Test
	void readsTheFileTheWaySpringBootDoes() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		writeImports("# the one this module registers", "", A + "   ", "");
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	/**
	 * A module with no imports file registers nothing, and a check with nothing to read
	 * is skipped by Gradle rather than run: {@code getSource()} is what carries
	 * {@code @SkipWhenEmpty}.
	 */
	@Test
	void hasNothingToReadWhenTheModuleHasNoImportsFile() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		assertThat(task().getSource().isEmpty()).isTrue();
	}

	private CheckAutoConfigurationImports task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckAutoConfigurationImports task = project.getTasks()
			.register(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME, CheckAutoConfigurationImports.class)
			.get();
		task.getResources().from(resources().toFile());
		task.getClasspath().from(classes().toFile());
		return task;
	}

	private String report(AutoConfigurationImportsTask task) {
		return read(outputDirectory(task).resolve(AutoConfigurationImportsTask.FAILURE_REPORT));
	}

	private Path outputDirectory(AutoConfigurationImportsTask task) {
		return task.getOutputDirectory().get().getAsFile().toPath();
	}

	private Path classes() {
		return this.directory.resolve("classes");
	}

	private Path resources() {
		return this.directory.resolve("resources");
	}

	private String read(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + file, ex);
		}
	}

	private void writeImports(String... lines) {
		Path importsFile = resources().resolve(AutoConfigurationImports.PATH);
		try {
			Files.createDirectories(importsFile.getParent());
			Files.writeString(importsFile, String.join(System.lineSeparator(), lines) + System.lineSeparator());
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + importsFile, ex);
		}
	}

}
