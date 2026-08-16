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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.proto.DownloadProtoTools;
import org.v31bank.build.proto.GenerateProtoSources;
import org.v31bank.build.proto.LintProto;

/**
 * What a project built from a {@code .proto} is.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.protobuf")
 * }
 * </pre>
 *
 * The APIs live in one place — {@code proto} at the root, managed by Buf — and a project
 * takes the one that is named after it. The match is by name and nothing else:
 * {@code proto/compliance} is the API for whichever project has {@code compliance} in its
 * name, so a project is served by being called what it is and there is no list of who
 * gets what.
 * <p>
 * Generation is part of building rather than something to remember to run. The sources
 * land in {@code src/main/java} and are committed, so a clone compiles without generating
 * first; the build regenerates them when the {@code .proto} changes, so what is compiled
 * is never something the contract stopped saying.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ProtobufPlugin implements Plugin<Project> {

	/** The APIs, and Buf's configuration for them, at the root of the build. */
	private static final String PROTO = "proto";

	/**
	 * The one segment every API's package starts with, and so the directory they all sit
	 * in: buf's {@code PACKAGE_DIRECTORY_MATCH} wants a file's path to spell out its
	 * package, and every package here begins {@code v31}.
	 */
	private static final String PACKAGE_ROOT = "v31";

	private static final String GENERATE_TASK_NAME = "generateProtoSources";

	private static final String LINT_TASK_NAME = "lintProto";

	private static final String TOOLS_TASK_NAME = "downloadProtoTools";

	private static final String BUF_VERSION = "bufVersion";

	private static final String PROTOC_VERSION = "protocVersion";

	private static final String GRPC_JAVA_GENERATOR_VERSION = "grpcJavaGeneratorVersion";

	/** Where buf and the generators it runs are kept, and what buf.gen.yaml names. */
	private static final String TOOLS = "buf";

	/**
	 * What generated protobuf code needs in order to compile.
	 * <p>
	 * Exported rather than kept to the implementation: the generated types are the API,
	 * so anything depending on this project names them and needs them on its own compile
	 * classpath. Versions come from the platform, as everywhere else.
	 */
	private static final List<String> RUNTIME = List.of("com.google.protobuf:protobuf-java", "io.grpc:grpc-protobuf",
			"io.grpc:grpc-stub");

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaLibraryPlugin.class);
		project.getPluginManager().apply(DeployedPlugin.class);
		RUNTIME.forEach((coordinate) -> project.getDependencies().add(JavaPlugin.API_CONFIGURATION_NAME, coordinate));
		apiFor(project).ifPresent((api) -> generate(project, api));
	}

	/**
	 * The API this project is named after, if there is one.
	 * <p>
	 * Empty covers both ways there can be none: a build with no {@code proto} directory
	 * at all, and a project whose name matches nothing in it. Neither is a problem — the
	 * plugin simply has nothing to generate — so both are the same answer rather than two
	 * things to check separately.
	 * @param project the project asking
	 * @return the directory under {@code proto} whose name this project's contains
	 * @throws GradleException if the name contains more than one, since there is then no
	 * telling which was meant
	 */
	private Optional<String> apiFor(Project project) {
		List<String> matches = apisIn(project).filter((api) -> project.getName().contains(api)).toList();
		if (matches.size() > 1) {
			throw new GradleException(project.getPath() + " is named after more than one API " + matches
					+ ", so which one it wants cannot be read from its name.");
		}
		return matches.stream().findFirst();
	}

	/**
	 * Every API there is.
	 * <p>
	 * {@code listFiles} answers {@code null} for a path that is not a directory as well
	 * as for one that is not there, so a build with no {@code proto} directory needs no
	 * separate question asked of it.
	 * @param project the project asking
	 * @return the name of each directory under {@code proto}, and none if there is no
	 * such directory
	 */
	private Stream<String> apisIn(Project project) {
		File[] directories = apiDirectory(project).listFiles();
		return (directories != null) ? Stream.of(directories)
			.filter((file) -> file.isDirectory() && !file.getName().startsWith("."))
			.map(File::getName) : Stream.empty();
	}

	private File protoDirectory(Project project) {
		return project.getRootProject().file(PROTO);
	}

	private File apiDirectory(Project project) {
		return new File(protoDirectory(project), PACKAGE_ROOT);
	}

	private void generate(Project project, String api) {
		File proto = protoDirectory(project);
		File directory = new File(apiDirectory(project), api);
		TaskProvider<DownloadProtoTools> tools = tools(project.getRootProject());
		TaskProvider<GenerateProtoSources> generate = project.getTasks()
			.register(GENERATE_TASK_NAME, GenerateProtoSources.class, (task) -> {
				task.setDescription("Generates this project's sources from the " + api + " proto.");
				// The path buf is given, which is the package spelled out.
				task.getApi().set(PACKAGE_ROOT + "/" + api);
				task.getBuf().set(tools.flatMap((installed) -> installed.getDestination().file("buf")));
				task.getProtoDirectory().set(proto);
				task.getDestination().set(mainSourceDirectory(project));
				task.getManifest().set(project.getLayout().getBuildDirectory().file("proto/generated-sources.txt"));
				task.onlyIf("the " + api + " API has a .proto to generate", (_) -> holdsProtos(directory));
			});
		project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure((compile) -> compile.dependsOn(generate));
		TaskProvider<LintProto> lint = project.getTasks().register(LINT_TASK_NAME, LintProto.class, (task) -> {
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Checks the " + api + " API against the rules in buf.yaml.");
			task.getApi().set(PACKAGE_ROOT + "/" + api);
			task.getBuf().set(tools.flatMap((installed) -> installed.getDestination().file("buf")));
			task.getProtoDirectory().set(proto);
			task.onlyIf("the " + api + " API has a .proto to check", (_) -> holdsProtos(directory));
		});
		project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure((check) -> check.dependsOn(lint));
	}

	private static boolean holdsProtos(File api) {
		try (Stream<Path> files = Files.walk(api.toPath())) {
			return files.anyMatch((file) -> file.toString().endsWith(".proto"));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to look inside " + api, ex);
		}
	}

	/**
	 * Where this project keeps its Source.
	 * @param project the project to write into
	 * @return its main source directory
	 */
	private File mainSourceDirectory(Project project) {
		return project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
			.getJava()
			.getSrcDirs()
			.iterator()
			.next();
	}

	/**
	 * Resolves one of the executables from Maven.
	 * <p>
	 * buf could fetch a generator from its own registry, and did until generation became
	 * part of the build — at which point every build called the registry and the registry
	 * began answering {@code resource_exhausted: too many requests}. buf itself is on
	 * Maven too, so all three are taken the same way: pinned, cached, and needing no
	 * network once they are there.
	 * @param project the project resolving it
	 * @param coordinate the generator, without a version or a classifier
	 * @param version the property naming which release to take
	 * @return the executable
	 */
	private Provider<RegularFile> fromMaven(Project project, String coordinate, String version) {
		String full = coordinate + ":" + project.property(version) + ":" + platform() + "@exe";
		Configuration resolved = project.getConfigurations()
			.detachedConfiguration(project.getDependencies().create(full));
		return project.getLayout().file(project.provider(resolved::getSingleFile));
	}

	/**
	 * The classifier protobuf and gRPC publish their executables under.
	 * @return the classifier for the machine building
	 */
	private static String platform() {
		String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
		boolean arm = arch.contains("aarch64") || arch.contains("arm64");
		if (os.contains("mac") || os.contains("darwin")) {
			return arm ? "osx-aarch_64" : "osx-x86_64";
		}
		if (os.contains("windows")) {
			return "windows-x86_64";
		}
		return arm ? "linux-aarch_64" : "linux-x86_64";
	}

	/**
	 * The one set of tools the whole build shares.
	 * <p>
	 * Registered on the root project rather than on each project that needs it, and so
	 * that {@code buf.gen.yaml} can name one path they all use. Asked for by name first,
	 * because a second {@code register} of the same name throws rather than handing back
	 * what is already there.
	 * @param root the root project
	 * @return the task that installs them
	 */
	private TaskProvider<DownloadProtoTools> tools(Project root) {
		TaskContainer tasks = root.getTasks();
		if (tasks.getNames().contains(TOOLS_TASK_NAME)) {
			return tasks.named(TOOLS_TASK_NAME, DownloadProtoTools.class);
		}
		return tasks.register(TOOLS_TASK_NAME, DownloadProtoTools.class, (task) -> {
			task.setDescription("Fetches buf and the generators it runs.");
			task.getBuf().set(fromMaven(root, "build.buf:buf", BUF_VERSION));
			task.getProtoc().set(fromMaven(root, "com.google.protobuf:protoc", PROTOC_VERSION));
			task.getGrpcJavaGenerator()
				.set(fromMaven(root, "io.grpc:protoc-gen-grpc-java", GRPC_JAVA_GENERATOR_VERSION));
			task.getDestination().set(root.getLayout().getBuildDirectory().dir(TOOLS));
		});
	}

}
