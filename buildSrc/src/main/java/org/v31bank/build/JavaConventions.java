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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import io.spring.javaformat.gradle.tasks.CheckFormat;
import io.spring.javaformat.gradle.tasks.Format;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

/**
 * Conventions applied to every project that builds Java.
 * <p>
 * Not a registered Gradle plugin: {@code ConventionsPlugin} instantiates it directly.
 * These rules used to be a {@code subprojects} block in the root build file, and as
 * ordinary code they can be read, compiled and tested.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class JavaConventions {

	private static final String INTERNAL_DEPENDENCIES = ":platform:V31-internal-dependencies";

	/**
	 * Names the JDK that compiles the code and runs the tests.
	 */
	private static final String BUILD_JAVA_VERSION = "buildJavaVersion";

	/**
	 * Names the JDK the result must run on, which is a different question.
	 */
	private static final String RUNTIME_JAVA_VERSION = "runtimeJavaVersion";

	private static final String CHECKSTYLE_TOOL_VERSION = "checkstyleToolVersion";

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (_) -> {
			configureSpringJavaFormat(project);
			configureDependencyManagement(project);
			configureJavaCompilation(project);
			configureTests(project);
		});
	}

	/**
	 * The formatter rewrites the file, so its shape is never discussed; checkstyle limits
	 * what the code may do. Its rules ship with the formatter, so the two cannot
	 * disagree.
	 * @param project the project to configure
	 */
	private void configureSpringJavaFormat(Project project) {
		project.getPluginManager().apply(SpringJavaFormatPlugin.class);
		project.getTasks().withType(Format.class).configureEach((format) -> format.setEncoding("UTF-8"));
		project.getPluginManager().apply(CheckstylePlugin.class);
		CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
		Object toolVersion = project.findProperty(CHECKSTYLE_TOOL_VERSION);
		if (toolVersion != null) {
			checkstyle.setToolVersion(toolVersion.toString());
		}
		checkstyle.getConfigDirectory().set(project.getRootProject().file("config/checkstyle"));
		String formatVersion = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
		DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
		checkstyleDependencies
			.add(project.getDependencies().create("com.puppycrawl.tools:checkstyle:" + checkstyle.getToolVersion()));
		checkstyleDependencies.add(
				project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + formatVersion));
		project.getTasks().withType(CheckFormat.class).configureEach(this::excludeGeneratedSources);
		project.getTasks().withType(Checkstyle.class).configureEach(this::excludeGeneratedSources);
	}

	private void excludeGeneratedSources(SourceTask task) {
		task.exclude((candidate) -> {
			String path = candidate.getFile().getPath().replace(File.separatorChar, '/');
			return path.contains("/apis/");
		});
	}

	/**
	 * The platform goes into a configuration of its own rather than into
	 * {@code implementation}, so that it reaches every classpath including source sets
	 * added later, stays out of the published metadata where a consumer would have to
	 * resolve a platform internal to this build, and being enforced cannot be overridden
	 * by a transitive dependency.
	 * @param project the project to configure
	 */
	private void configureDependencyManagement(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration dependencyManagement = configurations.create("dependencyManagement", (configuration) -> {
			configuration.setCanBeConsumed(false);
			configuration.setCanBeResolved(false);
		});
		configurations.matching(JavaConventions::needsManagedVersions)
			.all((configuration) -> configuration.extendsFrom(dependencyManagement));
		Dependency platform = project.getDependencies()
			.enforcedPlatform(
					project.getDependencies().project(Collections.singletonMap("path", INTERNAL_DEPENDENCIES)));
		dependencyManagement.getDependencies().add(platform);
	}

	private static boolean needsManagedVersions(Configuration configuration) {
		String name = configuration.getName();
		return name.endsWith("Classpath") || JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name);
	}

	/**
	 * The toolchain settles which compiler runs; {@code release} settles what that
	 * compiler may see, so a call to an API added after the runtime version fails here
	 * rather than on the JVM the artifact claims to support.
	 * @param project the project to configure
	 */
	private void configureJavaCompilation(Project project) {
		int buildVersion = version(project, BUILD_JAVA_VERSION);
		int runtimeVersion = version(project, RUNTIME_JAVA_VERSION);
		project.getExtensions()
			.configure(JavaPluginExtension.class,
					(java) -> java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(buildVersion)));
		project.getTasks().withType(JavaCompile.class).configureEach((compile) -> {
			compile.getOptions().getRelease().set(runtimeVersion);
			Set<String> args = new LinkedHashSet<>(compile.getOptions().getCompilerArgs());
			// -Werror is what makes the rest of the list mean anything: a warning nobody
			// has to act on is scrolled past.
			args.addAll(List.of("-parameters", "-Werror", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:rawtypes",
					"-Xlint:varargs"));
			compile.getOptions().setCompilerArgs(new ArrayList<>(args));
		});
	}

	private int version(Project project, String name) {
		return Integer.parseInt(project.property(name).toString());
	}

	private void configureTests(Project project) {
		project.getTasks().withType(Test.class, (test) -> {
			test.useJUnitPlatform();
			test.setMaxHeapSize("1536M");
		});
		project.getPlugins()
			.withType(JavaPlugin.class, (_) -> project.getDependencies()
				.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher"));
	}

}
