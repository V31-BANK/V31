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

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadataPlugin}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ConfigurationMetadataPluginTests {

	@TempDir
	private File directory;

	@Test
	void decidesNothingAboutHowTheProjectIsBuilt() {
		Project project = bareProject();
		project.getPlugins().apply(ConfigurationMetadataPlugin.class);
		assertThat(project.getTasks().findByName(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME)).isNull();
	}

	/**
	 * Nothing is generated here, so no processor is added. That is the whole difference
	 * from {@link ConfigurationPropertiesPlugin}.
	 */
	@Test
	void addsNoAnnotationProcessor() {
		Configuration annotationProcessor = project().getConfigurations()
			.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
		assertThat(annotationProcessor.getDependencies()).isEmpty();
	}

	@Test
	void registersACheckOfTheHandWrittenMetadata() {
		Project project = project();
		assertThat(project.getTasks().findByName(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME))
			.isInstanceOf(CheckManualSpringConfigurationMetadata.class);
		assertThat(project.getTasks().getByName(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME).getGroup())
			.isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
	}

	@Test
	void runsTheCheckAsPartOfCheck() {
		Task check = project().getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME);
		assertThat(TaskDependencies.namesOf(check.getDependsOn()))
			.contains(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME);
	}

	/**
	 * The file ships as it was written, so what is checked is the copy a consumer gets
	 * rather than the source it was copied from.
	 */
	@Test
	void checksTheMetadataWhereItShips() {
		Project project = project();
		CheckManualSpringConfigurationMetadata check = (CheckManualSpringConfigurationMetadata) project.getTasks()
			.getByName(ConfigurationMetadataPlugin.CHECK_METADATA_TASK_NAME);
		assertThat(check.getMetadataLocation().get()).hasName("spring-configuration-metadata.json")
			.hasParent(new File(project.getLayout().getBuildDirectory().get().getAsFile(), "resources/main/META-INF"));
	}

	@Test
	void offersTheMetadataThroughAConfigurationThatSaysWhatItIs() {
		Configuration metadata = project().getConfigurations()
			.getByName(ConfigurationPropertiesPlugin.METADATA_CONFIGURATION_NAME);
		assertThat(metadata.isCanBeConsumed()).isTrue();
		assertThat(metadata.getAttributes().getAttribute(Category.CATEGORY_ATTRIBUTE)).extracting(Category::getName)
			.isEqualTo(Category.DOCUMENTATION);
		assertThat(metadata.getAttributes().getAttribute(Usage.USAGE_ATTRIBUTE)).extracting(Usage::getName)
			.isEqualTo("configuration-properties-metadata");
		assertThat(metadata.getArtifacts()).hasSize(1);
	}

	private Project project() {
		Project project = bareProject();
		project.getPlugins().apply("java-library");
		project.getPlugins().apply(ConfigurationMetadataPlugin.class);
		return project;
	}

	private Project bareProject() {
		return ProjectBuilder.builder().withName("V31-example").withProjectDir(this.directory).build();
	}

}
