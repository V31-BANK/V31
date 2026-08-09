package org.v31bank.build.starters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
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

	private Configuration dependencies;

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
	public FileCollection getDependencies() {
		return this.dependencies;
	}

	public void setDependencies(Configuration dependencies) {
		this.dependencies = dependencies;
	}

	@OutputFile
	public abstract RegularFileProperty getDestination();

	@TaskAction
	void generateMetadata() throws IOException {
		Set<String> artifacts = this.dependencies.getResolvedConfiguration()
			.getResolvedArtifacts()
			.stream()
			.map(ResolvedArtifact::getName)
			.collect(Collectors.toCollection(TreeSet::new));
		Properties properties = new Properties();
		properties.setProperty("name", getStarterName().get());
		properties.setProperty("description", getStarterDescription().get());
		properties.setProperty("dependencies", String.join(",", artifacts));
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
