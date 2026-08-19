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
import org.gradle.api.provider.Property;
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
 * Fails when metadata written entirely by hand breaks any of the four rules, descriptions
 * included: there is no processor behind it to fall back on.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckManualSpringConfigurationMetadata extends DefaultTask {

	private final File projectDirectory;

	public CheckManualSpringConfigurationMetadata() {
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
		this.projectDirectory = getProject().getProjectDir();
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract Property<File> getMetadataLocation();

	@Input
	public abstract ListProperty<String> getExclusions();

	@TaskAction
	void check() {
		ConfigurationPropertiesAnalyzer analyzer = new ConfigurationPropertiesAnalyzer(
				List.of(getMetadataLocation().get()));
		Report report = new Report(this.projectDirectory);
		analyzer.analyzeOrder(report);
		analyzer.analyzeDuplicates(report);
		analyzer.analyzePropertyDescription(report, getExclusions().get());
		analyzer.analyzeDeprecationSince(report);
		File reportFile = getReportLocation().get().getAsFile();
		report.write(reportFile);
		if (report.hasProblems()) {
			throw new VerificationException(
					"Problems found in manual Spring configuration metadata. See %s for details."
						.formatted(reportFile));
		}
	}

}
