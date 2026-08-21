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

package org.v31bank.build.starters;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.ConventionsPlugin;
import org.v31bank.build.DeployedPlugin;
import org.v31bank.build.classpath.CheckClasspathForConflicts;
import org.v31bank.build.classpath.CheckClasspathForUnconstrainedDirectDependencies;
import org.v31bank.build.classpath.CheckClasspathForUnnecessaryExclusions;
import org.v31bank.build.classpath.ClasspathCheck;

/**
 * Configures a project as a V31 starter: published, exporting its dependencies rather
 * than hiding them, with its versions settled.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.starter")
 * }
 * </pre>
 *
 * A starter's build file is then left saying only what it is called and what it pulls in.
 * A starter has nothing to unit test, so the classpath checks registered here take the
 * place of the review its dependency graph would otherwise need by eye.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class StarterPlugin implements Plugin<Project> {

	private static final String STARTER_METADATA = "starterMetadata";

	@Override
	public void apply(Project project) {
		PluginManager pluginManager = project.getPluginManager();
		pluginManager.apply(JavaLibraryPlugin.class);
		pluginManager.apply(ConventionsPlugin.class);
		pluginManager.apply(DeployedPlugin.class);

		Configuration runtimeClasspath = project.getConfigurations()
			.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		registerMetadata(project, runtimeClasspath);

		registerClasspathCheck(project, runtimeClasspath, "ForConflicts", CheckClasspathForConflicts.class);
		registerClasspathCheck(project, runtimeClasspath, "ForUnconstrainedDirectDependencies",
				CheckClasspathForUnconstrainedDirectDependencies.class);
		registerClasspathCheck(project, runtimeClasspath, "ForUnnecessaryExclusions",
				CheckClasspathForUnnecessaryExclusions.class);
	}

	/**
	 * The file is published through a configuration of its own rather than the starter's
	 * jar, so a consumer asks for {@code starterMetadata} by name and Gradle builds it on
	 * demand.
	 * @param project the project
	 * @param runtimeClasspath what the starter resolves to
	 */
	private void registerMetadata(Project project, Configuration runtimeClasspath) {
		TaskProvider<StarterMetadata> metadata = project.getTasks()
			.register(STARTER_METADATA, StarterMetadata.class, (task) -> {
				task.setDescription("Generates metadata describing the starter.");
				task.setDependencies(runtimeClasspath);
				task.getDestination().set(project.getLayout().getBuildDirectory().file("starter-metadata.properties"));
			});
		project.getConfigurations().consumable(STARTER_METADATA).get();
		project.getArtifacts()
			.add(STARTER_METADATA, metadata.map(StarterMetadata::getDestination),
					(artifact) -> artifact.builtBy(metadata));
	}

	/**
	 * The task is named after the configuration it looks at, so a failure says which
	 * classpath it was talking about.
	 * @param <T> the type of check
	 * @param project the project
	 * @param classpath the configuration to check
	 * @param suffix what the check is looking for
	 * @param type the type of check
	 */
	private <T extends ClasspathCheck> void registerClasspathCheck(Project project, Configuration classpath,
			String suffix, Class<T> type) {
		TaskProvider<T> check = project.getTasks()
			.register(checkTaskName(classpath.getName() + suffix), type, (task) -> {
				task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
				task.setClasspath(classpath);
			});
		project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure((task) -> task.dependsOn(check));
	}

	private static String checkTaskName(String subject) {
		return "check" + Character.toUpperCase(subject.charAt(0)) + subject.substring(1);
	}

}
