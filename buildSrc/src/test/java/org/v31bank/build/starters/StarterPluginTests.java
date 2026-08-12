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
 * <p>
 * A starter has no code, so there is nothing in one to unit test. What these cover is the
 * other half of that sentence: because there is nothing to test, the checks have to be
 * there and have to be attached to {@code check}, or a starter is the one kind of project
 * in the build that nothing verifies at all.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class StarterPluginTests {

	private static final List<String> CHECK_TASK_NAMES = List.of("checkRuntimeClasspathForConflicts",
			"checkRuntimeClasspathForUnconstrainedDirectDependencies", "checkRuntimeClasspathForUnnecessaryExclusions");

	@TempDir
	private File directory;

	/**
	 * A starter hands its dependencies on rather than hiding them, which is the whole
	 * point of one — {@code java} alone would put them on {@code implementation}, where a
	 * consumer never sees them.
	 */
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

	/**
	 * A check nobody runs is a check that does not exist, and nobody types
	 * {@code checkRuntimeClasspathForUnnecessaryExclusions}.
	 */
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

	/**
	 * The runtime classpath, because that is what a consumer gets. Checking the compile
	 * classpath would miss every runtime-only dependency, which for a starter is most of
	 * them.
	 */
	@Test
	void checksWhatTheConsumerActuallyEndsUpWith() {
		Project starter = starter();
		// The source rather than the files: what the check keeps is what the
		// configuration resolves to, and asking for that here would resolve it.
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

	/**
	 * Offered through a configuration rather than put in the jar: a consumer asks for
	 * {@code starterMetadata} and gets the file, and the artifact says which task builds
	 * it, so nobody has to know the path or the order.
	 */
	@Test
	void offersTheMetadataThroughAConfigurationOfItsOwn() {
		Project starter = starter();
		assertThat(starter.getConfigurations().findByName("starterMetadata")).isNotNull();
		assertThat(starter.getConfigurations().getByName("starterMetadata").getArtifacts()).singleElement()
			.satisfies((artifact) -> assertThat(
					TaskDependencies.namesOf(artifact.getBuildDependencies().getDependencies(null)))
				.contains("starterMetadata"));
	}

	/**
	 * The starter's own graph, not a copy of it: the check has to fail when a dependency
	 * is added to the build file, which is the only way it can be added.
	 */
	@Test
	void describesWhatTheStarterResolvesTo() {
		Project starter = starter();
		StarterMetadata metadata = (StarterMetadata) starter.getTasks().getByName("starterMetadata");
		assertThat(metadata.getDependencies())
			.isSameAs(starter.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
		assertThat(metadata.getStarterName().get()).isEqualTo("V31-web-spring-boot-starter");
	}

	/**
	 * The conventions and the exclusions check both reach for a platform by path, so the
	 * hierarchy has to exist before a starter can be configured at all.
	 * @return a configured starter
	 */
	private Project starter() {
		Project root = ProjectBuilder.builder().withName("V31").withProjectDir(this.directory).build();
		Project platform = ProjectBuilder.builder().withName("platform").withParent(root).build();
		ProjectBuilder.builder().withName("V31-internal-dependencies").withParent(platform).build();
		ProjectBuilder.builder().withName("V31-dependencies").withParent(platform).build();
		Project starter = ProjectBuilder.builder().withName("V31-web-spring-boot-starter").withParent(root).build();
		// Supplied by gradle.properties in the real build, and by nothing at all here.
		starter.getExtensions().getExtraProperties().set("buildJavaVersion", "25");
		starter.getExtensions().getExtraProperties().set("runtimeJavaVersion", "25");
		starter.getExtensions().getExtraProperties().set("checkstyleToolVersion", "12.3.1");
		starter.getPlugins().apply(StarterPlugin.class);
		return starter;
	}

}
