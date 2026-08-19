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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Fails when a deprecated property points at a replacement that exists in no module.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckAggregatedSpringConfigurationMetadata extends DefaultTask {

	public CheckAggregatedSpringConfigurationMetadata() {
		setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
	}

	@OutputFile
	public abstract RegularFileProperty getReportLocation();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract ConfigurableFileCollection getConfigurationPropertyMetadata();

	@TaskAction
	void check() {
		List<ConfigurationProperty> danglingReplacements = danglingReplacements();
		File reportFile = getReportLocation().get().getAsFile();
		write(reportFile, report(danglingReplacements));
		if (!danglingReplacements.isEmpty()) {
			throw new VerificationException(
					"Problems found in aggregated Spring configuration metadata. See %s for details."
						.formatted(reportFile));
		}
	}

	/**
	 * The replacement usually lives in another module, so no module can ask this alone.
	 * @return the properties whose replacement is missing
	 */
	private List<ConfigurationProperty> danglingReplacements() {
		ConfigurationProperties properties = ConfigurationProperties.of(getConfigurationPropertyMetadata());
		Set<String> known = properties.stream().map(ConfigurationProperty::name).collect(Collectors.toSet());
		return properties.stream()
			.filter(ConfigurationProperty::deprecated)
			.filter((property) -> replacementOf(property) != null && !known.contains(replacementOf(property)))
			.toList();
	}

	private static String replacementOf(ConfigurationProperty property) {
		return (property.deprecation() != null) ? property.deprecation().replacement() : null;
	}

	private String report(List<ConfigurationProperty> danglingReplacements) {
		List<String> lines = new ArrayList<>();
		if (danglingReplacements.isEmpty()) {
			lines.add("No problems found.");
		}
		else {
			lines.add("The following properties have a replacement that does not exist:");
			lines.add("");
			danglingReplacements.forEach((property) -> lines
				.add("\t%s (replacement %s)".formatted(property.name(), replacementOf(property))));
		}
		lines.add("");
		return String.join(System.lineSeparator(), lines);
	}

	private void write(File file, String content) {
		try {
			Files.createDirectories(file.getParentFile().toPath());
			Files.writeString(file.toPath(), content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + file, ex);
		}
	}

}
