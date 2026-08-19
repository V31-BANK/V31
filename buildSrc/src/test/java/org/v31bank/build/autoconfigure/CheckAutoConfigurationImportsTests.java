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
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenAnEntryNamesAClassThatIsNotThere() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		writeImports(A, B);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("'%s' was not found".formatted(B))
			.withMessageContaining(AutoConfigurationImportsTask.IMPORTS_FILE);
	}

	@Test
	void failsWhenAnEntryNamesAClassThatLostItsAnnotation() {
		ClassFiles.plainClass(A).writeTo(classes());
		writeImports(A);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("'%s' is not annotated with @AutoConfiguration".formatted(A));
	}

	@Test
	void failsWhenTheEntriesAreOutOfOrderAndSaysWhatOrderToUse() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		ClassFiles.autoConfiguration(B).writeTo(classes());
		writeImports(B, A);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("sorted alphabetically")
			.satisfies((failure) -> assertThat(failure.getMessage()).containsSubsequence(A, B));
	}

	@Test
	void readsTheFileTheWaySpringBootDoes() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		writeImports("# the one this module registers", "", A + "   ", "");
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	/**
	 * Whether the module needed a file is a question about its classes, which
	 * {@link CheckAutoConfigurationClasses} answers.
	 */
	@Test
	void saysNothingAboutAModuleWithNoImportsFile() {
		ClassFiles.autoConfiguration(A).writeTo(classes());
		assertThatCode(task()::check).doesNotThrowAnyException();
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

	private Path classes() {
		return this.directory.resolve("classes");
	}

	private Path resources() {
		return this.directory.resolve("resources");
	}

	private void writeImports(String... lines) {
		Path importsFile = resources().resolve(AutoConfigurationImportsTask.IMPORTS_FILE);
		try {
			Files.createDirectories(importsFile.getParent());
			Files.writeString(importsFile, String.join(System.lineSeparator(), lines) + System.lineSeparator());
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + importsFile, ex);
		}
	}

}
