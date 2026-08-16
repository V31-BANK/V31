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

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

/**
 * Holds one API to the rules {@code buf.yaml} names.
 * <p>
 * The rules are already written down; without this nothing reads them, and a convention
 * nothing enforces is a convention that drifts. What they catch is what a compiler
 * cannot: a package that does not match its directory, a response reused across RPCs so
 * that neither can grow a field, a name that reads differently from every other name in
 * the contract.
 * <p>
 * One API at a time, so a failure says which contract it is about.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class LintProto extends DefaultTask {

	/**
	 * Which API to look at.
	 * @return the path under the proto root to lint
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
	 * Where buf is run from, and where it reads {@code buf.yaml}.
	 * @return the proto root
	 */
	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getProtoDirectory();

	@Inject
	protected abstract ExecOperations getExecOperations();

	@TaskAction
	void lint() {
		getExecOperations().exec((spec) -> {
			spec.setWorkingDir(getProtoDirectory().get().getAsFile());
			spec.commandLine(getBuf().get().getAsFile().getAbsolutePath(), "lint", "--path", getApi().get());
		});
	}

}
