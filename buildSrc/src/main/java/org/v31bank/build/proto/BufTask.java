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
import org.gradle.process.ExecOperations;

/**
 * One run of buf against one API.
 * <p>
 * Every buf command takes the same three things and takes them the same way: which
 * executable, which API, and the directory to run from — buf reads {@code buf.yaml} from
 * where it is invoked and names an API by its path relative to there, so the working
 * directory is not a detail but part of how an API is addressed. A subclass says only
 * which command it is and what that command's own arguments are.
 * <p>
 * The commands differ in more than their arguments, which is why they stay separate
 * tasks rather than becoming modes of one. Generating writes into the project and has an
 * output Gradle can check; checking writes nothing and belongs to {@code check}. Folding
 * the two together would put a source directory under a verification task, which is the
 * one thing a verification task must not touch.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class BufTask extends DefaultTask {

	/**
	 * Which API this task is about.
	 * @return the path under the proto root to work on
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
	 * Where buf is run from, and where it reads {@code buf.yaml}, so that an API is named
	 * the way it is imported.
	 * @return the proto root, holding {@code buf.yaml} and every API
	 */
	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getProtoDirectory();

	@Inject
	protected abstract ExecOperations getExecOperations();

	/**
	 * Runs buf, from the one place it can be run from.
	 * @param arguments the command and its own arguments, without the executable
	 */
	protected void buf(String... arguments) {
		getExecOperations().exec((spec) -> {
			spec.setWorkingDir(getProtoDirectory().get().getAsFile());
			spec.executable(getBuf().get().getAsFile().getAbsolutePath());
			spec.args((Object[]) arguments);
		});
	}

}
