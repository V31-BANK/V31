package org.v31bank.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Conventions applied to every project that publishes something.
 * <p>
 * Which projects those are is decided in {@code gradle/publications.gradle.kts}, by
 * applying {@code maven-publish}. This reacts to that and says how they publish, so the
 * two questions stay separate.
 * <p>
 * The versions of a published artifact's dependencies are deliberately left as declared,
 * which for most of them means absent: they come from the platform, and a consumer gets
 * them by importing {@code V31-dependencies}. That BOM is the supported way to depend on
 * V31, and it is what keeps a consumer on a combination of versions this build actually
 * tested together.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MavenPublishingConventions {

	void apply(Project project) {
		project.getPlugins().withType(MavenPublishPlugin.class, (maven) -> {
			PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
			MavenPublication publication = publishing.getPublications().create("v31", MavenPublication.class);
			// Matched rather than looked up: a subproject's own build file is evaluated
			// after this one, so the component does not exist yet at this point.
			publishComponent(project, publication, JavaPlugin.class, "java");
			publishComponent(project, publication, JavaPlatformPlugin.class, "javaPlatform");
			allowDependenciesWithoutVersions(project);
		});
	}

	/**
	 * Lets a module publish the version-less dependencies it declares.
	 * <p>
	 * Gradle rejects module metadata whose dependencies carry no version, on the
	 * assumption that nobody could resolve it. Here that is the intended shape: the
	 * versions live in {@code V31-dependencies}, and importing that BOM is how a consumer
	 * is meant to depend on V31. Writing resolved versions into each artifact instead
	 * would pin every consumer to whatever this build happened to resolve, and make the
	 * BOM pointless.
	 * @param project the project to configure
	 */
	private void allowDependenciesWithoutVersions(Project project) {
		project.getTasks()
			.withType(GenerateModuleMetadata.class)
			.configureEach((task) -> task.getSuppressedValidationErrors().add("dependencies-without-versions"));
	}

	private void publishComponent(Project project, MavenPublication publication,
			Class<? extends Plugin<Project>> pluginType, String componentName) {
		project.getPlugins()
			.withType(pluginType,
					(plugin) -> project.getComponents()
						.matching((component) -> componentName.equals(component.getName()))
						.all(publication::from));
	}

}
