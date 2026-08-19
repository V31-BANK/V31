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

import java.util.stream.Collectors;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.util.MetadataConfiguration;
import org.v31bank.build.util.SourceSets;

/**
 * Configures a project that defines {@code @ConfigurationProperties}: its metadata is
 * generated completely, checked, and offered to whatever collects it.
 * <p>
 * Declared by the project itself, beside whichever java plugin it chose:
 *
 * <pre class="code">
 * plugins {
 *     `java-library`
 *     id("org.v31bank.configuration-properties")
 * }
 * </pre>
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ConfigurationPropertiesPlugin implements Plugin<Project> {

	/**
	 * Name of the configuration the generated metadata is offered through.
	 */
	public static final String METADATA_CONFIGURATION_NAME = "configurationPropertiesMetadata";

	/**
	 * Name of the task that checks the generated metadata.
	 */
	public static final String CHECK_METADATA_TASK_NAME = "checkSpringConfigurationMetadata";

	/**
	 * Name of the task that checks the hand-written metadata.
	 */
	public static final String CHECK_ADDITIONAL_METADATA_TASK_NAME = "checkAdditionalSpringConfigurationMetadata";

	static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";

	static final String ADDITIONAL_METADATA_FILE = "META-INF/additional-spring-configuration-metadata.json";

	static final String METADATA_USAGE = "configuration-properties-metadata";

	private static final String CONFIGURATION_PROCESSOR = "org.springframework.boot:spring-boot-configuration-processor";

	private static final String ADDITIONAL_LOCATIONS_ARGUMENT = "-Aorg.springframework.boot.configurationprocessor.additionalMetadataLocations=";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (_) -> configure(project));
	}

	private void configure(Project project) {
		SourceSet main = SourceSets.of(project).main().unwrap();
		addAnnotationProcessor(project);
		compileWholeSourceSet(project, main);
		readAdditionalMetadata(project, main);
		registerChecks(project, main);
		offerMetadata(project, main);
	}

	private void addAnnotationProcessor(Project project) {
		project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, CONFIGURATION_PROCESSOR);
	}

	/**
	 * An incremental run drops every description belonging to a class that did not
	 * change.
	 * @param project the project to configure
	 * @param main the main source set
	 */
	private void compileWholeSourceSet(Project project, SourceSet main) {
		project.getTasks()
			.named(main.getCompileJavaTaskName(), JavaCompile.class)
			.configure((compile) -> compile.getOptions().setIncremental(false));
	}

	/**
	 * compileJava runs before processResources, so the processor is pointed at the
	 * sources.
	 * @param project the project to configure
	 * @param main the main source set
	 */
	private void readAdditionalMetadata(Project project, SourceSet main) {
		String locations = main.getResources()
			.getSourceDirectories()
			.getFiles()
			.stream()
			.map(project.getRootProject()::relativePath)
			.collect(Collectors.joining(","));
		project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class).configure((compile) -> {
			compile.getInputs()
				.files(project.getTasks().named(main.getProcessResourcesTaskName()))
				.withPathSensitivity(PathSensitivity.RELATIVE)
				.withPropertyName("processed resources");
			compile.getOptions().getCompilerArgs().add(ADDITIONAL_LOCATIONS_ARGUMENT + locations);
		});
	}

	private void registerChecks(Project project, SourceSet main) {
		TaskProvider<CheckAdditionalSpringConfigurationMetadata> checkAdditional = project.getTasks()
			.register(CHECK_ADDITIONAL_METADATA_TASK_NAME, CheckAdditionalSpringConfigurationMetadata.class, (task) -> {
				task.setDescription("Checks the hand-written configuration metadata of the main source set.");
				task.setSource(main.getResources());
				task.include(ADDITIONAL_METADATA_FILE);
				task.getReportLocation().set(report(project, "additional-spring-configuration-metadata"));
			});
		TaskProvider<CheckSpringConfigurationMetadata> checkGenerated = project.getTasks()
			.register(CHECK_METADATA_TASK_NAME, CheckSpringConfigurationMetadata.class, (task) -> {
				task.setDescription("Checks the generated configuration metadata of the main source set.");
				task.getMetadataLocation().set(generatedMetadata(project, main));
				task.getReportLocation().set(report(project, "spring-configuration-metadata"));
			});
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((check) -> check.dependsOn(checkAdditional, checkGenerated));
	}

	private void offerMetadata(Project project, SourceSet main) {
		MetadataConfiguration.create(project, METADATA_CONFIGURATION_NAME, METADATA_USAGE);
		project.getArtifacts()
			.add(METADATA_CONFIGURATION_NAME, generatedMetadata(project, main),
					(artifact) -> artifact.builtBy(main.getClassesTaskName()));
	}

	/**
	 * Taken from the compile task, not the source set, so whatever reads it waits for it.
	 * @param project the project to configure
	 * @param main the main source set
	 * @return where the generated metadata ends up
	 */
	private Provider<RegularFile> generatedMetadata(Project project, SourceSet main) {
		return project.getTasks()
			.named(main.getCompileJavaTaskName(), JavaCompile.class)
			.flatMap((compile) -> compile.getDestinationDirectory().file(METADATA_FILE));
	}

	private Provider<RegularFile> report(Project project, String name) {
		return project.getLayout().getBuildDirectory().file("reports/%s/check.txt".formatted(name));
	}

}
