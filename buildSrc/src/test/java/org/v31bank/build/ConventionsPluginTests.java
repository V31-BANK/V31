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
 * Every convention reacts to a plugin rather than being applied outright, because the
 * root build applies this before a subproject's own build file has run. The tests
 * therefore apply the conventions first and the plugin second, which is the order that
 * actually occurs.
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

	/**
	 * A platform has no source to compile and no runtime classpath, so the Java
	 * conventions have to stay away from it.
	 */
	@Test
	void leavesAPlatformWithoutTheJavaConventions() {
		Project project = conventions();
		project.getPlugins().apply("java-platform");
		assertThat(project.getConfigurations().findByName(DEPENDENCY_MANAGEMENT)).isNull();
	}

	/**
	 * Neither consumable nor resolvable, which is what keeps the platform out of the
	 * project's published metadata.
	 */
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

	/**
	 * The buckets a dependency is declared in must not inherit it, or the platform ends
	 * up in the published variants.
	 */
	@Test
	void leavesTheDeclarationBucketsAlone() {
		Project project = javaProject();
		for (String name : List.of("api", "implementation", "compileOnly", "runtimeOnly")) {
			Configuration configuration = project.getConfigurations().getByName(name);
			assertThat(parentNames(configuration)).as(name).doesNotContain(DEPENDENCY_MANAGEMENT);
		}
	}

	/**
	 * Enforced rather than suggested: a transitive dependency must not be able to pull
	 * the build onto a version the platform did not choose.
	 */
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

	/**
	 * Without them the compiler stays quiet about the things that compile and then fail:
	 * an unchecked cast, a call to something already deprecated, a raw type, an argument
	 * that will not land in the varargs array the caller meant.
	 */
	@Test
	void turnsOnTheWarningsThatCatchSomething() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-Xlint:unchecked",
					"-Xlint:deprecation", "-Xlint:rawtypes", "-Xlint:varargs"));
	}

	/**
	 * And makes them stop the build, which is the whole of what a warning is worth. Left
	 * as warnings they are printed, scrolled past, and joined by the next one.
	 */
	@Test
	void refusesToCompileCodeThatWarns() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-Werror"));
	}

	/**
	 * Spring reads constructor and method parameter names at runtime, and without this
	 * they are {@code arg0} and the binding fails on something that looks unrelated.
	 */
	@Test
	void keepsTheParameterNamesInTheBytecode() {
		assertThat(javaProject().getTasks().withType(JavaCompile.class)).isNotEmpty()
			.allSatisfy((compile) -> assertThat(compile.getOptions().getCompilerArgs()).contains("-parameters"));
	}

	/**
	 * Two tools that answer different questions: the formatter settles what the code
	 * looks like by rewriting it, checkstyle settles what it is allowed to do.
	 */
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

	/**
	 * One configuration at the root rather than a copy per project, so a rule is added in
	 * one place and applies everywhere.
	 */
	@Test
	void readsTheCheckstyleConfigurationFromTheRoot() {
		Project project = javaProject();
		File config = checkstyle(project).getConfigDirectory().get().getAsFile();
		assertThat(config).isEqualTo(new File(project.getRootDir(), "config/checkstyle"));
	}

	/**
	 * The rules that ship with the formatter, so the two tools cannot disagree about the
	 * same file — and checkstyle itself, because the version Gradle would otherwise pick
	 * is not the one those rules were built against.
	 */
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

	/**
	 * Gradle discovers nothing without the launcher, and reports that as "no tests found"
	 * rather than as a missing dependency.
	 */
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

	/**
	 * The conventions depend on the platform project by path, so the hierarchy has to
	 * exist before they can be applied.
	 * @return a project with the conventions and nothing else
	 */
	private Project conventions() {
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		Project project = ProjectBuilder.builder().withName("under-test").withParent(root).build();
		// Supplied by gradle.properties in the real build, and by nothing at all here.
		// The conventions read them rather than defaulting, so they have to be set before
		// the plugin is applied.
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
