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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MavenRepositoryPlugin}.
 * <p>
 * The repository exists so that the build can resolve V31 the way a consumer does — by
 * coordinate, out of a repository — without publishing anywhere real. Most of what is
 * asserted here is about what ends up <em>in</em> it: a project's own artifacts are not
 * enough, because whoever resolves them needs the V31 projects behind them too.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MavenRepositoryPluginTests {

	private static final String PUBLISH_TASK_NAME = "publishV31PublicationToProjectRepository";

	@TempDir
	private File directory;

	@Test
	void appliesMavenPublish() {
		assertThat(project().getPlugins().hasPlugin(MavenPublishPlugin.class)).isTrue();
	}

	/**
	 * Inside the build directory, and nowhere near the developer's {@code ~/.m2}, where
	 * an artifact from a build would outlive it and be resolved by something else.
	 */
	@Test
	void publishesIntoTheBuildDirectory() {
		Project project = project();
		MavenArtifactRepository repository = repository(project);
		assertThat(new File(repository.getUrl())).isEqualTo(buildDirectory(project, "maven-repository"));
	}

	@Test
	void namesTheRepositoryAfterTheProjectItBelongsTo() {
		assertThat(repository(project()).getName()).isEqualTo("project");
	}

	/**
	 * The repository is only worth offering once there is a publication to put in it.
	 * Applied on its own the plugin adds the repository and stops.
	 */
	@Test
	void offersNothingUntilThereIsAPublicationToPut() {
		Project project = project();
		assertThat(project.getTasks().findByName(PUBLISH_TASK_NAME)).isNull();
		assertThat(project.getConfigurations().findByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME))
			.isNull();
	}

	@Test
	void offersTheRepositoryThroughAConfigurationOfItsOwn() {
		Project project = deployedProject();
		assertThat(project.getConfigurations().findByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME))
			.isNotNull();
	}

	/**
	 * Asking for the configuration is what runs the publish task, so a consumer never has
	 * to know the path or the order.
	 */
	@Test
	void buildsTheRepositoryByPublishingIntoIt() {
		Project project = deployedProject();
		PublishArtifact artifact = mavenRepository(project).getArtifacts().iterator().next();
		assertThat(artifact.getFile()).isEqualTo(buildDirectory(project, "maven-repository"));
		assertThat(TaskDependencies.namesOf(artifact.getBuildDependencies().getDependencies(null)))
			.contains(PUBLISH_TASK_NAME);
	}

	/**
	 * Resolving one project from the result must not stop at the first project
	 * dependency, so each one is pulled in through its own repository rather than as an
	 * ordinary dependency.
	 */
	@Test
	void pullsInTheRepositoriesOfTheProjectsItDependsOn() {
		Project project = deployedProject();
		project.getDependencies().add("api", project.getDependencies().project(Map.of("path", ":V31-core")));
		assertThat(mavenRepository(project).getDependencies()).singleElement()
			.asInstanceOf(InstanceOfAssertFactories.type(ProjectDependency.class))
			.satisfies((dependency) -> {
				assertThat(dependency.getPath()).isEqualTo(":V31-core");
				assertThat(dependency.getTargetConfiguration())
					.isEqualTo(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME);
			});
	}

	/**
	 * A rename or a removal would otherwise leave the old artifact in the repository for
	 * something to go on resolving against, which is the one failure a throwaway
	 * repository is supposed to make impossible.
	 */
	@Test
	void emptiesTheRepositoryBeforePublishingIntoIt() throws IOException {
		Project project = deployedProject();
		Path stale = buildDirectory(project, "maven-repository").toPath()
			.resolve("org/v31bank/V31-core/0.1.0/V31-core-0.1.0.jar");
		Files.createDirectories(stale.getParent());
		Files.writeString(stale, "stale");
		Task publish = project.getTasks().getByName(PUBLISH_TASK_NAME);
		runFirstActionOf(publish);
		assertThat(stale).doesNotExist();
		assertThat(buildDirectory(project, "maven-repository")).doesNotExist();
	}

	/**
	 * The first build has nothing to clear, and a repository that was never created is
	 * not a failure.
	 */
	@Test
	void clearsARepositoryThatIsNotThereYetWithoutComplaining() {
		Project project = deployedProject();
		assertThat(buildDirectory(project, "maven-repository")).doesNotExist();
		runFirstActionOf(project.getTasks().getByName(PUBLISH_TASK_NAME));
	}

	@SuppressWarnings("unchecked")
	private static void runFirstActionOf(Task task) {
		// The publish action itself would need a real repository behind it; only the
		// doFirst this plugin adds is under test, and it is the one at the front.
		((Action<Task>) task.getActions().get(0)).execute(task);
	}

	private static MavenArtifactRepository repository(Project project) {
		PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
		return (MavenArtifactRepository) publishing.getRepositories().iterator().next();
	}

	private static Configuration mavenRepository(Project project) {
		return project.getConfigurations().getByName(MavenRepositoryPlugin.MAVEN_REPOSITORY_CONFIGURATION_NAME);
	}

	private static File buildDirectory(Project project, String path) {
		return project.getLayout().getBuildDirectory().dir(path).get().getAsFile();
	}

	private Project project() {
		Project project = ProjectBuilder.builder().withName("V31-web").withProjectDir(this.directory).build();
		project.getPlugins().apply(MavenRepositoryPlugin.class);
		return project;
	}

	/**
	 * The publish task the plugin waits for is created by the publication, which is
	 * {@link DeployedPlugin}'s job — so everything past the repository itself is only
	 * reachable through a project that is actually deployed.
	 * @return a project that publishes
	 */
	private Project deployedProject() {
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		ProjectBuilder.builder().withName("V31-core").withParent(root).build();
		Project project = ProjectBuilder.builder().withName("V31-web").withParent(root).build();
		project.getPlugins().apply("java-library");
		project.getPlugins().apply(DeployedPlugin.class);
		return project;
	}

}
