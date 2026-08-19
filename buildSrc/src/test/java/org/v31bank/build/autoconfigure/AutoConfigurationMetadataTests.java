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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AutoConfigurationMetadata}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AutoConfigurationMetadataTests {

	private static final String FIRST = "com.example.AaAutoConfiguration";

	private static final String SECOND = "com.example.BbAutoConfiguration";

	@TempDir
	private Path directory;

	@Test
	void namesTheModuleAndWhatItRegisters() throws IOException {
		ClassFiles.autoConfiguration(FIRST).writeTo(classes());
		writeImports(FIRST);
		AutoConfigurationMetadata task = task();
		task.getModuleName().set("V31-example-spring-boot");
		task.generateMetadata();
		assertThat(metadata()).containsEntry("module", "V31-example-spring-boot")
			.containsEntry("autoConfigurationClassNames", FIRST);
	}

	@Test
	void takesItsModuleNameFromTheProject() {
		writeImports();
		assertThat(task().getModuleName().get()).isEqualTo("V31-example-spring-boot");
	}

	/**
	 * A collected list is read by something outside the module, and a package-private
	 * class is not a thing anything out there can name.
	 */
	@Test
	void listsOnlyTheClassesSomethingElseCouldName() throws IOException {
		ClassFiles.autoConfiguration(FIRST).writeTo(classes());
		ClassFiles.autoConfiguration(SECOND).notPublic().writeTo(classes());
		writeImports(FIRST, SECOND);
		task().generateMetadata();
		assertThat(metadata().getProperty("autoConfigurationClassNames")).isEqualTo(FIRST);
	}

	@Test
	void keepsTheOrderTheModuleRegisteredThemIn() throws IOException {
		ClassFiles.autoConfiguration(SECOND).writeTo(classes());
		ClassFiles.autoConfiguration(FIRST).writeTo(classes());
		writeImports(SECOND, FIRST);
		task().generateMetadata();
		assertThat(metadata().getProperty("autoConfigurationClassNames")).isEqualTo(SECOND + "," + FIRST);
	}

	@Test
	void saysSoWhenTheModuleRegistersNothing() throws IOException {
		task().generateMetadata();
		assertThat(metadata()).containsEntry("autoConfigurationClassNames", "");
	}

	/**
	 * The file is compared build to build, so anything in it that changes on its own — a
	 * timestamp, a platform line separator, an unordered map — would make an unchanged
	 * module look changed.
	 */
	@Test
	void writesTheSameBytesForTheSameModule() throws IOException {
		ClassFiles.autoConfiguration(FIRST).writeTo(classes());
		writeImports(FIRST);
		task().generateMetadata();
		byte[] first = read(destination());
		task().generateMetadata();
		assertThat(read(destination())).isEqualTo(first);
		assertThat(new String(first, StandardCharsets.ISO_8859_1).lines()).noneMatch((line) -> line.startsWith("#"));
	}

	@Test
	void failsWhenARegisteredClassWasNeverCompiled() {
		writeImports(FIRST);
		assertThatExceptionOfType(GradleException.class).isThrownBy(task()::generateMetadata)
			.withMessageContaining(FIRST)
			.withMessageContaining(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME);
	}

	private AutoConfigurationMetadata task() {
		Project project = ProjectBuilder.builder()
			.withName("V31-example-spring-boot")
			.withProjectDir(this.directory.toFile())
			.build();
		AutoConfigurationMetadata task = project.getTasks()
			.register(AutoConfigurationPlugin.METADATA_NAME, AutoConfigurationMetadata.class)
			.get();
		task.getResources().from(resources().toFile());
		task.getClasspath().from(classes().toFile());
		task.getDestination().set(destination().toFile());
		return task;
	}

	private Properties metadata() {
		Properties properties = new Properties();
		try {
			properties.load(Files.newBufferedReader(destination(), StandardCharsets.ISO_8859_1));
			return properties;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + destination(), ex);
		}
	}

	private Path classes() {
		return this.directory.resolve("classes");
	}

	private Path resources() {
		return this.directory.resolve("resources");
	}

	private Path destination() {
		return this.directory.resolve("build/auto-configuration-metadata.properties");
	}

	private byte[] read(Path file) {
		try {
			return Files.readAllBytes(file);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + file, ex);
		}
	}

	private void writeImports(String... entries) {
		Path importsFile = resources().resolve(AutoConfigurationImportsTask.IMPORTS_FILE);
		try {
			Files.createDirectories(importsFile.getParent());
			Files.writeString(importsFile, String.join(System.lineSeparator(), entries) + System.lineSeparator());
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + importsFile, ex);
		}
	}

}
