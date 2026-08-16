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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

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
 * Not declared by a project; {@link DeployedPlugin} applies it. It lets the build resolve
 * V31 by coordinate the way a consumer does, without publishing anywhere real and without
 * touching the developer's {@code ~/.m2}. One project's own artifacts are not enough to
 * resolve it, so the V31 projects it depends on contribute their repositories too.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class MavenRepositoryPlugin implements Plugin<Project> {

	/**
	 * Name of the configuration a project depends on to resolve against the repository.
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
		// Emptied first, so a rename or a removal leaves no stale artifact to resolve
		// against.
		publishTask.doFirst(new CleanAction(location));
		Configuration repository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);
		project.getArtifacts().add(repository.getName(), location, (artifact) -> artifact.builtBy(publishTask));
		DependencySet contents = repository.getDependencies();
		project.getPlugins()
			.withType(JavaPlugin.class,
					(_) -> addProjectDependencies(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, contents));
		project.getPlugins()
			.withType(JavaLibraryPlugin.class,
					(_) -> addProjectDependencies(project, JavaPlugin.API_CONFIGURATION_NAME, contents));
		project.getPlugins()
			.withType(JavaPlatformPlugin.class,
					(_) -> addProjectDependencies(project, JavaPlatformPlugin.API_CONFIGURATION_NAME, contents));
	}

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
		 * Deletes with plain file operations because the project is not reachable from a
		 * task at execution time under the configuration cache.
		 * @param task the publish task about to run
		 */
		@Override
		public void execute(Task task) {
			delete(this.location);
		}

		private void delete(File file) {
			Path root = file.toPath();
			if (!Files.exists(root)) {
				return;
			}
			try (Stream<Path> paths = Files.walk(root)) {
				// Deepest first: a directory cannot go until it is empty.
				for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
					Files.delete(path);
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to clear " + file, ex);
			}
		}
	}

}
