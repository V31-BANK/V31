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
 * All three come from Maven — buf as {@code build.buf:buf}, the generators as
 * {@code com.google.protobuf:protoc} and {@code io.grpc:protoc-gen-grpc-java} — each
 * published one executable per platform under a classifier. Pinned in
 * {@code gradle.properties}, cached like any other dependency, and needing no network
 * once they are there.
 * <p>
 * Copied into {@code build/buf} rather than named where Gradle cached them, because that
 * path carries a hash of the artifact and moves with every version. {@code buf.gen.yaml}
 * has to name a generator by path, and the only path a committed file can name is one the
 * build guarantees.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class DownloadProtoTools extends DefaultTask {

	/**
	 * What the three are called once installed.
	 * <p>
	 * This task chooses the names, so this task is where they are written down: anything
	 * needing one asks for it below rather than spelling the same path out again, and a
	 * rename cannot leave the two disagreeing — which is a failure nothing would catch
	 * until buf ran and said it could not find a plugin.
	 */
	private static final String BUF = "buf";

	private static final String PROTOC = "protoc";

	private static final String GRPC_JAVA_GENERATOR = "protoc-gen-grpc-java";

	/**
	 * The compiler, which reads the {@code .proto} and drives the generators.
	 * @return {@code buf}
	 */
	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getBuf();

	/**
	 * One of the two buf drives.
	 * @return {@code protoc}, which writes the messages
	 */
	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getProtoc();

	/**
	 * The other.
	 * @return {@code protoc-gen-grpc-java}, which writes the service stubs
	 */
	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getGrpcJavaGenerator();

	/**
	 * Under the build directory, so it is thrown away with everything else generated and
	 * never mistaken for something installed.
	 * @return where to put them, which is what {@code buf.gen.yaml} names
	 */
	@OutputDirectory
	public abstract DirectoryProperty getDestination();

	/**
	 * Where buf ended up, said by the task that put it there.
	 * <p>
	 * The name is this task's to choose and nobody else's to know: anything wanting to
	 * run buf asks for it here rather than spelling out the same path again, so the two
	 * cannot come to disagree. Not an input or an output of anything — it is one of the
	 * files {@link #getDestination()} already covers, named.
	 * @return the installed {@code buf}
	 */
	@Internal
	public Provider<RegularFile> getInstalledBuf() {
		return getDestination().file(BUF);
	}

	/**
	 * Where protoc ended up.
	 * @return the installed {@code protoc}
	 */
	@Internal
	public Provider<RegularFile> getInstalledProtoc() {
		return getDestination().file(PROTOC);
	}

	/**
	 * Where the gRPC generator ended up.
	 * @return the installed {@code protoc-gen-grpc-java}
	 */
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

	/**
	 * Files it under the name it is known by, without the version and the platform Maven
	 * serves it under, and makes it runnable — Maven hands over an ordinary file.
	 * @param tool the executable Maven resolved
	 * @param target where it belongs
	 * @throws IOException if it cannot be copied
	 */
	private void install(RegularFileProperty tool, Path target) throws IOException {
		File resolved = tool.get().getAsFile();
		Files.copy(resolved.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
		if (!target.toFile().setExecutable(true)) {
			throw new GradleException("Copied " + target + " but could not make it executable");
		}
	}

}
