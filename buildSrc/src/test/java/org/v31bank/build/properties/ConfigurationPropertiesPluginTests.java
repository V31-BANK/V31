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

import java.io.File;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesPlugin}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ConfigurationPropertiesPluginTests {

	@TempDir
	private File directory;

	@Test
	void decidesNothingAboutHowTheProjectIsBuilt() {
		Project project = bareProject();
		project.getPlugins().apply(ConfigurationPropertiesPlugin.class);
		assertThat(project.getPlugins().hasPlugin(JavaPlugin.class)).isFalse();
		assertThat(project.getTasks().findByName(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME)).isNull();
	}

	@Test
	void compilesWithTheProcessorThatWritesTheMetadataAndNoOther() {
		Configuration annotationProcessor = project().getConfigurations()
			.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
		assertThat(annotationProcessor.getDependencies()).extracting(Dependency::getName)
			.contains("spring-boot-configuration-processor")
			.doesNotContain("spring-boot-autoconfigure-processor");
	}

	/**
	 * The reason the whole source set is compiled every time: an incremental run drops
	 * every description belonging to a class that did not change.
	 */
	@Test
	void compilesTheWholeSourceSetSoThatNoDescriptionIsLost() {
		JavaCompile compileJava = (JavaCompile) project().getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
		assertThat(compileJava.getOptions().isIncremental()).isFalse();
	}

	@Test
	void pointsTheProcessorAtTheHandWrittenMetadataAndWaitsForIt() {
		Project project = project();
		JavaCompile compileJava = (JavaCompile) project.getTasks().getByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
		assertThat(compileJava.getOptions().getCompilerArgs()).anyMatch((argument) -> argument
			.startsWith("-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations=")
				&& argument.contains("src/main/resources"));
		assertThat(TaskDependencies
			.namesOf(compileJava.getInputs().getFiles().getBuildDependencies().getDependencies(compileJava)))
			.contains(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
	}

	@Test
	void registersACheckOfTheGeneratedMetadataAndOneOfTheHandWrittenOne() {
		Project project = project();
		assertThat(project.getTasks().findByName(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME))
			.isInstanceOf(CheckSpringConfigurationMetadata.class);
		assertThat(project.getTasks().findByName(ConfigurationPropertiesPlugin.CHECK_ADDITIONAL_METADATA_TASK_NAME))
			.isInstanceOf(CheckAdditionalSpringConfigurationMetadata.class);
	}

	@Test
	void runsBothChecksAsPartOfCheck() {
		Task check = project().getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME);
		assertThat(TaskDependencies.namesOf(check.getDependsOn())).contains(
				ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME,
				ConfigurationPropertiesPlugin.CHECK_ADDITIONAL_METADATA_TASK_NAME);
	}

	@Test
	void presentsBothChecksAsVerificationTasks() {
		Project project = project();
		for (String name : List.of(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME,
				ConfigurationPropertiesPlugin.CHECK_ADDITIONAL_METADATA_TASK_NAME)) {
			assertThat(project.getTasks().getByName(name).getGroup()).as(name)
				.isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
		}
	}

	@Test
	void checksTheMetadataWhereTheCompilerWroteIt() {
		Project project = project();
		CheckSpringConfigurationMetadata check = (CheckSpringConfigurationMetadata) project.getTasks()
			.getByName(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME);
		assertThat(check.getMetadataLocation().get().getAsFile()).hasName("spring-configuration-metadata.json")
			.hasParent(
					new File(project.getLayout().getBuildDirectory().get().getAsFile(), "classes/java/main/META-INF"));
	}

	/**
	 * The file does not exist until the compiler has written it, so a check that does not
	 * wait for the compiler fails validation on a clean build rather than running.
	 */
	@Test
	void waitsForTheCompilerThatWritesTheMetadata() {
		Project project = project();
		CheckSpringConfigurationMetadata check = (CheckSpringConfigurationMetadata) project.getTasks()
			.getByName(ConfigurationPropertiesPlugin.CHECK_METADATA_TASK_NAME);
		assertThat(TaskDependencies.namesOf(check.getInputs().getFiles().getBuildDependencies().getDependencies(check)))
			.contains(JavaPlugin.COMPILE_JAVA_TASK_NAME);
	}

	@Test
	void offersTheMetadataThroughAConfigurationThatSaysWhatItIs() {
		Project project = project();
		Configuration metadata = project.getConfigurations()
			.getByName(ConfigurationPropertiesPlugin.METADATA_CONFIGURATION_NAME);
		assertThat(metadata.isCanBeConsumed()).isTrue();
		assertThat(metadata.isCanBeResolved()).isFalse();
		assertThat(metadata.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE)).extracting(Category::getName)
			.isEqualTo(Category.DOCUMENTATION);
		assertThat(metadata.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).extracting(Usage::getName)
			.isEqualTo("configuration-properties-metadata");
		assertThat(metadata.getArtifacts()).singleElement()
			.satisfies((artifact) -> assertThat(
					TaskDependencies.namesOf(artifact.getBuildDependencies().getDependencies(null)))
				.contains(JavaPlugin.CLASSES_TASK_NAME));
	}

	private Project project() {
		Project project = bareProject();
		project.getPlugins().apply("java-library");
		project.getPlugins().apply(ConfigurationPropertiesPlugin.class);
		return project;
	}

	private Project bareProject() {
		return ProjectBuilder.builder().withName("V31-example").withProjectDir(this.directory).build();
	}

}
