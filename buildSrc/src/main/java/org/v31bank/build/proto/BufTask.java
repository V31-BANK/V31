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
 * buf reads {@code buf.yaml} from the directory it is invoked in and names an API by its
 * path relative to there, so the working directory is part of how an API is addressed. A
 * subclass supplies only the command and that command's own arguments.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class BufTask extends DefaultTask {

	@Input
	public abstract Property<String> getApi();

	@InputFile
	@PathSensitive(PathSensitivity.NONE)
	public abstract RegularFileProperty getBuf();

	@InputDirectory
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract DirectoryProperty getProtoDirectory();

	@Inject
	protected abstract ExecOperations getExecOperations();

	protected void buf(String... arguments) {
		getExecOperations().exec((spec) -> {
			spec.setWorkingDir(getProtoDirectory().get().getAsFile());
			spec.executable(getBuf().get().getAsFile().getAbsolutePath());
			spec.args((Object[]) arguments);
		});
	}

}
