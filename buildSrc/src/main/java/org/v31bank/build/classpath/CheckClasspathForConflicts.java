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

package org.v31bank.build.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

/**
 * Fails when two jars on the classpath contain the same entry.
 * <p>
 * Which of the two wins is decided by classpath order, which nothing in the build
 * guarantees and no test observes — so the wrong one is loaded silently, and the symptom
 * turns up somewhere unrelated at runtime. A starter exists to hand a consumer a set of
 * jars that work together; two copies of a class is that promise already broken.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckClasspathForConflicts extends ClasspathCheck {

	private final List<Predicate<String>> ignores = new ArrayList<>();

	@TaskAction
	void checkForConflicts() throws IOException {
		ClasspathContents contents = new ClasspathContents();
		for (File file : getClasspath()) {
			if (file.isDirectory()) {
				Path root = file.toPath();
				try (Stream<Path> paths = Files.walk(root)) {
					paths.filter(Files::isRegularFile)
						.forEach((entry) -> contents.add(root.relativize(entry).toString(), root.toString()));
				}
			}
			else {
				try (JarFile jar = new JarFile(file)) {
					for (JarEntry entry : Collections.list(jar.entries())) {
						if (!entry.isDirectory()) {
							contents.add(entry.getName(), file.getAbsolutePath());
						}
					}
				}
			}
		}
		Map<String, List<String>> conflicts = contents.getConflicts(this.ignores);
		if (!conflicts.isEmpty()) {
			StringBuilder message = new StringBuilder(String.format("Found classpath conflicts:%n"));
			conflicts.forEach((entry, locations) -> {
				message.append(String.format("    %s%n", entry));
				locations.forEach((location) -> message.append(String.format("        %s%n", location)));
			});
			throw new GradleException(message.toString());
		}
	}

	/**
	 * Accepts an entry that appears more than once anyway.
	 * <p>
	 * The escape hatch for a duplicate that is known and harmless. Adding one is a
	 * decision about a specific entry, made where it can be read and explained, rather
	 * than a check somebody turns off.
	 * @param predicate matches the entry names to accept
	 */
	public void ignore(Predicate<String> predicate) {
		this.ignores.add(predicate);
	}

	private static final class ClasspathContents {

		/**
		 * Entries that jars are expected to disagree about. Every jar carries its own
		 * licence and its own module descriptor; that is not a conflict.
		 */
		private static final Set<String> IGNORED_NAMES = new HashSet<>(Arrays.asList("about.html", "changelog.txt",
				"LICENSE", "license.txt", "module-info.class", "notice.txt", "readme.txt"));

		private final Map<String, List<String>> entries = new HashMap<>();

		private void add(String name, String source) {
			this.entries.computeIfAbsent(name, (key) -> new ArrayList<>()).add(source);
		}

		private Map<String, List<String>> getConflicts(List<Predicate<String>> ignores) {
			return this.entries.entrySet()
				.stream()
				.filter((entry) -> entry.getValue().size() > 1)
				.filter((entry) -> canConflict(entry.getKey(), ignores))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (first, second) -> first, TreeMap::new));
		}

		private boolean canConflict(String name, List<Predicate<String>> ignores) {
			if (name.startsWith("META-INF/")) {
				return false;
			}
			if (IGNORED_NAMES.contains(name)) {
				return false;
			}
			for (Predicate<String> ignore : ignores) {
				if (ignore.test(name)) {
					return false;
				}
			}
			return true;
		}

	}

}
