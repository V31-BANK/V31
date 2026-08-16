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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
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

/**
 * Writes down what a starter is and what it brings in.
 * <p>
 * The dependency graph is the whole of a starter, and it is only visible by resolving it
 * — which means anything that wants to describe a starter, compare two of them, or notice
 * that one grew a dependency has to run a build to find out. This puts the answer in a
 * file, and {@link StarterPlugin} publishes that file through a configuration of its own,
 * so a project can consume it by name without knowing where it was written.
 * <p>
 * The output is sorted and carries no timestamp, so two builds of an unchanged starter
 * produce byte-identical files and a diff between two versions shows only what moved.
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

	/**
	 * What the starter resolves to, as files.
	 * <p>
	 * The input, so that the metadata is rewritten whenever the graph moves and not
	 * otherwise.
	 * @return the resolved runtime classpath
	 */
	@Classpath
	public abstract ConfigurableFileCollection getDependencyFiles();

	/**
	 * The same thing as names, which is what actually gets written down.
	 * <p>
	 * Worked out from the resolution rather than from the file names, and kept as plain
	 * strings because a {@link Configuration} cannot be carried into a task's execution —
	 * the configuration cache cannot serialise one.
	 * @return the name of each artifact the starter resolves to
	 */
	@Input
	public abstract SetProperty<String> getDependencyNames();

	/**
	 * Takes a configuration apart into the two things this task needs of it.
	 * <p>
	 * Both lazily: nothing is resolved until the metadata is actually written.
	 * @param dependencies what the starter resolves to
	 */
	public void setDependencies(Configuration dependencies) {
		getDependencyFiles().setFrom(dependencies);
		getDependencyNames().set(dependencies.getIncoming()
			.getArtifacts()
			.getResolvedArtifacts()
			.map((artifacts) -> artifacts.stream()
				.map(StarterMetadata::nameOf)
				.collect(Collectors.toCollection(TreeSet::new))));
	}

	/**
	 * What an artifact is called, without its version.
	 * @param artifact the resolved artifact
	 * @return the module it came from, or the project
	 */
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
		Files.write(destination, store(properties));
	}

	/**
	 * Renders the properties the way a {@code .properties} file is meant to look.
	 * <p>
	 * Two things have to be taken off {@link Properties#store}. It writes the current
	 * time as a comment whatever it is passed, which would make an unchanged starter
	 * produce a different file on every build; and it writes the platform's line
	 * separator, which would make the same starter produce a different file on Windows.
	 * <p>
	 * The stream overload is the one to render with. It is the overload that escapes
	 * anything outside Latin-1 as {@code \\uXXXX} — the writer overload leaves such
	 * characters as they are, which is unreadable to
	 * {@link Properties#load(java.io.InputStream)} and unwritable as ISO-8859-1.
	 * Everything that comes back is therefore ASCII, so decoding it to strip the comment
	 * and encoding it again loses nothing.
	 * @param properties the properties to render
	 * @return the file's contents
	 * @throws IOException if rendering fails
	 */
	private byte[] store(Properties properties) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		properties.store(buffer, null);
		String content = buffer.toString(StandardCharsets.ISO_8859_1)
			.lines()
			.filter((line) -> !line.startsWith("#"))
			.collect(Collectors.joining("\n", "", "\n"));
		return content.getBytes(StandardCharsets.ISO_8859_1);
	}

}
