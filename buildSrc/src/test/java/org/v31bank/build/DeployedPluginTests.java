package org.v31bank.build;

import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DeployedPlugin}.
 * <p>
 * A project declares this plugin for itself, so there is no rule here about which
 * projects publish — applying it is the decision. What is tested is what applying it
 * does, in the order it happens: the plugin arrives before the project's own build file
 * has said whether it is a library or a platform.
 * <p>
 * That the right component actually reaches the publication is asserted by the build
 * itself, which publishes a jar for every library and a pom for the platform.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class DeployedPluginTests {

	@Test
	void appliesMavenPublish() {
		assertThat(project().getPlugins().hasPlugin(MavenPublishPlugin.class)).isTrue();
	}

	@Test
	void createsOnePublication() {
		assertThat(publishing(project()).getPublications().getNames()).containsExactly("v31");
	}

	@Test
	void waitsForALibraryToSayWhatItIs() {
		Project project = project();
		project.getPlugins().apply("java-library");
		assertThat(project.getComponents().findByName("java")).isNotNull();
		assertThat(publishing(project).getPublications().getNames()).containsExactly("v31");
	}

	@Test
	void waitsForAPlatformToSayWhatItIs() {
		Project project = project();
		project.getPlugins().apply("java-platform");
		assertThat(project.getComponents().findByName("javaPlatform")).isNotNull();
		assertThat(publishing(project).getPublications().getNames()).containsExactly("v31");
	}

	/**
	 * Neither component exists yet when the plugin is applied, which is why it has to
	 * pick one up whenever it appears rather than look for it on the spot.
	 */
	@Test
	void findsNoComponentWhenApplied() {
		Project project = project();
		assertThat(project.getComponents().findByName("java")).isNull();
		assertThat(project.getComponents().findByName("javaPlatform")).isNull();
	}

	/**
	 * Gradle otherwise refuses metadata whose dependencies carry no version. Here that is
	 * the intended shape: the versions live in the BOM a consumer imports.
	 */
	@Test
	void allowsTheVersionLessDependenciesTheBomExistsToSupply() {
		Project project = project();
		project.getPlugins().apply("java-library");
		assertThat(project.getTasks().withType(GenerateModuleMetadata.class)).isNotEmpty()
			.allSatisfy((task) -> assertThat(task.getSuppressedValidationErrors().get())
				.contains("dependencies-without-versions"));
	}

	private static PublishingExtension publishing(Project project) {
		return project.getExtensions().getByType(PublishingExtension.class);
	}

	private Project project() {
		Project project = ProjectBuilder.builder().withName("V31-core").build();
		project.getPlugins().apply(DeployedPlugin.class);
		return project;
	}

}
