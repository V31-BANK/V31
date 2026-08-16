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

package org.v31bank.build.starters;

import java.io.File;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.classpath.CheckClasspathForConflicts;
import org.v31bank.build.classpath.CheckClasspathForUnconstrainedDirectDependencies;
import org.v31bank.build.classpath.CheckClasspathForUnnecessaryExclusions;
import org.v31bank.build.classpath.ClasspathCheck;
import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StarterPlugin}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class StarterPluginTests {

	private static final List<String> CHECK_TASK_NAMES = List.of("checkRuntimeClasspathForConflicts",
			"checkRuntimeClasspathForUnconstrainedDirectDependencies", "checkRuntimeClasspathForUnnecessaryExclusions");

	@TempDir
	private File directory;

	@Test
	void makesTheStarterALibrary() {
		assertThat(starter().getPlugins().hasPlugin(JavaLibraryPlugin.class)).isTrue();
	}

	@Test
	void publishesTheStarter() {
		Project starter = starter();
		assertThat(starter.getExtensions().getByType(PublishingExtension.class).getPublications().getNames())
			.containsExactly("v31");
	}

	@Test
	void holdsTheStarterToTheSameConventionsAsEverythingElse() {
		assertThat(starter().getConfigurations().findByName("dependencyManagement")).isNotNull();
	}

	@Test
	void registersEveryCheckAStarterCanFail() {
		Project starter = starter();
		assertThat(CHECK_TASK_NAMES).allSatisfy((name) -> assertThat(starter.getTasks().findByName(name)).isNotNull());
	}

	@Test
	void registersEachCheckAsTheKindOfCheckItIs() {
		Project starter = starter();
		assertThat(starter.getTasks().getByName(CHECK_TASK_NAMES.get(0)))
			.isInstanceOf(CheckClasspathForConflicts.class);
		assertThat(starter.getTasks().getByName(CHECK_TASK_NAMES.get(1)))
			.isInstanceOf(CheckClasspathForUnconstrainedDirectDependencies.class);
		assertThat(starter.getTasks().getByName(CHECK_TASK_NAMES.get(2)))
			.isInstanceOf(CheckClasspathForUnnecessaryExclusions.class);
	}

	@Test
	void runsEveryCheckAsPartOfCheck() {
		Project starter = starter();
		List<String> dependencies = TaskDependencies
			.namesOf(starter.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).getDependsOn());
		assertThat(dependencies).containsAll(CHECK_TASK_NAMES);
	}

	@Test
	void presentsTheChecksAsVerificationTasks() {
		Project starter = starter();
		assertThat(CHECK_TASK_NAMES).allSatisfy((name) -> assertThat(starter.getTasks().getByName(name).getGroup())
			.isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP));
	}

	@Test
	void checksWhatTheConsumerActuallyEndsUpWith() {
		Project starter = starter();
		// Held as the source, because asking for the files would resolve the
		// configuration.
		Object runtimeClasspath = starter.getConfigurations()
			.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		assertThat(CHECK_TASK_NAMES).allSatisfy((name) -> {
			ClasspathCheck check = (ClasspathCheck) starter.getTasks().getByName(name);
			assertThat(check.getClasspathFiles().getFrom()).containsExactly(runtimeClasspath);
		});
	}

	@Test
	void registersTheMetadataTask() {
		assertThat(starter().getTasks().findByName("starterMetadata")).isInstanceOf(StarterMetadata.class);
	}

	@Test
	void offersTheMetadataThroughAConfigurationOfItsOwn() {
		Project starter = starter();
		assertThat(starter.getConfigurations().findByName("starterMetadata")).isNotNull();
		assertThat(starter.getConfigurations().getByName("starterMetadata").getArtifacts()).singleElement()
			.satisfies((artifact) -> assertThat(
					TaskDependencies.namesOf(artifact.getBuildDependencies().getDependencies(null)))
				.contains("starterMetadata"));
	}

	@Test
	void describesWhatTheStarterResolvesTo() {
		Project starter = starter();
		StarterMetadata metadata = (StarterMetadata) starter.getTasks().getByName("starterMetadata");
		assertThat(metadata.getDependencyFiles().getFrom())
			.containsExactly(starter.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
		assertThat(metadata.getStarterName().get()).isEqualTo("V31-web-spring-boot-starter");
	}

	private Project starter() {
		// Both platform projects are reached by path, so they have to exist first.
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		ProjectBuilder.builder().withName("V31-dependencies").withParent(platform).build();
		Project starter = ProjectBuilder.builder().withName("V31-web-spring-boot-starter").withParent(root).build();
		// Supplied by gradle.properties in the real build; the conventions read them,
		// never default.
		starter.getExtensions().getExtraProperties().set("buildJavaVersion", "25");
		starter.getExtensions().getExtraProperties().set("runtimeJavaVersion", "25");
		starter.getExtensions().getExtraProperties().set("checkstyleToolVersion", "12.3.1");
		starter.getPlugins().apply(StarterPlugin.class);
		return starter;
	}

}
