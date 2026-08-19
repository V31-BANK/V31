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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.DeployedPlugin;
import org.v31bank.build.optional.OptionalDependenciesPlugin;
import org.v31bank.build.util.MetadataConfiguration;
import org.v31bank.build.util.SourceSets;

/**
 * Configures a project as a V31 auto-configuration module: published, compiled with the
 * auto-configuration processor, describing what it offers, and checked against what it
 * says it registers.
 * <p>
 * Declared by the project itself, beside whichever java plugin it chose:
 *
 * <pre class="code">
 * plugins {
 *     `java-library`
 *     id("org.v31bank.auto-configuration")
 * }
 * </pre>
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class AutoConfigurationPlugin implements Plugin<Project> {

	/**
	 * Name of both the metadata task and the configuration it is offered through.
	 */
	public static final String METADATA_NAME = "autoConfigurationMetadata";

	/**
	 * Name of the task that checks the imports file.
	 */
	public static final String CHECK_IMPORTS_TASK_NAME = "checkAutoConfigurationImports";

	/**
	 * Name of the task that checks the annotated classes.
	 */
	public static final String CHECK_CLASSES_TASK_NAME = "checkAutoConfigurationClasses";

	/**
	 * Name of the configuration holding what the module resolves whatever a consumer asks
	 * for.
	 */
	public static final String REQUIRED_CLASSPATH_CONFIGURATION_NAME = "autoConfigurationRequiredClasspath";

	/**
	 * Name of the configuration holding what only an optional dependency brings in.
	 */
	public static final String OPTIONAL_CLASSPATH_CONFIGURATION_NAME = "autoConfigurationOptionalClasspath";

	private static final String METADATA_USAGE = "auto-configuration-metadata";

	private static final String METADATA_FILE = "auto-configuration-metadata.properties";

	private static final String AUTOCONFIGURE = "org.springframework.boot:spring-boot-autoconfigure";

	private static final String AUTO_CONFIGURATION_PROCESSOR = "org.springframework.boot:spring-boot-autoconfigure-processor";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(DeployedPlugin.class);
		project.getPlugins().withType(JavaPlugin.class, (_) -> configure(project));
	}

	private void configure(Project project) {
		SourceSet main = SourceSets.of(project).main().unwrap();
		addAnnotationProcessors(project);
		registerMetadata(project, main);
		registerChecks(project, main);
	}

	private void addAnnotationProcessors(Project project) {
		project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, AUTO_CONFIGURATION_PROCESSOR);
	}

	private void registerMetadata(Project project, SourceSet main) {
		TaskProvider<AutoConfigurationMetadata> metadata = project.getTasks()
			.register(METADATA_NAME, AutoConfigurationMetadata.class, (task) -> {
				task.setDescription("Generates metadata describing the module's auto-configurations.");
				task.setSourceSet(main);
				task.dependsOn(main.getClassesTaskName());
				task.getOutputFile().set(project.getLayout().getBuildDirectory().file(METADATA_FILE));
			});
		MetadataConfiguration.create(project, METADATA_NAME, METADATA_USAGE);
		project.getArtifacts()
			.add(METADATA_NAME, metadata.map(AutoConfigurationMetadata::getOutputFile),
					(artifact) -> artifact.builtBy(metadata));
	}

	private void registerChecks(Project project, SourceSet main) {
		TaskProvider<CheckAutoConfigurationImports> checkImports = project.getTasks()
			.register(CHECK_IMPORTS_TASK_NAME, CheckAutoConfigurationImports.class, (task) -> {
				task.setDescription(
						"Checks the %s file of the main source set.".formatted(AutoConfigurationImports.PATH));
				readMainSourceSet(task, main);
			});
		TaskProvider<CheckAutoConfigurationClasses> checkClasses = project.getTasks()
			.register(CHECK_CLASSES_TASK_NAME, CheckAutoConfigurationClasses.class, (task) -> {
				task.setDescription("Checks the auto-configuration classes of the main source set.");
				readMainSourceSet(task, main);
				task.getRequiredDependencies().from(requiredClasspath(project, main));
			});
		project.getPlugins()
			.withType(OptionalDependenciesPlugin.class, (_) -> checkClasses
				.configure((task) -> task.getOptionalDependencies().from(optionalClasspath(project))));
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((check) -> check.dependsOn(checkImports, checkClasses));
	}

	/**
	 * The imports file is read where it is written, so a check need not wait on
	 * processResources to say what is already plain.
	 * @param task the task to configure
	 * @param main the main source set
	 */
	private void readMainSourceSet(AutoConfigurationImportsTask task, SourceSet main) {
		task.getResources().from(main.getResources());
		task.getClasspath().from(main.getOutput().getClassesDirs());
	}

	/**
	 * implementation reaches api as well, which extends into it.
	 * @param project the project to configure
	 * @param main the main source set
	 * @return the configuration to resolve
	 */
	private Configuration requiredClasspath(Project project, SourceSet main) {
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration required = resolvable(project, REQUIRED_CLASSPATH_CONFIGURATION_NAME,
				configurations.getByName(main.getImplementationConfigurationName()),
				configurations.getByName(main.getRuntimeOnlyConfigurationName()));
		required.getDependencies().add(project.getDependencies().create(AUTOCONFIGURE));
		return required;
	}

	private Configuration optionalClasspath(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		return resolvable(project, OPTIONAL_CLASSPATH_CONFIGURATION_NAME,
				configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME));
	}

	/**
	 * Names must end in Classpath: that is what the conventions match to attach the
	 * platform.
	 * @param project the project to configure
	 * @param name what to call it
	 * @param parents what it takes its dependencies from
	 * @return the configuration to resolve
	 */
	private Configuration resolvable(Project project, String name, Configuration... parents) {
		return project.getConfigurations().create(name, (configuration) -> {
			configuration.setCanBeConsumed(false);
			configuration.setCanBeResolved(true);
			configuration.extendsFrom(parents);
		});
	}

}
