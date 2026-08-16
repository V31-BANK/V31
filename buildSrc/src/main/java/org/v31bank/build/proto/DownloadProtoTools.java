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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Puts everything generation needs where {@code buf.gen.yaml} says it is.
 * <p>
 * The tools are resolved from Maven and copied into {@code build/buf} rather than named
 * where Gradle cached them: a cache path carries a hash of the artifact and moves with
 * every version, and a committed {@code buf.gen.yaml} can only name a fixed path.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class DownloadProtoTools extends DefaultTask {

	private static final String BUF = "buf";

	private static final String PROTOC = "protoc";

	private static final String GRPC_JAVA_GENERATOR = "protoc-gen-grpc-java";

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getBuf();

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getProtoc();

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getGrpcJavaGenerator();

	@OutputDirectory
	public abstract DirectoryProperty getDestination();

	/**
	 * Lets a caller name the installed {@code buf} without repeating the path this task
	 * chose. Not an input or an output of anything: the file is already covered by
	 * {@link #getDestination()}.
	 * @return the installed {@code buf}
	 */
	@Internal
	public Provider<RegularFile> getInstalledBuf() {
		return getDestination().file(BUF);
	}

	@Internal
	public Provider<RegularFile> getInstalledProtoc() {
		return getDestination().file(PROTOC);
	}

	@Internal
	public Provider<RegularFile> getInstalledGrpcJavaGenerator() {
		return getDestination().file(GRPC_JAVA_GENERATOR);
	}

	@TaskAction
	void install() throws IOException {
		Path destination = getDestination().get().getAsFile().toPath();
		Files.createDirectories(destination);
		install(getBuf(), destination.resolve(BUF));
		install(getProtoc(), destination.resolve(PROTOC));
		install(getGrpcJavaGenerator(), destination.resolve(GRPC_JAVA_GENERATOR));
	}

	private void install(RegularFileProperty tool, Path target) throws IOException {
		File resolved = tool.get().getAsFile();
		Files.copy(resolved.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
		// Maven serves the tool as an ordinary file, never as an executable.
		if (!target.toFile().setExecutable(true)) {
			throw new GradleException("Copied " + target + " but could not make it executable");
		}
	}

}
