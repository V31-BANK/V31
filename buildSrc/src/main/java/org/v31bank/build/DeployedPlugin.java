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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.tasks.GenerateModuleMetadata;

/**
 * Marks a project as one that is published, and applies everything that follows from
 * that.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.deployed")
 * }
 * </pre>
 *
 * Applying it is the decision — there is no rule elsewhere inferring which projects
 * publish from where they sit or what they are called. A {@code -service} is an
 * application and simply does not declare it; nor does the internal platform, whose
 * versions reach a consumer through {@code V31-dependencies} instead.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class DeployedPlugin implements Plugin<Project> {

	private static final String PUBLICATION_NAME = "v31";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(MavenPublishPlugin.class);
		project.getPluginManager().apply(MavenRepositoryPlugin.class);
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		MavenPublication publication = publishing.getPublications().create(PUBLICATION_NAME, MavenPublication.class);
		// Matched rather than looked up: a subproject's own build file is evaluated
		// after this one, so the component does not exist yet at this point.
		publishComponent(project, publication, JavaPlugin.class, "java");
		publishComponent(project, publication, JavaPlatformPlugin.class, "javaPlatform");
		allowDependenciesWithoutVersions(project);
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
					(_) -> project.getComponents()
						.matching((component) -> componentName.equals(component.getName()))
						.all(publication::from));
	}

}
