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

package org.v31bank.build.test;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

/**
 * Adds an {@code intTest} source set and task for tests that need more than the project
 * they are in.
 * <p>
 * Kept apart from {@code test} because the two fail for different reasons and cost
 * different amounts: a unit test failing means the code is wrong, while one of these
 * failing usually means something about how the build assembles or publishes is wrong.
 * Mixing them makes a slow, environment-dependent test look like an ordinary one.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class IntegrationTestPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code intTest} task.
	 */
	public static String INT_TEST_TASK_NAME = "intTest";

	/**
	 * Name of the {@code intTest} source set.
	 */
	public static String INT_TEST_SOURCE_SET_NAME = "intTest";

	@Override
	public void apply(Project project) {
		project.getPlugins().withType(JavaPlugin.class, (_) -> configureIntegrationTesting(project));
	}

	private void configureIntegrationTesting(Project project) {
		SourceSet intTestSourceSet = createSourceSet(project);
		TaskProvider<Test> task = createTestTask(project, intTestSourceSet);
		project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure((check) -> check.dependsOn(task));
		project.getDependencies()
			.add(intTestSourceSet.getRuntimeOnlyConfigurationName(), "org.junit.platform:junit-platform-launcher");
		declareAsTestSources(project, intTestSourceSet);
	}

	/**
	 * Gradle marks only {@code test} as testing source, so everything an IDE imports from
	 * here arrives as production code and the analysers running over it read these tests
	 * as production code too — an assertion becomes something to remove. The source set
	 * has to say what it is, and it says it here rather than in each developer's project
	 * settings, where the next import undoes it.
	 * @param project the project
	 * @param intTestSourceSet the source set
	 */
	private void declareAsTestSources(Project project, SourceSet intTestSourceSet) {
		project.getPluginManager().apply(IdeaPlugin.class);
		IdeaModule module = project.getExtensions().getByType(IdeaModel.class).getModule();
		module.getTestSources().from(intTestSourceSet.getJava().getSourceDirectories());
		module.getTestResources().from(intTestSourceSet.getResources().getSourceDirectories());
	}

	private SourceSet createSourceSet(Project project) {
		SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
		SourceSet intTestSourceSet = sourceSets.create(INT_TEST_SOURCE_SET_NAME);
		SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		intTestSourceSet.setCompileClasspath(intTestSourceSet.getCompileClasspath().plus(main.getOutput()));
		intTestSourceSet.setRuntimeClasspath(intTestSourceSet.getRuntimeClasspath().plus(main.getOutput()));
		return intTestSourceSet;
	}

	private TaskProvider<Test> createTestTask(Project project, SourceSet intTestSourceSet) {
		return project.getTasks().register(INT_TEST_TASK_NAME, Test.class, (task) -> {
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Runs integration tests.");
			task.setTestClassesDirs(intTestSourceSet.getOutput().getClassesDirs());
			task.setClasspath(intTestSourceSet.getRuntimeClasspath());
			task.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
		});
	}

}
