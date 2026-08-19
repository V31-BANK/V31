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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationException;

import org.v31bank.build.autoconfigure.AutoConfigurationClass.Attribute;
import org.v31bank.build.autoconfigure.AutoConfigurationClass.Reference;

/**
 * Fails when an {@code @AutoConfiguration} class is not what the module says it is.
 * <p>
 * Where {@link CheckAutoConfigurationImports} starts from the file and asks whether the
 * classes are there, this starts from the classes and asks whether the file knows about
 * them: a new auto-configuration that nobody registered does nothing at all, and says
 * nothing about it.
 * <p>
 * It also settles the one question a compiler cannot. {@code before} and {@code after}
 * name a class, which loads it as the annotation is read, so pointing them at a
 * dependency a consumer may not have brings the whole context down; {@code beforeName}
 * and {@code afterName} take a string, which does not, but a string pointing at a class
 * that is always there is a name nothing will ever tell you has gone stale.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckAutoConfigurationClasses extends AutoConfigurationImportsTask {

	private static final String CLASS_NAME_SUFFIX = "AutoConfiguration";

	private static final String CLASS_FILE_SUFFIX = ".class";

	/**
	 * What the module compiles and runs against no matter what a consumer asks for.
	 * @return the required dependencies
	 */
	@Classpath
	public abstract ConfigurableFileCollection getRequiredDependencies();

	/**
	 * What the module compiles against but a consumer may never resolve.
	 * @return the optional dependencies
	 */
	@Classpath
	public abstract ConfigurableFileCollection getOptionalDependencies();

	/**
	 * Classes that carry the annotation and are deliberately left out of the imports
	 * file, which is otherwise reported as a class nobody registered.
	 * @return the class names to leave unregistered
	 */
	@Input
	public abstract SetProperty<String> getOmittedFromImports();

	@TaskAction
	void check() {
		List<AutoConfigurationClass> autoConfigurations = autoConfigurationClasses();
		Problems problems = new Problems();
		checkRegistration(autoConfigurations, loadImports(), problems);
		checkOrdering(autoConfigurations, problems);
		if (!problems.isEmpty()) {
			throw new VerificationException(problems.report());
		}
	}

	private void checkRegistration(List<AutoConfigurationClass> autoConfigurations, List<String> imports,
			Problems problems) {
		Set<String> omitted = getOmittedFromImports().get();
		for (AutoConfigurationClass autoConfiguration : autoConfigurations) {
			String name = autoConfiguration.name();
			if (!name.endsWith(CLASS_NAME_SUFFIX)) {
				problems.add(name, "name should end with %s".formatted(CLASS_NAME_SUFFIX));
			}
			boolean registered = imports.contains(name);
			if (omitted.contains(name) && registered) {
				problems.add(name, "is registered in %s but declared as omitted from it".formatted(IMPORTS_FILE));
			}
			else if (!omitted.contains(name) && !registered) {
				problems.add(name, "is not registered in %s".formatted(IMPORTS_FILE));
			}
		}
	}

	private void checkOrdering(List<AutoConfigurationClass> autoConfigurations, Problems problems) {
		// Reading every class name off two classpaths is not cheap, and a module whose
		// auto-configurations order themselves against nothing has no use for either.
		if (autoConfigurations.stream().allMatch((autoConfiguration) -> autoConfiguration.references().isEmpty())) {
			return;
		}
		Set<String> required = classNamesIn(getRequiredDependencies());
		Set<String> optionalOnly = new HashSet<>(classNamesIn(getOptionalDependencies()));
		optionalOnly.removeAll(required);
		for (AutoConfigurationClass autoConfiguration : autoConfigurations) {
			for (Reference reference : autoConfiguration.references()) {
				problemWith(reference, required, optionalOnly)
					.ifPresent((problem) -> problems.add(autoConfiguration.name(), problem));
			}
		}
	}

	/**
	 * The rule both forms of the attribute are held to: refer to a class that may be
	 * absent by name, and to one that is always there as a class.
	 * @param reference what one auto-configuration says about another
	 * @param required every class the module resolves no matter what
	 * @param optionalOnly every class it resolves only through an optional dependency
	 * @return the problem, if the wrong form was used
	 */
	private Optional<String> problemWith(Reference reference, Set<String> required, Set<String> optionalOnly) {
		Attribute attribute = reference.attribute();
		String className = reference.className();
		boolean mayBeAbsent = optionalOnly.contains(className);
		if (!attribute.refersByName()) {
			return mayBeAbsent ? Optional.of(problem(attribute, className, "is from an optional dependency"))
					: Optional.empty();
		}
		if (mayBeAbsent) {
			return Optional.empty();
		}
		return Optional.of(required.contains(className) ? problem(attribute, className, "is from a required dependency")
				: "%s '%s' was not found".formatted(attribute.attributeName(), className));
	}

	private String problem(Attribute attribute, String className, String because) {
		return "%s '%s' %s and belongs in %s".formatted(attribute.attributeName(), className, because,
				attribute.counterpart().attributeName());
	}

	private List<AutoConfigurationClass> autoConfigurationClasses() {
		List<AutoConfigurationClass> autoConfigurations = new ArrayList<>();
		for (File root : getClasspath().getFiles()) {
			if (root.isDirectory()) {
				walk(root.toPath(),
						(classFile) -> AutoConfigurationClass.of(classFile).ifPresent(autoConfigurations::add));
			}
		}
		return autoConfigurations;
	}

	private static Set<String> classNamesIn(FileCollection classpath) {
		Set<String> classNames = new HashSet<>();
		for (File file : classpath.getFiles()) {
			if (file.isDirectory()) {
				Path root = file.toPath();
				walk(root, (classFile) -> classNames.add(classNameOf(root.relativize(classFile).toString())));
			}
			else if (file.getName().endsWith(".jar")) {
				readJar(file, classNames);
			}
		}
		return classNames;
	}

	private static void readJar(File jar, Set<String> classNames) {
		try (JarFile jarFile = new JarFile(jar)) {
			jarFile.stream()
				.filter((entry) -> !entry.isDirectory())
				.map(JarEntry::getName)
				.filter((name) -> name.endsWith(CLASS_FILE_SUFFIX))
				.map(CheckAutoConfigurationClasses::classNameOf)
				.forEach(classNames::add);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + jar, ex);
		}
	}

	private static void walk(Path root, Consumer<Path> classFiles) {
		try (Stream<Path> files = Files.walk(root)) {
			files.filter(Files::isRegularFile)
				.filter((file) -> file.getFileName().toString().endsWith(CLASS_FILE_SUFFIX))
				.forEach(classFiles);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + root, ex);
		}
	}

	private static String classNameOf(String classFilePath) {
		String withoutSuffix = classFilePath.substring(0, classFilePath.length() - CLASS_FILE_SUFFIX.length());
		return withoutSuffix.replace(File.separatorChar, '/').replace('/', '.');
	}

	/**
	 * What was found, gathered by the class it was found in so that a module with several
	 * problems is fixed in one pass rather than one build at a time.
	 */
	private static final class Problems {

		private final Map<String, List<String>> byClassName = new TreeMap<>();

		private void add(String className, String problem) {
			this.byClassName.computeIfAbsent(className, (name) -> new ArrayList<>()).add(problem);
		}

		private boolean isEmpty() {
			return this.byClassName.isEmpty();
		}

		private String report() {
			StringBuilder report = new StringBuilder("Found auto-configuration problems:%n".formatted());
			this.byClassName.forEach((className, problems) -> {
				report.append("    %s:%n".formatted(className));
				problems.forEach((problem) -> report.append("        - %s%n".formatted(problem)));
			});
			return report.toString();
		}

	}

}
