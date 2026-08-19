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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link CheckAutoConfigurationClasses}.
 * <p>
 * A dependency is a directory of class files here rather than a jar, which is one of the
 * two shapes a resolved classpath comes in and the one a test can write.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckAutoConfigurationClassesTests {

	private static final String EXAMPLE = "com.example.ExampleAutoConfiguration";

	private static final String ALWAYS_THERE = "com.example.AlwaysThereAutoConfiguration";

	private static final String MAY_BE_ABSENT = "com.example.MayBeAbsentAutoConfiguration";

	@TempDir
	private Path directory;

	@Test
	void passesWhenEveryAnnotatedClassIsRegistered() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports(EXAMPLE);
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenAnAnnotatedClassIsNotRegistered() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining(EXAMPLE)
			.withMessageContaining("is not registered in");
	}

	/**
	 * The mistake that made skipping on an absent file the wrong thing to do: a module's
	 * first auto-configuration, written before anyone thought about the file that has to
	 * name it.
	 */
	@Test
	void failsWhenThereIsNoImportsFileAtAllToRegisterIn() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining(EXAMPLE)
			.withMessageContaining("is not registered in");
	}

	@Test
	void passesWhenThereIsNeitherAnImportsFileNorAnythingToRegister() {
		ClassFiles.plainClass("com.example.Plain").writeTo(classes());
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenTheNameDoesNotSayWhatTheClassIs() {
		ClassFiles.autoConfiguration("com.example.ExampleConfig").writeTo(classes());
		writeImports("com.example.ExampleConfig");
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("name should end with AutoConfiguration");
	}

	@Test
	void acceptsAClassDeclaredAsOmittedFromTheImports() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports();
		CheckAutoConfigurationClasses task = task();
		task.getOmittedFromImports().add(EXAMPLE);
		assertThatCode(task::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenAClassDeclaredAsOmittedIsRegisteredAnyway() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		task.getOmittedFromImports().add(EXAMPLE);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining("declared as omitted from it");
	}

	@Test
	void failsWhenAClassIsNamedWhereOnlyItsNameWillDo() {
		ClassFiles.autoConfiguration(EXAMPLE).before(MAY_BE_ABSENT).writeTo(classes());
		ClassFiles.plainClass(MAY_BE_ABSENT).writeTo(optionalDependencies());
		writeImports(EXAMPLE);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining(
					"before '%s' is from an optional dependency and belongs in beforeName".formatted(MAY_BE_ABSENT));
	}

	@Test
	void failsWhenOnlyANameIsUsedForAClassThatIsAlwaysThere() {
		ClassFiles.autoConfiguration(EXAMPLE).afterName(ALWAYS_THERE).writeTo(classes());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(requiredDependencies());
		writeImports(EXAMPLE);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining(
					"afterName '%s' is from a required dependency and belongs in after".formatted(ALWAYS_THERE));
	}

	@Test
	void failsWhenANameMatchesNothingOnEitherClasspath() {
		ClassFiles.autoConfiguration(EXAMPLE).beforeName("com.example.GoneAutoConfiguration").writeTo(classes());
		writeImports(EXAMPLE);
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("beforeName 'com.example.GoneAutoConfiguration' was not found");
	}

	@Test
	void passesWhenBothFormsAreTheRightWayRound() {
		ClassFiles.autoConfiguration(EXAMPLE).before(ALWAYS_THERE).afterName(MAY_BE_ABSENT).writeTo(classes());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(requiredDependencies());
		ClassFiles.plainClass(MAY_BE_ABSENT).writeTo(optionalDependencies());
		writeImports(EXAMPLE);
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	/**
	 * A dependency that is optional here but required there is required: a consumer that
	 * resolves it at all gets the class.
	 */
	@Test
	void treatsAClassOnBothClasspathsAsAlwaysThere() {
		ClassFiles.autoConfiguration(EXAMPLE).before(ALWAYS_THERE).writeTo(classes());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(requiredDependencies());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(optionalDependencies());
		writeImports(EXAMPLE);
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void gathersEveryProblemOfOneClassTogether() {
		ClassFiles.autoConfiguration("com.example.ExampleConfig").beforeName("com.example.Gone").writeTo(classes());
		writeImports();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task()::check)
			.withMessageContaining("name should end with AutoConfiguration")
			.withMessageContaining("is not registered in")
			.withMessageContaining("beforeName 'com.example.Gone' was not found");
	}

	@Test
	void readsNoDependencyWhenNothingDeclaresAnOrder() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		task.getRequiredDependencies().from(unreadableJar().toFile());
		assertThatCode(task::check).doesNotThrowAnyException();
	}

	/**
	 * The other half of {@link #readsNoDependencyWhenNothingDeclaresAnOrder()}: the jar
	 * it is handed really is one nothing can read, so passing there was the order being
	 * absent and not the jar being fine.
	 */
	@Test
	void readsTheDependenciesWhenSomethingDeclaresAnOrder() {
		ClassFiles.autoConfiguration(EXAMPLE).before(ALWAYS_THERE).writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		task.getRequiredDependencies().from(unreadableJar().toFile());
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(task::check);
	}

	private CheckAutoConfigurationClasses task() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		CheckAutoConfigurationClasses task = project.getTasks()
			.register(AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME, CheckAutoConfigurationClasses.class)
			.get();
		task.getResources().from(resources().toFile());
		task.getClasspath().from(classes().toFile());
		task.getRequiredDependencies().from(requiredDependencies().toFile());
		task.getOptionalDependencies().from(optionalDependencies().toFile());
		return task;
	}

	private Path classes() {
		return this.directory.resolve("classes");
	}

	private Path resources() {
		return this.directory.resolve("resources");
	}

	private Path requiredDependencies() {
		return this.directory.resolve("required");
	}

	private Path optionalDependencies() {
		return this.directory.resolve("optional");
	}

	private Path unreadableJar() {
		return write(this.directory.resolve("unreadable.jar"), "not a jar");
	}

	private void writeImports(String... entries) {
		write(resources().resolve(AutoConfigurationImportsTask.IMPORTS_FILE),
				String.join(System.lineSeparator(), entries) + System.lineSeparator());
	}

	private Path write(Path file, String content) {
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, content);
			return file;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + file, ex);
		}
	}

}
