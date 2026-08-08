package org.v31bank.build;

import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.Checkstyle;
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

	private static final int JAVA_VERSION = 21;

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			configureDependencyManagement(project);
			configureJavaCompilation(project);
			configureTests(project);
			configureFormatting(project);
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
		project.getPlugins()
			.withId("com.google.protobuf",
					(protobuf) -> project
						.afterEvaluate((evaluated) -> configurations.matching(JavaConventions::needsManagedVersions)
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

	private void configureJavaCompilation(Project project) {
		project.getExtensions()
			.configure(JavaPluginExtension.class,
					(java) -> java.getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(JAVA_VERSION)));
		project.getTasks().withType(JavaCompile.class).configureEach((compile) -> {
			compile.getOptions().getRelease().set(JAVA_VERSION);
		});
	}

	private void configureTests(Project project) {
		project.getTasks().withType(Test.class, (test) -> {
			test.useJUnitPlatform();
			test.setMaxHeapSize("1536M");
		});
		project.getPlugins()
				.withType(JavaPlugin.class, (javaPlugin) -> project.getDependencies()
						.add(JavaPlugin.TEST_RUNTIME_ONLY_CONFIGURATION_NAME, "org.junit.platform:junit-platform-launcher"));
	}

	/**
	 * Not applied under {@code apis}, where the sources are produced by protoc:
	 * formatting generated code achieves nothing and is undone the next time the contract
	 * is compiled.
	 * @param project the project to configure
	 */
	private void configureFormatting(Project project) {
		Project parent = project.getParent();
		if (parent == null || !"apis".equals(parent.getName())) {
			project.getPlugins().apply("io.spring.javaformat");
		}
	}

}
