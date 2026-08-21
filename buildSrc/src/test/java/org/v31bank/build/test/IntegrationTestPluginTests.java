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

import java.io.File;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrationTestPlugin}.
 * <p>
 * Each test applies this plugin before {@code java}, because it reacts to the java plugin
 * rather than requiring it to be there already.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class IntegrationTestPluginTests {

	@TempDir
	private File directory;

	@org.junit.jupiter.api.Test
	void addsNothingToAProjectThatCompilesNoJava() {
		Project project = project();
		assertThat(project.getTasks().findByName(IntegrationTestPlugin.INT_TEST_TASK_NAME)).isNull();
	}

	@org.junit.jupiter.api.Test
	void addsTheSourceSetWhenTheJavaPluginArrivesAfterwards() {
		Project project = javaProject();
		assertThat(sourceSets(project).findByName(IntegrationTestPlugin.INT_TEST_SOURCE_SET_NAME)).isNotNull();
	}

	@org.junit.jupiter.api.Test
	void looksForSourcesUnderTheSourceSetsOwnDirectory() {
		SourceSet intTest = intTestSourceSet(javaProject());
		assertThat(intTest.getJava().getSrcDirs()).singleElement()
			.satisfies((dir) -> assertThat(dir).hasName("java").hasParent(new File(this.directory, "src/intTest")));
		assertThat(intTest.getResources().getSrcDirs()).singleElement()
			.satisfies((dir) -> assertThat(dir).hasName("resources"));
	}

	@org.junit.jupiter.api.Test
	void tellsTheIdeTheSourcesAreTests() {
		Project project = javaProject();
		SourceSet intTest = intTestSourceSet(project);
		IdeaModule module = project.getExtensions().getByType(IdeaModel.class).getModule();
		assertThat(module.getTestSources().getFiles()).containsAll(intTest.getJava().getSrcDirs());
		assertThat(module.getTestResources().getFiles()).containsAll(intTest.getResources().getSrcDirs());
	}

	@org.junit.jupiter.api.Test
	void compilesAgainstTheProjectsOwnClasses() {
		Project project = javaProject();
		SourceSet main = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		assertThat(intTestSourceSet(project).getCompileClasspath().getFiles()).containsAll(main.getOutput().getFiles());
	}

	@org.junit.jupiter.api.Test
	void registersATaskThatRunsTheSourceSet() {
		Project project = javaProject();
		Test task = intTestTask(project);
		SourceSet intTest = intTestSourceSet(project);
		assertThat(task.getTestClassesDirs().getFiles()).isEqualTo(intTest.getOutput().getClassesDirs().getFiles());
		assertThat(task.getClasspath()).isSameAs(intTest.getRuntimeClasspath());
	}

	@org.junit.jupiter.api.Test
	void presentsTheTaskAsAVerificationTask() {
		Test task = intTestTask(javaProject());
		assertThat(task.getGroup()).isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
		assertThat(task.getDescription()).isEqualTo("Runs integration tests.");
	}

	@org.junit.jupiter.api.Test
	void runsAsPartOfCheck() {
		Project project = javaProject();
		assertThat(TaskDependencies
			.namesOf(project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).getDependsOn()))
			.contains(IntegrationTestPlugin.INT_TEST_TASK_NAME);
	}

	@org.junit.jupiter.api.Test
	void yieldsToTheUnitTestsWithoutRequiringThem() {
		Test task = intTestTask(javaProject());
		assertThat(TaskDependencies.namesOf(task.getShouldRunAfter().getDependencies(task)))
			.contains(JavaPlugin.TEST_TASK_NAME);
		assertThat(TaskDependencies.namesOf(task.getDependsOn())).doesNotContain(JavaPlugin.TEST_TASK_NAME);
	}

	/**
	 * These tests drive a build of their own, and Gradle's native-platform loads a
	 * library from an unnamed module. Without the grant, JEP 472 prints a warning today
	 * and fails the call on some later JDK.
	 */
	@org.junit.jupiter.api.Test
	void grantsTheNativeAccessTheBuildItDrivesNeeds() {
		assertThat(intTestTask(javaProject()).getJvmArgs()).contains("--enable-native-access=ALL-UNNAMED");
	}

	@org.junit.jupiter.api.Test
	void putsTheLauncherOnTheRuntimeClasspath() {
		Project project = javaProject();
		String configuration = intTestSourceSet(project).getRuntimeOnlyConfigurationName();
		assertThat(project.getConfigurations().getByName(configuration).getDependencies()).anySatisfy((dependency) -> {
			assertThat(dependency.getGroup()).isEqualTo("org.junit.platform");
			assertThat(dependency.getName()).isEqualTo("junit-platform-launcher");
		});
	}

	private static SourceSetContainer sourceSets(Project project) {
		return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
	}

	private static SourceSet intTestSourceSet(Project project) {
		return sourceSets(project).getByName(IntegrationTestPlugin.INT_TEST_SOURCE_SET_NAME);
	}

	private static Test intTestTask(Project project) {
		return (Test) project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME);
	}

	private Project project() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory).build();
		project.getPlugins().apply(IntegrationTestPlugin.class);
		return project;
	}

	private Project javaProject() {
		Project project = project();
		project.getPlugins().apply("java");
		return project;
	}

}
