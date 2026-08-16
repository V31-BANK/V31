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

package org.v31bank.build;

import java.io.File;
import java.util.List;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConventionsPlugin}.
 * <p>
 * Each test applies the conventions first and the plugin second, the order the real build
 * uses: the root applies the conventions before a subproject's own build file has run.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ConventionsPluginTests {

	private static final String DEPENDENCY_MANAGEMENT = "dependencyManagement";

	@TempDir
	private File directory;

	@Test
	void addsNothingToAProjectWithNoPlugins() {
		Project project = conventions();
		assertThat(project.getConfigurations().findByName(DEPENDENCY_MANAGEMENT)).isNull();
	}

	@Test
	void addsTheConventionsWhenTheJavaPluginArrivesAfterwards() {
		Project project = conventions();
		project.getPlugins().apply("java-library");
		assertThat(project.getConfigurations().findByName(DEPENDENCY_MANAGEMENT)).isNotNull();
	}

	@Test
	void leavesAPlatformWithoutTheJavaConventions() {
		Project project = conventions();
		project.getPlugins().apply("java-platform");
		assertThat(project.getConfigurations().findByName(DEPENDENCY_MANAGEMENT)).isNull();
	}

	@Test
	void keepsTheDependencyManagementConfigurationOutOfPublishedMetadata() {
		Configuration dependencyManagement = javaProject().getConfigurations().getByName(DEPENDENCY_MANAGEMENT);
		assertThat(dependencyManagement.isCanBeConsumed()).isFalse();
		assertThat(dependencyManagement.isCanBeResolved()).isFalse();
	}

	@Test
	void makesEveryResolvableConfigurationTakeItsVersionsFromThePlatform() {
		Project project = javaProject();
		for (String name : List.of("compileClasspath", "runtimeClasspath", "testCompileClasspath",
				"testRuntimeClasspath", "annotationProcessor")) {
			Configuration configuration = project.getConfigurations().getByName(name);
			assertThat(parentNames(configuration)).as(name).contains(DEPENDENCY_MANAGEMENT);
		}
	}

	@Test
	void leavesTheDeclarationBucketsAlone() {
		Project project = javaProject();
		for (String name : List.of("api", "implementation", "compileOnly", "runtimeOnly")) {
			Configuration configuration = project.getConfigurations().getByName(name);
			assertThat(parentNames(configuration)).as(name).doesNotContain(DEPENDENCY_MANAGEMENT);
		}
	}

	@Test
	void makesThePlatformsVersionsMandatory() {
		Configuration dependencyManagement = javaProject().getConfigurations().getByName(DEPENDENCY_MANAGEMENT);
		assertThat(dependencyManagement.getDependencies()).singleElement()
			.satisfies((dependency) -> assertThat(dependency.getName()).isEqualTo("V31-internal-dependencies"));
	}

	@Test
	void compilesWithTheBuildJdkAndTargetsTheRuntimeOne() {
		Project project = javaProject();
		JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
		assertThat(java.getToolchain().getLanguageVersion().get().asInt()).isEqualTo(25);
		assertThat(project.getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getRelease().get()).isEqualTo(21));
	}

	@Test
	void turnsOnTheWarningsThatCatchSomething() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-Xlint:unchecked",
					"-Xlint:deprecation", "-Xlint:rawtypes", "-Xlint:varargs"));
	}

	@Test
	void refusesToCompileCodeThatWarns() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-Werror"));
	}

	@Test
	void keepsTheParameterNamesInTheBytecode() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-parameters"));
	}

	@Test
	void holdsTheCodeToOneShapeAndOneSetOfHabits() {
		Project project = javaProject();
		assertThat(project.getPlugins().hasPlugin(SpringJavaFormatPlugin.class)).isTrue();
		assertThat(project.getPlugins().hasPlugin(CheckstylePlugin.class)).isTrue();
	}

	@Test
	void takesTheCheckstyleVersionFromTheBuildRatherThanGradlesDefault() {
		assertThat(checkstyle(javaProject()).getToolVersion()).isEqualTo("12.3.1");
	}

	@Test
	void readsTheCheckstyleConfigurationFromTheRoot() {
		Project project = javaProject();
		File config = checkstyle(project).getConfigDirectory().get().getAsFile();
		assertThat(config).isEqualTo(new File(project.getRootDir(), "config/checkstyle"));
	}

	@Test
	void putsBothCheckstyleAndTheSpringRulesOnTheCheckstyleClasspath() {
		assertThat(javaProject().getConfigurations().getByName("checkstyle").getDependencies())
			.extracting(Dependency::getName)
			.contains("checkstyle", "spring-javaformat-checkstyle");
	}

	@Test
	void runsTestsOnTheJUnitPlatform() {
		assertThat(javaProject().getTasks().withType(org.gradle.api.tasks.testing.Test.class)).isNotEmpty()
			.allSatisfy((test) -> assertThat(test.getMaxHeapSize()).isEqualTo("1536M"));
	}

	@Test
	void putsTheLauncherOnTheTestRuntimeClasspath() {
		Configuration testRuntimeOnly = javaProject().getConfigurations()
			.getByName(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME);
		assertThat(testRuntimeOnly.getDependencies()).anySatisfy((dependency) -> {
			assertThat(dependency.getGroup()).isEqualTo("org.junit.platform");
			assertThat(dependency.getName()).isEqualTo("junit-platform-launcher");
		});
	}

	private static CheckstyleExtension checkstyle(Project project) {
		return project.getExtensions().getByType(CheckstyleExtension.class);
	}

	private static List<String> parentNames(Configuration configuration) {
		return configuration.getExtendsFrom().stream().map(Configuration::getName).toList();
	}

	private Project conventions() {
		// The conventions depend on the platform project by path, so it has to exist first.
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		Project project = ProjectBuilder.builder().withName("under-test").withParent(root).build();
		// Supplied by gradle.properties in the real build; the conventions read them, never default.
		project.getExtensions().getExtraProperties().set("buildJavaVersion", "25");
		project.getExtensions().getExtraProperties().set("runtimeJavaVersion", "21");
		project.getExtensions().getExtraProperties().set("checkstyleToolVersion", "12.3.1");
		project.getPlugins().apply(ConventionsPlugin.class);
		return project;
	}

	private Project javaProject() {
		Project project = conventions();
		project.getPlugins().apply("java-library");
		return project;
	}

}
