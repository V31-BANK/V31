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

package org.v31bank.build.proto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/**
 * Generates one API's sources into the project.
 * <p>
 * One API at a time — {@code buf generate --path <api>} — which is what makes a generated
 * file traceable: everything the run writes came from that one directory's
 * {@code .proto}, so it all belongs to this project and nothing has to be sorted out
 * afterwards.
 * <p>
 * buf writes into a staging directory rather than into the project, so that what it
 * produced can be written down before it is copied. That list is what the next run takes
 * away: a message removed from the API takes its class with it, rather than leaving one
 * behind that still compiles.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class GenerateProtoSources extends DefaultTask {

	/**
	 * What buf is told to generate.
	 * <p>
	 * A language is added by adding a plugin entry, and nothing else has to change. The
	 * paths are relative to the proto directory, which is where buf is run from, and
	 * point at what {@code DownloadProtoTools} put under the build directory — so nothing
	 * has to be installed first, on any machine.
	 */
	private static final String TEMPLATE = """
			version: v2

			managed:
			  enabled: true
			  disable:
			    - file_option: java_package
			  override:
			    - file_option: java_multiple_files
			      value: true

			plugins:
			  - protoc_builtin: java
			    protoc_path: ../build/buf/protoc
			    out: .
			  - local: ../build/buf/protoc-gen-grpc-java
			    out: .
			""";

	/**
	 * Which API this project takes.
	 * @return the directory under the proto root to generate
	 */
	@Input
	public abstract Property<String> getApi();

	/**
	 * Fetched by the build rather than found on the {@code PATH}.
	 * @return the {@code buf} to run
	 */
	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getBuf();

	/**
	 * Where buf is run from, so that an API is named the way it is imported.
	 * @return the proto root, holding {@code buf.yaml} and the APIs
	 */
	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getProtoDirectory();

	/**
	 * A source directory, so only the files this task wrote are ever removed from it. Not
	 * declared as an output either: {@code compileJava} reads it, and declaring it would
	 * make every generated file something one task produces and another silently
	 * consumes.
	 * @return where the generated sources go
	 */
	@Internal
	public abstract DirectoryProperty getDestination();

	@OutputFile
	public abstract RegularFileProperty getManifest();

	@Inject
	protected abstract ExecOperations getExecOperations();

	@Inject
	protected abstract FileSystemOperations getFileSystemOperations();

	@TaskAction
	void generate() throws IOException {
		removeWhatTheLastRunWrote();
		Path staging = getTemporaryDir().toPath().resolve("generated");
		// The whole directory: it holds the last run's output and nothing else.
		getFileSystemOperations().delete((spec) -> spec.delete(staging));
		Files.createDirectories(staging);
		getExecOperations().exec((spec) -> {
			spec.setWorkingDir(getProtoDirectory().get().getAsFile());
			spec.commandLine(getBuf().get().getAsFile().getAbsolutePath(), "generate", "--path", getApi().get(),
					"--output", staging.toString(), "--template", template());
		});
		List<String> written = contentsOf(staging);
		getFileSystemOperations().copy((spec) -> spec.from(staging).into(getDestination()));
		Path manifest = getManifest().get().getAsFile().toPath();
		Files.createDirectories(manifest.getParent());
		Files.write(manifest, written);
		getLogger().lifecycle("Generated {} source(s) from the {} API", written.size(), getApi().get());
	}

	/**
	 * The files the last run put in the destination, and only those. A directory would be
	 * the simpler thing to delete, but the destination is a source directory: taking all
	 * of it would take whatever else lives there too.
	 * @throws IOException if the record cannot be read
	 */
	private void removeWhatTheLastRunWrote() throws IOException {
		Path manifest = getManifest().get().getAsFile().toPath();
		if (!Files.exists(manifest)) {
			return;
		}
		Path destination = getDestination().get().getAsFile().toPath();
		Object[] written = Files.readAllLines(manifest).stream().map(destination::resolve).toArray();
		getFileSystemOperations().delete((spec) -> spec.delete(written));
	}

	/**
	 * Writes {@link #TEMPLATE} out for buf to read.
	 * <p>
	 * buf takes its instructions as a file and nothing else, so the one written down
	 * above has to become one. It goes in the task's own temporary directory, where it is
	 * rewritten on every run and never mistaken for something anyone maintains.
	 * @return the path to hand buf
	 */
	private String template() {
		Path template = getTemporaryDir().toPath().resolve("buf.gen.yaml");
		try {
			Files.writeString(template, TEMPLATE);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + template, ex);
		}
		return template.toString();
	}

	private List<String> contentsOf(Path directory) throws IOException {
		try (Stream<Path> files = Files.walk(directory)) {
			return files.filter(Files::isRegularFile)
				.map((file) -> directory.relativize(file).toString())
				.sorted()
				.toList();
		}
	}

}
