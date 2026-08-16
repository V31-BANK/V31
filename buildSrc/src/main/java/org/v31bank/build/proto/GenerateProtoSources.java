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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Generates one API's sources into the project.
 * <p>
 * buf writes into a staging directory first, so the files it produced can be recorded
 * before they are copied. The next run deletes exactly what that record names, so a
 * message dropped from the {@code .proto} takes its class with it instead of leaving one
 * behind that still compiles.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class GenerateProtoSources extends BufTask {

	/**
	 * The generator paths are single-quoted because they are absolute: quoting survives a
	 * space, and single quotes take no escapes, which a Windows path needs.
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
			    protoc_path: '%s'
			    out: .
			  - local: '%s'
			    out: .
			""";

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getProtoc();

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getGrpcJavaGenerator();

	/**
	 * Not declared as an output: {@code compileJava} reads this directory, and declaring
	 * it would make every generated file something one task produces and another
	 * consumes.
	 * @return the directory the generated sources are copied into
	 */
	@Internal
	public abstract DirectoryProperty getDestination();

	@OutputFile
	public abstract RegularFileProperty getManifest();

	@Inject
	protected abstract FileSystemOperations getFileSystemOperations();

	@TaskAction
	void generate() throws IOException {
		removeWhatTheLastRunWrote();
		Path staging = getTemporaryDir().toPath().resolve("generated");
		// Staging holds only the last run's output, so it is safe to take whole.
		getFileSystemOperations().delete((spec) -> spec.delete(staging));
		Files.createDirectories(staging);
		buf("generate", "--path", getApi().get(), "--output", staging.toString(), "--template", template());
		List<String> written = contentsOf(staging);
		getFileSystemOperations().copy((spec) -> spec.from(staging).into(getDestination()));
		Path manifest = getManifest().get().getAsFile().toPath();
		Files.createDirectories(manifest.getParent());
		Files.write(manifest, written);
		getLogger().lifecycle("Generated {} source(s) from the {} API", written.size(), getApi().get());
	}

	/**
	 * The destination is a source directory, so deleting all of it would take whatever
	 * else lives there; only the files the manifest names go.
	 * @throws IOException if the manifest cannot be read
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
	 * buf takes its instructions as a file and nothing else, so {@link #TEMPLATE} is
	 * written out on every run, into the temporary directory where nobody will maintain
	 * it by hand.
	 * @return the path to hand buf
	 */
	private String template() {
		Path template = getTemporaryDir().toPath().resolve("buf.gen.yaml");
		try {
			Files.writeString(template, TEMPLATE.formatted(getProtoc().get().getAsFile().getAbsolutePath(),
					getGrpcJavaGenerator().get().getAsFile().getAbsolutePath()));
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
