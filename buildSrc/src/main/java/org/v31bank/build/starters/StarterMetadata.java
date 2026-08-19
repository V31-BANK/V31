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

package org.v31bank.build.starters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.v31bank.build.util.PropertiesFiles;

/**
 * Writes a properties file naming a starter and every artifact it resolves to.
 * <p>
 * The dependency graph is only visible by resolving it, so this puts the answer on disk
 * where it can be read without running a build. The output is sorted and carries no
 * timestamp, so an unchanged starter produces a byte-identical file and a diff between
 * two versions shows only what moved.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class StarterMetadata extends DefaultTask {

	public StarterMetadata() {
		Project project = getProject();
		getStarterName().convention(project.provider(project::getName));
		getStarterDescription().convention(project.provider(project::getDescription));
	}

	@Input
	public abstract Property<String> getStarterName();

	@Input
	public abstract Property<String> getStarterDescription();

	@Classpath
	public abstract ConfigurableFileCollection getDependencyFiles();

	/**
	 * The same artifacts as {@link #getDependencyFiles()}, named. Held as plain strings
	 * because the configuration cache cannot serialise a {@link Configuration} into a
	 * task's execution.
	 * @return the name of each artifact the starter resolves to
	 */
	@Input
	public abstract SetProperty<String> getDependencyNames();

	public void setDependencies(Configuration dependencies) {
		getDependencyFiles().setFrom(dependencies);
		getDependencyNames().set(dependencies.getIncoming()
			.getArtifacts()
			.getResolvedArtifacts()
			.map((artifacts) -> artifacts.stream()
				.map(StarterMetadata::nameOf)
				.collect(Collectors.toCollection(TreeSet::new))));
	}

	private static String nameOf(ResolvedArtifactResult artifact) {
		ComponentIdentifier component = artifact.getId().getComponentIdentifier();
		if (component instanceof ModuleComponentIdentifier module) {
			return module.getModule();
		}
		if (component instanceof ProjectComponentIdentifier project) {
			return project.getProjectName();
		}
		return component.getDisplayName();
	}

	@OutputFile
	public abstract RegularFileProperty getDestination();

	@TaskAction
	void generateMetadata() throws IOException {
		Properties properties = new Properties();
		properties.setProperty("name", getStarterName().get());
		properties.setProperty("description", getStarterDescription().get());
		properties.setProperty("dependencies", String.join(",", getDependencyNames().get()));
		Path destination = getDestination().getAsFile().get().toPath();
		Files.createDirectories(destination.getParent());
		Files.write(destination, PropertiesFiles.render(properties));
	}

}
