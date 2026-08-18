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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.RegularFile;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;

import org.v31bank.build.proto.BufTask;
import org.v31bank.build.proto.DownloadProtoTools;
import org.v31bank.build.proto.GenerateProtoSources;
import org.v31bank.build.proto.LintProto;
import org.v31bank.build.util.SourceSets;
import org.v31bank.build.util.SourceSets.Directories;

/**
 * Generates a project's Java sources from the {@code .proto} it is named after.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.protobuf")
 * }
 * </pre>
 *
 * Projects are matched to an API under the root {@code proto} directory by name alone, so
 * there is no list of who gets what. Generated sources are committed, so a clone compiles
 * without running buf, and the build regenerates them when the {@code .proto} changes.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ProtobufPlugin implements Plugin<Project> {

	private static final String PROTO = "proto";

	/**
	 * buf's {@code PACKAGE_DIRECTORY_MATCH} wants a file's path to spell out its package,
	 * and every package here begins {@code v31}. Every path this plugin builds is derived
	 * from this one line.
	 */
	private static final String APIS = "v31";

	private static final String GENERATE_TASK_NAME = "generateProtoSources";

	private static final String LINT_TASK_NAME = "lintProto";

	private static final String TOOLS_TASK_NAME = "downloadProtoTools";

	private static final String BUF_VERSION = "bufVersion";

	/**
	 * One version per generator-and-runtime pair, because they are one decision: the
	 * platform pins the library from the same property this fetches the generator with,
	 * so the two cannot drift.
	 */
	private static final String PROTOBUF_VERSION = "protobufVersion";

	private static final String GRPC_VERSION = "grpcVersion";

	/**
	 * Where buf and the generators it runs are installed, and the path
	 * {@code buf.gen.yaml} names.
	 */
	private static final String TOOLS = "buf";

	/**
	 * Added to {@code api} rather than {@code implementation}: the generated types are
	 * the API, so anything depending on this project names them.
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

	private Optional<Api> apiFor(Project project) {
		List<Api> matches = apisIn(project.getRootProject().file(PROTO))
			.filter((api) -> project.getName().contains(api.name()))
			.toList();
		if (matches.size() > 1) {
			throw new GradleException(
					project.getPath() + " is named after more than one API " + matches.stream().map(Api::name).toList()
							+ ", so which one it wants cannot be read from its name.");
		}
		return matches.stream().findFirst();
	}

	private Stream<Api> apisIn(File root) {
		// null covers both a missing path and a non-directory one.
		File[] directories = new File(root, APIS).listFiles();
		return (directories != null) ? Stream.of(directories)
			.filter((file) -> file.isDirectory() && !file.getName().startsWith("."))
			.map((file) -> new Api(root, file.getName())) : Stream.empty();
	}

	private void declareAsGeneratedSources(Project project) {
		Directories java = SourceSets.of(project).main().java();
		java.unwrap().srcDir(SourceSets.of(project).generatedSources());
		project.getPluginManager().apply(IdeaPlugin.class);
		IdeaModule module = project.getExtensions().getByType(IdeaModel.class).getModule();
		module.getGeneratedSourceDirs().add(
				java.directory(file -> file.toPath().endsWith(SourceSets.GENERATED_SOURCES)));
	}

	private void generate(Project project, Api api) {
		declareAsGeneratedSources(project);
		TaskProvider<DownloadProtoTools> tools = tools(project.getRootProject());
		Provider<RegularFile> buf = tools.flatMap(DownloadProtoTools::getInstalledBuf);
		TaskProvider<GenerateProtoSources> generate = bufTask(project, GENERATE_TASK_NAME, GenerateProtoSources.class,
				api, buf, (task) -> {
					task.setDescription("Generates this project's sources from the " + api.name() + " proto.");
					task.getProtoc().set(tools.flatMap(DownloadProtoTools::getInstalledProtoc));
					task.getGrpcJavaGenerator().set(tools.flatMap(DownloadProtoTools::getInstalledGrpcJavaGenerator));
					task.getDestination().set(SourceSets.of(project).generatedSources());
					task.getManifest().set(project.getLayout().getBuildDirectory().file("proto/generated-sources.txt"));
				});
		project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME).configure((compile) -> compile.dependsOn(generate));
		TaskProvider<LintProto> lint = bufTask(project, LINT_TASK_NAME, LintProto.class, api, buf, (task) -> {
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Checks the " + api.name() + " API against the rules in buf.yaml.");
		});
		project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure((check) -> check.dependsOn(lint));
	}

	private <T extends BufTask> TaskProvider<T> bufTask(Project project, String name, Class<T> type, Api api,
			Provider<RegularFile> buf, Action<? super T> configure) {
		return project.getTasks().register(name, type, (task) -> {
			task.getApi().set(api.path());
			task.getBuf().set(buf);
			task.getProtoDirectory().set(api.root());
			task.onlyIf("the " + api.name() + " API has a .proto", (_) -> api.holdsProtos());
			configure.execute(task);
		});
	}

	private Provider<RegularFile> fromMaven(Project project, String coordinate, String version) {
		// buf's own registry answered resource_exhausted once every build called it.
		String full = coordinate + ":" + project.property(version) + ":" + platform() + "@exe";
		Configuration resolved = project.getConfigurations()
			.detachedConfiguration(project.getDependencies().create(full));
		return project.getLayout().file(project.provider(resolved::getSingleFile));
	}

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
	 * Registered on the root project so the whole build shares one install and
	 * {@code buf.gen.yaml} can name one path. Looked up by name first, because a second
	 * {@code register} of the same name throws.
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
			task.getProtoc().set(fromMaven(root, "com.google.protobuf:protoc", PROTOBUF_VERSION));
			task.getGrpcJavaGenerator()
				.set(fromMaven(root, "io.grpc:protoc-gen-grpc-java", GRPC_VERSION));
			task.getDestination().set(root.getLayout().getBuildDirectory().dir(TOOLS));
		});
	}

	/**
	 * One API: the directory holding its {@code .proto} and the path buf addresses it by.
	 * <p>
	 * Both are derived from {@link #APIS} here rather than spelled out at each use, so
	 * they cannot drift apart when the layout moves.
	 *
	 * @param root where buf runs, holding {@code buf.yaml} and every API
	 * @param name what the API is called, and so what a project's name is matched against
	 */
	private record Api(File root, String name) {

		/**
		 * Written with {@code /} on every platform: buf reads this path, and so does an
		 * importing {@code .proto}.
		 * @return this API's path beneath the proto root
		 */
		String path() {
			return APIS + "/" + this.name;
		}

		File directory() {
			return new File(this.root, path());
		}

		/**
		 * buf fails on an empty directory, and a directory can be added before the
		 * {@code .proto} that goes in it is written.
		 * @return whether this API holds a {@code .proto}
		 */
		boolean holdsProtos() {
			try (Stream<Path> files = Files.walk(directory().toPath())) {
				return files.anyMatch((file) -> file.toString().endsWith(".proto"));
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to look inside " + directory(), ex);
			}
		}

	}

}
