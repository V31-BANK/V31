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

package org.v31bank.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * A check on a module's auto-configuration imports file and the classes it names.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class AutoConfigurationImportsTask extends DefaultTask {

	/**
	 * The name of the report every check writes, whether or not it found anything.
	 */
	public static final String FAILURE_REPORT = "failure-report.txt";

	protected AutoConfigurationImportsTask() {
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		getOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir(getName()));
	}

	/**
	 * Not an input itself: one file out of it is, and getSource() is what declares that.
	 * @return the resources to look in
	 */
	@Internal
	public abstract ConfigurableFileCollection getResources();

	/**
	 * Absent means the module registers nothing, so the check is skipped rather than
	 * failed.
	 * @return the imports file
	 */
	@InputFiles
	@SkipWhenEmpty
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return getResources().getAsFileTree().matching((filter) -> filter.include(AutoConfigurationImports.PATH));
	}

	@Classpath
	public abstract ConfigurableFileCollection getClasspath();

	/**
	 * Also what lets a check go up to date: a task declaring no output is always re-run.
	 * @return the directory to report into
	 */
	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	protected List<String> loadImports() {
		return AutoConfigurationImports.read(getSource().getSingleFile());
	}

	protected File writeReport(String report) {
		File reportFile = getOutputDirectory().file(FAILURE_REPORT).get().getAsFile();
		write(reportFile, report);
		return reportFile;
	}

	protected void write(File file, String content) {
		try {
			Files.createDirectories(file.getParentFile().toPath());
			Files.writeString(file.toPath(), content);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + file, ex);
		}
	}

}
