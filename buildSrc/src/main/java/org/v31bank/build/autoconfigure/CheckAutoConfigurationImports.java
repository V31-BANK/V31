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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

/**
 * Fails when the imports file names something it should not.
 * <p>
 * Spring Boot reads this file and nothing else to find a module's auto-configurations. A
 * name that no longer resolves, or one that resolves to a class somebody removed the
 * annotation from, is not an error at build time and not an error at startup either: the
 * configuration is quietly skipped, and the beans it would have contributed are missing
 * wherever they were expected.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckAutoConfigurationImports extends AutoConfigurationImportsTask {

	@TaskAction
	void check() {
		// No file is not this task's business: whether one was needed is a question about
		// the classes, and CheckAutoConfigurationClasses is the one that asks it.
		importsFile().ifPresent(this::check);
	}

	private void check(File importsFile) {
		List<String> imports = loadImports();
		List<String> problems = new ArrayList<>();
		imports.forEach((registered) -> checkRegistered(registered).ifPresent(problems::add));
		checkSorted(imports).ifPresent(problems::add);
		if (!problems.isEmpty()) {
			throw new VerificationException(report(importsFile, problems));
		}
	}

	private Optional<String> checkRegistered(String className) {
		Optional<Path> classFile = findClassFile(className);
		if (classFile.isEmpty()) {
			return Optional.of("'%s' was not found".formatted(className));
		}
		if (AutoConfigurationClass.of(classFile.get()).isEmpty()) {
			return Optional.of("'%s' is not annotated with @AutoConfiguration".formatted(className));
		}
		return Optional.empty();
	}

	/**
	 * The order of the file decides nothing — {@code before} and {@code after} do — so
	 * holding it to alphabetical order costs a module nothing and makes a diff of it mean
	 * something.
	 * @param imports the registered class names, in the order the file lists them
	 * @return the problem, if the file is out of order
	 */
	private Optional<String> checkSorted(List<String> imports) {
		List<String> sorted = imports.stream().sorted().toList();
		if (sorted.equals(imports)) {
			return Optional.empty();
		}
		return Optional.of("entries should be sorted alphabetically:%n%s".formatted(indented(sorted)));
	}

	private String report(File importsFile, List<String> problems) {
		return "Found problems in %s:%n%s".formatted(importsFile,
				problems.stream().map((problem) -> "    - " + problem).collect(Collectors.joining("%n".formatted())));
	}

	private String indented(List<String> lines) {
		return lines.stream().map((line) -> "        " + line).collect(Collectors.joining("%n".formatted()));
	}

}
