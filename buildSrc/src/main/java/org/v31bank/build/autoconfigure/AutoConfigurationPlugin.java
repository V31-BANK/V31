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
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.ConventionsPlugin;
import org.v31bank.build.DeployedPlugin;
import org.v31bank.build.optional.OptionalDependenciesPlugin;
import org.v31bank.build.util.SourceSets;

/**
 * Configures a project as a V31 auto-configuration module: published, compiled with the
 * processors Spring Boot expects of one, and held to what it says it registers.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.auto-configuration")
 * }
 * </pre>
 *
 * A module's build file is then left saying only what it is called and what it depends
 * on. What the plugin adds beyond that is two checks, because an auto-configuration is
 * the one kind of code in this repository that fails by doing nothing: it is found
 * through a file of class names, applied only if its conditions hold, and a mistake in
 * either shows up as beans that are simply not there.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class AutoConfigurationPlugin implements Plugin<Project> {

	/**
	 * Name of the task that checks the imports file.
	 */
	public static final String CHECK_IMPORTS_TASK_NAME = "checkAutoConfigurationImports";

	/**
	 * Name of the task that checks the annotated classes.
	 */
	public static final String CHECK_CLASSES_TASK_NAME = "checkAutoConfigurationClasses";

	/**
	 * Name of both the metadata task and the configuration it is offered through.
	 */
	public static final String METADATA_NAME = "autoConfigurationMetadata";

	/**
	 * Name of the configuration holding what the module resolves whatever a consumer asks
	 * for.
	 */
	public static final String REQUIRED_CLASSPATH_CONFIGURATION_NAME = "autoConfigurationRequiredClasspath";

	/**
	 * Name of the configuration holding what only an optional dependency brings in.
	 */
	public static final String OPTIONAL_CLASSPATH_CONFIGURATION_NAME = "autoConfigurationOptionalClasspath";

	private static final String CONFIGURATION_PROCESSOR = "org.springframework.boot:spring-boot-configuration-processor";

	private static final String AUTO_CONFIGURATION_PROCESSOR = "org.springframework.boot:spring-boot-autoconfigure-processor";

	@Override
	public void apply(Project project) {
		PluginManager pluginManager = project.getPluginManager();
		pluginManager.apply(JavaLibraryPlugin.class);
		pluginManager.apply(DeployedPlugin.class);
		SourceSet main = SourceSets.of(project).main().unwrap();
		addAnnotationProcessors(project);
		registerChecks(project, main);
		registerMetadata(project, main);
	}

	/**
	 * One processor writes the metadata an IDE completes {@code application.yaml} from;
	 * the other writes the conditions Spring Boot filters on before loading a class. Both
	 * are what makes a module an auto-configuration module rather than a jar that happens
	 * to contain one, so neither is left to a build file to remember.
	 * @param project the project to configure
	 */
	private void addAnnotationProcessors(Project project) {
		DependencyHandler dependencies = project.getDependencies();
		dependencies.add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, CONFIGURATION_PROCESSOR);
		dependencies.add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, AUTO_CONFIGURATION_PROCESSOR);
	}

	private void registerChecks(Project project, SourceSet main) {
		TaskProvider<CheckAutoConfigurationImports> checkImports = project.getTasks()
			.register(CHECK_IMPORTS_TASK_NAME, CheckAutoConfigurationImports.class, (task) -> {
				task.setDescription("Checks the auto-configurations the main source set registers.");
				readMainSourceSet(task, main);
				configureAsCheck(task);
			});
		TaskProvider<CheckAutoConfigurationClasses> checkClasses = project.getTasks()
			.register(CHECK_CLASSES_TASK_NAME, CheckAutoConfigurationClasses.class, (task) -> {
				task.setDescription("Checks the @AutoConfiguration classes of the main source set.");
				readMainSourceSet(task, main);
				configureAsCheck(task);
				task.getRequiredDependencies().from(requiredClasspath(project, main));
			});
		project.getPlugins()
			.withType(OptionalDependenciesPlugin.class, (_) -> checkClasses
				.configure((task) -> task.getOptionalDependencies().from(optionalClasspath(project))));
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((check) -> check.dependsOn(checkImports, checkClasses));
	}

	private void configureAsCheck(AutoConfigurationImportsTask task) {
		task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		// A task that declares no output is never up to date, so its inputs never get to
		// decide.
		task.getOutputs().upToDateWhen((_) -> true);
	}

	/**
	 * The file is offered through a configuration of its own rather than the module's
	 * jar, so a consumer asks for {@code autoConfigurationMetadata} by name and Gradle
	 * builds it on demand.
	 * @param project the project to configure
	 * @param main the source set to describe
	 */
	private void registerMetadata(Project project, SourceSet main) {
		TaskProvider<AutoConfigurationMetadata> metadata = project.getTasks()
			.register(METADATA_NAME, AutoConfigurationMetadata.class, (task) -> {
				task.setDescription("Generates metadata describing the module's auto-configurations.");
				// The built resources rather than the source ones: what this describes is
				// the module that ships, not the module as it is written.
				task.getResources().from(main.getOutput());
				task.getClasspath().from(main.getOutput().getClassesDirs());
				task.getDestination()
					.set(project.getLayout().getBuildDirectory().file("auto-configuration-metadata.properties"));
			});
		project.getConfigurations().create(METADATA_NAME);
		project.getArtifacts()
			.add(METADATA_NAME, metadata.map(AutoConfigurationMetadata::getDestination),
					(artifact) -> artifact.builtBy(metadata));
	}

	/**
	 * The imports file is read where it is written rather than where it is copied to, so
	 * that a check does not wait on the resources being processed to say what is already
	 * plain.
	 * @param task the task to configure
	 * @param main the source set to read
	 */
	private void readMainSourceSet(AutoConfigurationImportsTask task, SourceSet main) {
		task.getResources().from(main.getResources());
		task.getClasspath().from(main.getOutput().getClassesDirs());
	}

	/**
	 * What the module compiles and runs against no matter what a consumer asks for.
	 * {@code implementation} is enough to reach {@code api} as well, which extends into
	 * it.
	 * @param project the project to configure
	 * @param main the source set whose dependencies these are
	 * @return the configuration to resolve
	 */
	private Configuration requiredClasspath(Project project, SourceSet main) {
		ConfigurationContainer configurations = project.getConfigurations();
		return resolvable(project, REQUIRED_CLASSPATH_CONFIGURATION_NAME,
				configurations.getByName(main.getImplementationConfigurationName()),
				configurations.getByName(main.getRuntimeOnlyConfigurationName()));
	}

	private Configuration optionalClasspath(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		return resolvable(project, OPTIONAL_CLASSPATH_CONFIGURATION_NAME,
				configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME));
	}

	/**
	 * Both names end in {@code Classpath} because that is what the conventions match to
	 * put the platform on a configuration. Named anything else, these resolve
	 * dependencies that were declared without a version and fail.
	 * @param project the project to configure
	 * @param name what to call the configuration
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
