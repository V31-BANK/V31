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

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.DeployedPlugin;
import org.v31bank.build.optional.OptionalDependenciesPlugin;
import org.v31bank.build.task.TaskDependencies;
import org.v31bank.build.util.SourceSets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigurationPlugin}.
 * <p>
 * The conventions reach for the platform project by path, so a project under test is
 * built inside a root that has one, the way the real build does.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class AutoConfigurationPluginTests {

	private static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";

	@TempDir
	private File directory;

	@Test
	void buildsALibraryThatIsPublished() {
		Project project = project();
		assertThat(project.getPlugins().hasPlugin(JavaLibraryPlugin.class)).isTrue();
		assertThat(project.getPlugins().hasPlugin(DeployedPlugin.class)).isTrue();
	}

	@Test
	void compilesWithTheProcessorsAnAutoConfigurationModuleNeeds() {
		Configuration annotationProcessor = project().getConfigurations()
			.getByName(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME);
		assertThat(annotationProcessor.getDependencies()).extracting(Dependency::getName)
			.contains("spring-boot-configuration-processor", "spring-boot-autoconfigure-processor");
	}

	@Test
	void registersACheckOfTheFileAndACheckOfTheClasses() {
		Project project = project();
		assertThat(project.getTasks().findByName(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME))
			.isInstanceOf(CheckAutoConfigurationImports.class);
		assertThat(project.getTasks().findByName(AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME))
			.isInstanceOf(CheckAutoConfigurationClasses.class);
	}

	@Test
	void runsBothChecksAsPartOfCheck() {
		Project project = project();
		Task check = project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME);
		assertThat(TaskDependencies.namesOf(check.getDependsOn()))
			.contains(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME, AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME);
	}

	@Test
	void presentsBothChecksAsVerificationTasks() {
		Project project = project();
		for (String name : List.of(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME,
				AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME)) {
			assertThat(project.getTasks().getByName(name).getGroup()).as(name)
				.isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
		}
	}

	/**
	 * Waiting on the compiler and not on the whole {@code classes} task is the point of
	 * reading the imports file where it is written: a check of what the module registers
	 * does not need its resources copied first.
	 */
	@Test
	void readsTheMainSourceSetOfTheProject() {
		Project project = project();
		SourceSet main = SourceSets.of(project).main().unwrap();
		for (String name : List.of(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME,
				AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME)) {
			AutoConfigurationImportsTask task = (AutoConfigurationImportsTask) project.getTasks().getByName(name);
			assertThat(task.getClasspath().getFiles()).as(name)
				.containsExactlyInAnyOrderElementsOf(main.getOutput().getClassesDirs().getFiles());
			assertThat(TaskDependencies.namesOf(task.getClasspath().getBuildDependencies().getDependencies(task)))
				.as(name)
				.containsExactly(main.getCompileJavaTaskName());
		}
	}

	@Test
	void takesTheRequiredClasspathFromEverythingTheModuleAlwaysResolves() {
		Project project = project();
		SourceSet main = SourceSets.of(project).main().unwrap();
		Configuration required = project.getConfigurations()
			.getByName(AutoConfigurationPlugin.REQUIRED_CLASSPATH_CONFIGURATION_NAME);
		assertThat(required.isCanBeResolved()).isTrue();
		assertThat(required.isCanBeConsumed()).isFalse();
		assertThat(parentNames(required)).contains(main.getImplementationConfigurationName(),
				main.getRuntimeOnlyConfigurationName());
	}

	/**
	 * {@code api} is not named directly because {@code implementation} extends it, which
	 * is the one thing that would quietly leave a module's exported dependencies out of
	 * the check.
	 */
	@Test
	void reachesTheExportedDependenciesThroughImplementation() {
		Project project = project();
		Configuration implementation = project.getConfigurations()
			.getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME);
		assertThat(parentNames(implementation)).contains(JavaPlugin.API_CONFIGURATION_NAME);
	}

	/**
	 * Both derived classpaths are resolved, and everything they hold was declared without
	 * a version. The conventions put the platform on a configuration whose name ends in
	 * {@code Classpath} and on no other, so the names are load-bearing.
	 */
	@Test
	void namesTheDerivedClasspathsSoThatTheyCarryThePlatform() {
		Project project = project();
		project.getPlugins().apply(OptionalDependenciesPlugin.class);
		for (String name : List.of(AutoConfigurationPlugin.REQUIRED_CLASSPATH_CONFIGURATION_NAME,
				AutoConfigurationPlugin.OPTIONAL_CLASSPATH_CONFIGURATION_NAME)) {
			assertThat(name).endsWith("Classpath");
			assertThat(parentNames(project.getConfigurations().getByName(name))).as(name)
				.contains(DEPENDENCY_MANAGEMENT);
		}
	}

	@Test
	void asksAboutOptionalDependenciesOnlyWhenTheModuleHasSome() {
		Project project = project();
		assertThat(
				project.getConfigurations().findByName(AutoConfigurationPlugin.OPTIONAL_CLASSPATH_CONFIGURATION_NAME))
			.isNull();
		project.getPlugins().apply(OptionalDependenciesPlugin.class);
		Configuration optional = project.getConfigurations()
			.getByName(AutoConfigurationPlugin.OPTIONAL_CLASSPATH_CONFIGURATION_NAME);
		assertThat(optional.isCanBeResolved()).isTrue();
		assertThat(parentNames(optional)).contains(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME);
	}

	@Test
	void feedsTheOptionalDependenciesToTheCheckThatJudgesThem() {
		Project project = project();
		project.getPlugins().apply(OptionalDependenciesPlugin.class);
		CheckAutoConfigurationClasses check = (CheckAutoConfigurationClasses) project.getTasks()
			.getByName(AutoConfigurationPlugin.CHECK_CLASSES_TASK_NAME);
		assertThat(check.getOptionalDependencies().getFrom()).isNotEmpty();
	}

	@Test
	void registersTheMetadataTask() {
		assertThat(project().getTasks().findByName(AutoConfigurationPlugin.METADATA_NAME))
			.isInstanceOf(AutoConfigurationMetadata.class);
	}

	@Test
	void offersTheMetadataThroughAConfigurationOfItsOwn() {
		Project project = project();
		assertThat(project.getConfigurations().findByName(AutoConfigurationPlugin.METADATA_NAME)).isNotNull();
		assertThat(project.getConfigurations().getByName(AutoConfigurationPlugin.METADATA_NAME).getArtifacts())
			.singleElement()
			.satisfies((artifact) -> assertThat(
					TaskDependencies.namesOf(artifact.getBuildDependencies().getDependencies(null)))
				.contains(AutoConfigurationPlugin.METADATA_NAME));
	}

	@Test
	void keepsTheMetadataOutOfTheModuleItDescribes() {
		Project project = project();
		AutoConfigurationMetadata metadata = (AutoConfigurationMetadata) project.getTasks()
			.getByName(AutoConfigurationPlugin.METADATA_NAME);
		assertThat(metadata.getDestination().get().getAsFile()).hasName("auto-configuration-metadata.properties")
			.hasParent(project.getLayout().getBuildDirectory().get().getAsFile());
	}

	/**
	 * The checks read the file where it is written so they need not wait on the resources
	 * being processed; the metadata describes what ships, so it reads the built ones.
	 */
	@Test
	void describesTheModuleThatShipsRatherThanTheOneThatIsWritten() {
		Project project = project();
		SourceSet main = SourceSets.of(project).main().unwrap();
		AutoConfigurationMetadata metadata = (AutoConfigurationMetadata) project.getTasks()
			.getByName(AutoConfigurationPlugin.METADATA_NAME);
		AutoConfigurationImportsTask check = (AutoConfigurationImportsTask) project.getTasks()
			.getByName(AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME);
		assertThat(metadata.getResources().getFrom()).containsExactly(main.getOutput());
		assertThat(check.getResources().getFrom()).containsExactly(main.getResources());
	}

	private static Set<String> parentNames(Configuration configuration) {
		return configuration.getExtendsFrom().stream().map(Configuration::getName).collect(Collectors.toSet());
	}

	private Project project() {
		// The conventions depend on the platform project by path, so it has to exist
		// first.
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		Project project = ProjectBuilder.builder().withName("V31-example-spring-boot").withParent(root).build();
		// Supplied by gradle.properties in the real build; the conventions read them,
		// never default.
		project.getExtensions().getExtraProperties().set("buildJavaVersion", "25");
		project.getExtensions().getExtraProperties().set("runtimeJavaVersion", "25");
		project.getExtensions().getExtraProperties().set("checkstyleToolVersion", "12.3.1");
		project.getPlugins().apply(AutoConfigurationPlugin.class);
		return project;
	}

}
