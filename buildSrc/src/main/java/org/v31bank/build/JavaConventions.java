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
 * What used to be a {@code subprojects} block in the root build file. Expressed as a
 * plugin instead, so that a subproject opts in by applying it rather than by being a
 * child of the right project, and so the rules can be read, compiled and tested as
 * ordinary code.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class JavaConventions {

	/**
	 * The platform every project resolves its third-party versions from.
	 */
	private static final String INTERNAL_DEPENDENCIES = ":platform:V31-internal-dependencies";

	/**
	 * The property naming the JDK that compiles and that runs the tests. Pinning it
	 * through a toolchain is what makes the same source produce the same result on every
	 * machine, rather than whatever JDK happens to be running the build.
	 */
	private static final String BUILD_JAVA_VERSION = "buildJavaVersion";

	/**
	 * The property naming what the result is allowed to use and to run on. Settled with
	 * {@code release}, which fixes the syntax, the bytecode version and the visible API
	 * together — so a call to something added later fails here rather than on the JVM the
	 * artifact claimed to support.
	 */
	private static final String RUNTIME_JAVA_VERSION = "runtimeJavaVersion";

	/**
	 * The property naming the checkstyle release to run, so that it sits beside the other
	 * tool versions in {@code gradle.properties} rather than inside the build logic.
	 */
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
	 * Holds the code to one shape and one set of habits.
	 * <p>
	 * Two tools, because they answer different questions. The formatter settles what the
	 * code looks like — indentation, wrapping, import order — and settles it by rewriting
	 * the file, so it is never something to discuss. Checkstyle settles what the code is
	 * allowed to do: which packages may be imported, whether a deprecation says what
	 * replaced it, whether a public type explains itself. Neither can do the other's job,
	 * and both turn a recurring review comment into a build failure.
	 * <p>
	 * The checkstyle rules are the ones shipped with the formatter, so the two cannot
	 * disagree, and the version comes from the formatter's own jar rather than being
	 * named twice.
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
			return path.contains("/generated/sources/") || path.contains("/generated-source/")
					|| path.contains("/apis/");
		});
	}

	/**
	 * Gives every project the platform's versions without any project having to ask.
	 * <p>
	 * The platform goes into a configuration of its own that the resolvable ones extend,
	 * rather than into {@code implementation}. Three things follow from that, and all
	 * three are the reason it is done this way:
	 * <ul>
	 * <li>It reaches every classpath — compile, runtime, test, and any source set added
	 * later — instead of the two configurations someone remembered to name.</li>
	 * <li>The configuration is neither consumable nor resolvable, so the platform never
	 * appears in this project's published metadata. Declared as {@code implementation} it
	 * does, and a consumer then has to resolve a platform that only exists inside this
	 * build.</li>
	 * <li>It is an <em>enforced</em> platform: the versions are mandatory rather than
	 * suggestions, so a transitive dependency cannot quietly pull the build onto a
	 * different one.</li>
	 * </ul>
	 * This mirrors what Spring Boot's own build does with
	 * {@code spring-boot-internal-dependencies}.
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

		// The protobuf plugin sets the parents of its own resolvable configurations
		// after creating them, discarding whatever was added in between. Adding it
		// again once the project is configured is what makes it stick.
		project.getPluginManager()
			.withPlugin("com.google.protobuf",
					(_) -> project.afterEvaluate((_) -> configurations.matching(JavaConventions::needsManagedVersions)
						.all((configuration) -> configuration.extendsFrom(dependencyManagement))));

		Dependency platform = project.getDependencies()
			.enforcedPlatform(
					project.getDependencies().project(Collections.singletonMap("path", INTERNAL_DEPENDENCIES)));
		dependencyManagement.getDependencies().add(platform);
	}

	/**
	 * Whether a configuration is one that resolves dependencies, and so needs the
	 * platform's versions.
	 * <p>
	 * The classpaths and the annotation processor path are the obvious ones. The protobuf
	 * plugin adds its own — {@code compileProtoPath}, {@code testCompileProtoPath} —
	 * which resolve {@code protobuf-java} and the gRPC artifacts; they end in
	 * {@code Path} rather than {@code Classpath}, so matching only the latter leaves the
	 * API projects unable to resolve anything.
	 * @param configuration the configuration to consider
	 * @return whether it should take its versions from the platform
	 */
	private static boolean needsManagedVersions(Configuration configuration) {
		String name = configuration.getName();
		return name.endsWith("Classpath") || name.endsWith("ProtoPath")
				|| JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name);
	}

	/**
	 * Both, because they answer different questions.
	 * <p>
	 * The toolchain settles which compiler runs, so that the same source produces the
	 * same bytecode here and on a build agent. The release settles what that compiler is
	 * allowed to see: without it the whole JDK is on the compile classpath, and a call to
	 * something added after this version compiles cleanly and fails at runtime.
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
			// -Werror is what makes the rest of this list mean anything. A warning
			// nobody has to act on is read once and then scrolled past, and the build
			// that prints two hundred of them is the build where the one that mattered
			// went unread.
			args.addAll(List.of("-parameters", "-Werror", "-Xlint:unchecked", "-Xlint:deprecation", "-Xlint:rawtypes",
					"-Xlint:varargs"));
			compile.getOptions().setCompilerArgs(new ArrayList<>(args));
		});
	}

	/**
	 * Reads a version.
	 * <p>
	 * No check that it is there: {@code property} raises one naming the property, and
	 * {@code buildSrc} settles the same two properties before this build logic is even
	 * compiled — anything missing has already stopped the build by now.
	 * @param project the project to read from
	 * @param name the property naming the version
	 * @return the version it names
	 */
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
