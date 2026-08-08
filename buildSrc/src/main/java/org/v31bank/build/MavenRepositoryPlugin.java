package org.v31bank.build;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Publishes a project into a Maven repository inside its own build directory.
 * <p>
 * This is what lets the build resolve V31 the way a consumer does — from a repository, by
 * coordinate — without publishing anything anywhere real and without going anywhere near
 * the developer's {@code ~/.m2}. The repository is exposed as a configuration named
 * {@code mavenRepository}, so a project that wants to resolve against it depends on this
 * one through that configuration.
 * <p>
 * A project's own artifacts are not enough to resolve it: whoever consumes it will also
 * need whatever V31 projects it depends on, and the platform its versions come from. Both
 * are pulled in here, so asking for one project's repository yields everything needed to
 * resolve it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class MavenRepositoryPlugin implements Plugin<Project> {

	/**
	 * The configuration carrying the repository directory, and everything that has to be
	 * in it.
	 */
	public static final String MAVEN_REPOSITORY_CONFIGURATION_NAME = "mavenRepository";

	private static final String PUBLISH_TASK_NAME = "publishV31PublicationToProjectRepository";

	private static final String REPOSITORY_NAME = "project";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(MavenPublishPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		File location = project.getLayout().getBuildDirectory().dir("maven-repository").get().getAsFile();
		publishing.getRepositories().maven((repository) -> {
			repository.setName(REPOSITORY_NAME);
			repository.setUrl(location.toURI());
		});
		project.getTasks()
			.matching((task) -> PUBLISH_TASK_NAME.equals(task.getName()))
			.all((task) -> setUpProjectRepository(project, task, location));
	}

	private void setUpProjectRepository(Project project, Task publishTask, File location) {
		// Emptied first, so a rename or a removal cannot leave a stale artifact behind
		// for something to resolve against.
		publishTask.doFirst(new CleanAction(location));
		Configuration repository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);
		project.getArtifacts().add(repository.getName(), location, (artifact) -> artifact.builtBy(publishTask));
		DependencySet contents = repository.getDependencies();
		project.getPlugins()
			.withType(JavaPlugin.class, (javaPlugin) -> addProjectDependencies(project,
					JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, contents));
		project.getPlugins()
			.withType(JavaLibraryPlugin.class, (javaLibraryPlugin) -> addProjectDependencies(project,
					JavaPlugin.API_CONFIGURATION_NAME, contents));
		project.getPlugins()
			.withType(JavaPlatformPlugin.class, (javaPlatformPlugin) -> addProjectDependencies(project,
					JavaPlatformPlugin.API_CONFIGURATION_NAME, contents));
	}

	/**
	 * Pulls in the repositories of the V31 projects this one depends on, so that
	 * resolving it from the result does not stop at the first project dependency.
	 * @param project the project being configured
	 * @param configurationName the configuration to read dependencies from
	 * @param contents the repository's contents, to add to
	 */
	private void addProjectDependencies(Project project, String configurationName, DependencySet contents) {
		project.getConfigurations()
			.getByName(configurationName)
			.getDependencies()
			.withType(ProjectDependency.class)
			.all((dependency) -> {
				ProjectDependency copy = dependency.copy();
				if (copy.getAttributes().isEmpty()) {
					copy.setTargetConfiguration(MAVEN_REPOSITORY_CONFIGURATION_NAME);
				}
				contents.add(copy);
			});
	}

	private record CleanAction(File location) implements Action<Task> {

		/**
		 * Deleted with plain file operations rather than through the project, which is
		 * not reachable from a task at execution time under the configuration cache.
		 * @param task the publish task about to run
		 */
		@Override
		public void execute(Task task) {
			delete(this.location);
		}

		private void delete(File file) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					delete(child);
				}
			}
			file.delete();
		}
	}

}
