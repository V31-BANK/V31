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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

/**
 * A task that reads the auto-configurations a module registers and the classes it
 * registers them from.
 * <p>
 * Both halves are read here because neither is meaningful alone: the file names classes
 * and the classes claim to be registered, so anything worth saying about one is said by
 * looking at the other.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class AutoConfigurationImportsTask extends DefaultTask {

	/**
	 * The file Spring Boot reads a module's auto-configurations from.
	 */
	public static final String IMPORTS_FILE = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	private static final String COMMENT_START = "#";

	/**
	 * Where to look for the imports file. Not an input in its own right: one file out of
	 * it is what this task reads, and {@link #getSource()} is what declares that.
	 * @return the resources to look in
	 */
	@Internal
	public abstract ConfigurableFileCollection getResources();

	/**
	 * The imports file, of which a module has one or none. Absent is a state to be
	 * checked and not a reason to skip: a module that registers nothing while holding a
	 * class that expects to be registered is the mistake worth catching.
	 * @return the imports file
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public FileTree getSource() {
		return getResources().getAsFileTree().matching((filter) -> filter.include(IMPORTS_FILE));
	}

	/**
	 * Where the classes named by the imports file are compiled to.
	 * @return the classes to check against
	 */
	@Classpath
	public abstract ConfigurableFileCollection getClasspath();

	/**
	 * Where one of the registered classes was compiled to.
	 * @param className binary name of the class to look for
	 * @return its class file, if the classpath has it
	 */
	protected Optional<Path> findClassFile(String className) {
		String classFilePath = className.replace('.', '/') + ".class";
		return getClasspath().getFiles()
			.stream()
			.map((root) -> root.toPath().resolve(classFilePath))
			.filter(Files::isRegularFile)
			.findFirst();
	}

	/**
	 * The file itself, for a task that has something to say about it.
	 * @return the imports file, if the module has one
	 */
	protected Optional<File> importsFile() {
		return getSource().getFiles().stream().findFirst();
	}

	/**
	 * The classes the module registers, in the order the file lists them and without the
	 * comments and blank lines Spring Boot's own reader passes over. A module with no
	 * file registers nothing, which is a thing to be said rather than an error.
	 * @return the registered class names
	 */
	protected List<String> loadImports() {
		return importsFile().map(AutoConfigurationImportsTask::readLines).orElseGet(List::of);
	}

	private static List<String> readLines(File importsFile) {
		try {
			return Files.readAllLines(importsFile.toPath())
				.stream()
				.map(AutoConfigurationImportsTask::withoutComment)
				.filter((line) -> !line.isEmpty())
				.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + importsFile, ex);
		}
	}

	private static String withoutComment(String line) {
		int comment = line.indexOf(COMMENT_START);
		return ((comment == -1) ? line : line.substring(0, comment)).trim();
	}

}
