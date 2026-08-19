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

package org.v31bank.build.properties;

import java.io.File;

import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.properties.ConfigurationPropertiesAnalyzer.Report;

/**
 * Fails when a module's hand-written metadata is out of order, names something twice, or
 * deprecates a property without saying since when.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckAdditionalSpringConfigurationMetadata extends SourceTask {

	private final File projectDirectory;

	public CheckAdditionalSpringConfigurationMetadata() {
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		this.projectDirectory = getProject().getProjectDir();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@Override
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return super.getSource();
	}

	/**
	 * Descriptions are not required here: this file exists to add what the processor
	 * could not work out, and demanding one per line would make the escape hatch harder
	 * than the escape.
	 */
	@TaskAction
	void check() {
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(getSource().getFiles());
		Report report = new Report(this.projectDirectory);
		analyzer.analyzeOrder(report);
		analyzer.analyzeDuplicates(report);
		analyzer.analyzeDeprecationSince(report);
		File reportFile = getReportLocation().get().getAsFile();
		report.write(reportFile);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in additional Spring configuration metadata. See %s for details."
						.formatted(reportFile));
		}
	}

}
