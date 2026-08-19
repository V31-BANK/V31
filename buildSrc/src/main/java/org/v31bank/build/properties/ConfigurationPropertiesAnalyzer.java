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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads configuration metadata files and records what is wrong with them, all of it in
 * one pass rather than the first thing found.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
final class ConfigurationPropertiesAnalyzer {

	private final Collection<File> sources;

	ConfigurationPropertiesAnalyzer(Collection<File> sources) {
		if (sources.isEmpty()) {
			throw new IllegalArgumentException("At least one source should be provided");
		}
		this.sources = sources;
	}

	/**
	 * The order carries no meaning, so holding it to one keeps a hand-written file
	 * mergeable.
	 * @param report where to record what was found
	 */
	void analyzeOrder(Report report) {
		analyze(report, (metadata) -> {
			Analysis analysis = new Analysis("Metadata element order:");
			forEachElementType(metadata, (elementType, names) -> {
				List<String> sorted = names.stream().sorted().toList();
				for (int i = 0; i < names.size(); i++) {
					if (!names.get(i).equals(sorted.get(i))) {
						analysis.add("Wrong order at $.%s[%d].name - expected '%s' but found '%s'"
							.formatted(elementType, i, sorted.get(i), names.get(i)));
					}
				}
			});
			return analysis;
		});
	}

	/**
	 * Which of two entries with one name wins is the reader's business, not the author's.
	 * @param report where to record what was found
	 */
	void analyzeDuplicates(Report report) {
		analyze(report, (metadata) -> {
			Analysis analysis = new Analysis("Metadata element duplicates:");
			forEachElementType(metadata, (elementType, names) -> {
				Set<String> seen = new HashSet<>();
				for (int i = 0; i < names.size(); i++) {
					if (!seen.add(names.get(i))) {
						analysis.add("Duplicate name '%s' at $.%s[%d]".formatted(names.get(i), elementType, i));
					}
				}
			});
			return analysis;
		});
	}

	/**
	 * The description is what an IDE shows whoever sets the property; without it they get
	 * a name and nothing else. Exclusions are exact names or prefixes ending in '.*'.
	 * @param report where to record what was found
	 * @param exclusions names allowed to say nothing
	 */
	void analyzePropertyDescription(Report report, List<String> exclusions) {
		analyze(report, (metadata) -> {
			Analysis analysis = new Analysis("The following properties have no description:");
			for (Map<String, Object> property : metadata.elements("properties")) {
				String name = (String) property.get("name");
				boolean deprecated = property.get("deprecation") != null;
				boolean described = property.get("description") != null;
				if (!deprecated && !described && !isExcluded(exclusions, name)) {
					analysis.add(name);
				}
			}
			return analysis;
		});
	}

	/**
	 * Without a version nobody upgrading can tell whether it was already gone where they
	 * were.
	 * @param report where to record what was found
	 */
	void analyzeDeprecationSince(Report report) {
		analyze(report, (metadata) -> {
			Analysis analysis = new Analysis("The following properties are deprecated without a 'since' version:");
			for (Map<String, Object> property : metadata.elements("properties")) {
				Object deprecation = property.get("deprecation");
				if (deprecation instanceof Map<?, ?> details && !details.containsKey("since")) {
					analysis.add((String) property.get("name"));
				}
			}
			return analysis;
		});
	}

	private void analyze(Report report, Function<ConfigurationMetadata, Analysis> rule) {
		for (File source : this.sources) {
			report.register(source, rule.apply(ConfigurationMetadata.of(source)));
		}
	}

	private void forEachElementType(ConfigurationMetadata metadata, BiConsumerOfNames action) {
		for (String elementType : ConfigurationMetadata.ELEMENT_TYPES) {
			action.accept(elementType, metadata.names(elementType));
		}
	}

	private boolean isExcluded(List<String> exclusions, String propertyName) {
		for (String exclusion : exclusions) {
			if (exclusion.equals(propertyName)) {
				return true;
			}
			if (exclusion.endsWith(".*") && propertyName.startsWith(exclusion.substring(0, exclusion.length() - 2))) {
				return true;
			}
		}
		return false;
	}

	@FunctionalInterface
	private interface BiConsumerOfNames {

		void accept(String elementType, List<String> names);

	}

	static final class Report {

		private final File baseDirectory;

		private final Map<File, List<Analysis>> analyses = new LinkedHashMap<>();

		Report(File baseDirectory) {
			this.baseDirectory = baseDirectory;
		}

		void register(File source, Analysis analysis) {
			this.analyses.computeIfAbsent(source, (file) -> new ArrayList<>()).add(analysis);
		}

		boolean hasProblems() {
			return this.analyses.values().stream().flatMap(List::stream).anyMatch(Analysis::hasProblems);
		}

		List<Analysis> analysesOf(File source) {
			return this.analyses.getOrDefault(source, List.of());
		}

		void write(File file) {
			try {
				Files.createDirectories(file.toPath().getParent());
				Files.writeString(file.toPath(), render(), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Failed to write " + file, ex);
			}
		}

		private String render() {
			if (this.analyses.isEmpty()) {
				return "No problems found.";
			}
			StringWriter rendered = new StringWriter();
			try (PrintWriter writer = new PrintWriter(rendered)) {
				separated(writer, this.analyses.entrySet(), (entry) -> {
					writer.println(this.baseDirectory.toPath().relativize(entry.getKey().toPath()));
					if (entry.getValue().stream().anyMatch(Analysis::hasProblems)) {
						separated(writer, entry.getValue(), (analysis) -> analysis.writeTo(writer));
					}
					else {
						writer.println("No problems found.");
					}
				});
			}
			return rendered.toString();
		}

		private static <T> void separated(PrintWriter writer, Iterable<T> elements, Consumer<T> write) {
			for (Iterator<T> iterator = elements.iterator(); iterator.hasNext();) {
				write.accept(iterator.next());
				if (iterator.hasNext()) {
					writer.println();
				}
			}
		}

	}

	static final class Analysis {

		private final String header;

		private final List<String> items = new ArrayList<>();

		Analysis(String header) {
			this.header = header;
		}

		void add(String item) {
			this.items.add(item);
		}

		boolean hasProblems() {
			return !this.items.isEmpty();
		}

		List<String> items() {
			return this.items;
		}

		void writeTo(PrintWriter writer) {
			writer.println(this.header);
			if (this.items.isEmpty()) {
				writer.println("No problems found.");
			}
			else {
				this.items.forEach((item) -> writer.println("\t- " + item));
			}
		}

		@Override
		public String toString() {
			StringWriter rendered = new StringWriter();
			writeTo(new PrintWriter(rendered, true));
			return rendered.toString();
		}

	}

}
