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
 * Tests for {@link CheckAutoConfigurationClasses}.
 * <p>
 * A dependency is a directory of class files here rather than a jar, which is one of the
 * two shapes a resolved classpath comes in and the one a test can write. What is wrong is
 * asserted on the report the check leaves behind, because that is where it says it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CheckAutoConfigurationClassesTests {

	private static final String EXAMPLE = "com.example.ExampleAutoConfiguration";

	private static final String TEST_SLICE = "com.example.ExampleTestAutoConfiguration";

	private static final String ALWAYS_THERE = "com.example.AlwaysThereAutoConfiguration";

	private static final String MAY_BE_ABSENT = "com.example.MayBeAbsentAutoConfiguration";

	@TempDir
	private Path directory;

	@Test
	void passesWhenEveryAnnotatedClassIsRegistered() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatCode(task::check).doesNotThrowAnyException();
		assertThat(report(task)).isEmpty();
	}

	@Test
	void failsWhenAnAnnotatedClassIsNotRegistered() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		writeImports();
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check)
			.withMessageContaining(AutoConfigurationImportsTask.FAILURE_REPORT);
		assertThat(report(task)).contains(EXAMPLE).contains("is not registered in");
	}

	@Test
	void failsWhenTheNameDoesNotSayWhatTheClassIs() {
		ClassFiles.autoConfiguration("com.example.ExampleConfig").writeTo(classes());
		writeImports("com.example.ExampleConfig");
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("name should end with AutoConfiguration");
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
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("should not be registered in");
	}

	/**
	 * A test slice is asked for by name rather than registered, so leaving it out of the
	 * file is right and no module has to say so.
	 */
	@Test
	void expectsATestSliceToBeLeftUnregistered() {
		ClassFiles.autoConfiguration(TEST_SLICE).writeTo(classes());
		writeImports();
		assertThatCode(task()::check).doesNotThrowAnyException();
	}

	@Test
	void failsWhenATestSliceIsRegisteredAnyway() {
		ClassFiles.autoConfiguration(TEST_SLICE).writeTo(classes());
		writeImports(TEST_SLICE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("should not be registered in");
	}

	@Test
	void failsWhenAClassIsNamedWhereOnlyItsNameWillDo() {
		ClassFiles.autoConfiguration(EXAMPLE).before(MAY_BE_ABSENT).writeTo(classes());
		ClassFiles.plainClass(MAY_BE_ABSENT).writeTo(optionalDependencies());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task))
			.contains("before '%s' is from an optional dependency and should be declared in beforeName"
				.formatted(MAY_BE_ABSENT));
	}

	@Test
	void failsWhenOnlyANameIsUsedForAClassThatIsAlwaysThere() {
		ClassFiles.autoConfiguration(EXAMPLE).afterName(ALWAYS_THERE).writeTo(classes());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(requiredDependencies());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains(
				"afterName '%s' is from a required dependency and should be declared in after".formatted(ALWAYS_THERE));
	}

	@Test
	void failsWhenANameMatchesNothingOnEitherClasspath() {
		ClassFiles.autoConfiguration(EXAMPLE).beforeName("com.example.GoneAutoConfiguration").writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("beforeName 'com.example.GoneAutoConfiguration' not found");
	}

	/**
	 * All four attributes, each used the wrong way round, so that the message for every
	 * one of them is produced rather than the two that happen to be tested elsewhere.
	 */
	@Test
	void namesEveryAttributeItRejects() {
		ClassFiles.autoConfiguration(EXAMPLE)
			.before(MAY_BE_ABSENT)
			.after(MAY_BE_ABSENT)
			.beforeName(ALWAYS_THERE)
			.afterName(ALWAYS_THERE)
			.writeTo(classes());
		ClassFiles.plainClass(MAY_BE_ABSENT).writeTo(optionalDependencies());
		ClassFiles.plainClass(ALWAYS_THERE).writeTo(requiredDependencies());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task))
			.contains("before '%s' is from an optional dependency and should be declared in beforeName"
				.formatted(MAY_BE_ABSENT))
			.contains("after '%s' is from an optional dependency and should be declared in afterName"
				.formatted(MAY_BE_ABSENT))
			.contains("beforeName '%s' is from a required dependency and should be declared in before"
				.formatted(ALWAYS_THERE))
			.contains("afterName '%s' is from a required dependency and should be declared in after"
				.formatted(ALWAYS_THERE));
	}

	@Test
	void namesEveryAttributeWhoseTargetIsNowhere() {
		ClassFiles.autoConfiguration(EXAMPLE)
			.beforeName("com.example.GoneA")
			.afterName("com.example.GoneB")
			.writeTo(classes());
		writeImports(EXAMPLE);
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("beforeName 'com.example.GoneA' not found")
			.contains("afterName 'com.example.GoneB' not found");
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
		CheckAutoConfigurationClasses task = task();
		assertThatExceptionOfType(VerificationException.class).isThrownBy(task::check);
		assertThat(report(task)).contains("name should end with AutoConfiguration")
			.contains("is not registered in")
			.contains("beforeName 'com.example.Gone' not found");
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

	/**
	 * A module with no imports file has nothing for this to read, so Gradle skips it:
	 * {@code getSource()} is what carries {@code @SkipWhenEmpty}.
	 */
	@Test
	void hasNothingToReadWhenTheModuleHasNoImportsFile() {
		ClassFiles.autoConfiguration(EXAMPLE).writeTo(classes());
		assertThat(task().getSource().isEmpty()).isTrue();
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

	private String report(AutoConfigurationImportsTask task) {
		return read(
				task.getOutputDirectory().file(AutoConfigurationImportsTask.FAILURE_REPORT).get().getAsFile().toPath());
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
		write(resources().resolve(AutoConfigurationImports.PATH),
				String.join(System.lineSeparator(), entries) + System.lineSeparator());
	}

	private String read(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + file, ex);
		}
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
