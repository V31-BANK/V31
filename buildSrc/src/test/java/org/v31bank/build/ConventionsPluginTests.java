package org.v31bank.build;

import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

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

	@Test
	void buildsAgainstTheJavaVersionThePlatformTargets() {
		Project project = javaProject();
		JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
		assertThat(java.getToolchain().getLanguageVersion().get().asInt()).isEqualTo(21);
		project.getTasks()
			.withType(JavaCompile.class)
			.forEach((compile) -> assertThat(compile.getOptions().getRelease().get()).isEqualTo(21));
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
	 * The conventions depend on the platform project by path, so the hierarchy has to
	 * exist before they can be applied.
	 */
	private Project conventions() {
		Project root = ProjectBuilder.builder().withName("V31").build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		Project project = ProjectBuilder.builder().withName("under-test").withParent(root).build();
		project.getPlugins().apply(ConventionsPlugin.class);
		return project;
	}

	private Project javaProject() {
		Project project = conventions();
		project.getPlugins().apply("java-library");
		return project;
	}

	private static List<String> parentNames(Configuration configuration) {
		return configuration.getExtendsFrom().stream().map(Configuration::getName).toList();
	}

}
