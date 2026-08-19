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
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

import org.v31bank.build.properties.ConfigurationPropertiesAnalyzer.Report;

/**
 * Fails when a generated property has no description, which is the one thing the
 * processor cannot supply on its own.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckSpringConfigurationMetadata extends DefaultTask {

	private final File projectDirectory;

	public CheckSpringConfigurationMetadata() {
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		this.projectDirectory = getProject().getProjectDir();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getMetadataLocation();

	@Input
	public abstract ListProperty<String> getExclusions();

	@TaskAction
	void check() {
		File metadata = getMetadataLocation().get().getAsFile();
		Report report = new Report(this.projectDirectory);
		new ConfigurationPropertiesAnalyzer(List.of(metadata)).analyzePropertyDescription(report,
				getExclusions().get());
		File reportFile = getReportLocation().get().getAsFile();
		report.write(reportFile);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in Spring configuration metadata. See %s for details.".formatted(reportFile));
		}
	}

}
