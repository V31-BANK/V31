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
 * Each test applies the plugin before saying whether the project is a library or a
 * platform, because the plugin has to cope with a component that does not exist yet.
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

	@Test
	void findsNoComponentWhenApplied() {
		Project project = project();
		assertThat(project.getComponents().findByName("java")).isNull();
		assertThat(project.getComponents().findByName("javaPlatform")).isNull();
	}

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
