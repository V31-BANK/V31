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
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.properties.ConfigurationPropertiesAnalyzer.Analysis;
import org.v31bank.build.properties.ConfigurationPropertiesAnalyzer.Report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationPropertiesAnalyzer}.
 * <p>
 * Every rule is asked once about a file that breaks it and once about a file that does
 * not, because a rule that never passes and a rule that never fails are equally useless.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ConfigurationPropertiesAnalyzerTests {

	@TempDir
	private Path directory;

	@Test
	void refusesToAnalyseNothing() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigurationPropertiesAnalyzer(List.of()));
	}

	@Test
	void findsEntriesOutOfOrder() {
		File source = write(MetadataFiles.metadata().property("v31.b", "B").property("v31.a", "A"));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeOrder)).containsExactly(
				"Wrong order at $.properties[0].name - expected 'v31.a' but found 'v31.b'",
				"Wrong order at $.properties[1].name - expected 'v31.b' but found 'v31.a'");
	}

	@Test
	void findsNothingWrongWithEntriesInOrder() {
		File source = write(
				MetadataFiles.metadata().group("v31.a").property("v31.a", "A").property("v31.b", "B").hint("v31.a"));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeOrder)).isEmpty();
	}

	@Test
	void findsAnEntryWrittenTwice() {
		File source = write(MetadataFiles.metadata().property("v31.a", "A").property("v31.a", "Again"));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeDuplicates))
			.containsExactly("Duplicate name 'v31.a' at $.properties[1]");
	}

	@Test
	void findsNothingWrongWithDistinctEntries() {
		File source = write(MetadataFiles.metadata().property("v31.a", "A").property("v31.b", "B"));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeDuplicates)).isEmpty();
	}

	@Test
	void findsAPropertyThatSaysNothing() {
		File source = write(MetadataFiles.metadata().property("v31.a", "A").undescribed("v31.b"));
		assertThat(describedItems(source, List.of())).containsExactly("v31.b");
	}

	@Test
	void letsADeprecatedPropertySayNothing() {
		File source = write(MetadataFiles.metadata().deprecated("v31.gone", "v31.a", "0.1.0"));
		assertThat(describedItems(source, List.of())).isEmpty();
	}

	@Test
	void letsAnExcludedPropertySayNothing() {
		File source = write(MetadataFiles.metadata().undescribed("v31.a").undescribed("v31.b"));
		assertThat(describedItems(source, List.of("v31.a", "v31.b"))).isEmpty();
	}

	@Test
	void letsAWholeExcludedPrefixSayNothing() {
		File source = write(MetadataFiles.metadata().undescribed("v31.internal.a").undescribed("v31.internal.b"));
		assertThat(describedItems(source, List.of("v31.internal.*"))).isEmpty();
	}

	@Test
	void findsADeprecationWithNoVersion() {
		File source = write(MetadataFiles.metadata().deprecated("v31.gone", "v31.a", null));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeDeprecationSince))
			.containsExactly("v31.gone");
	}

	@Test
	void findsNothingWrongWithADeprecationThatHasOne() {
		File source = write(MetadataFiles.metadata().deprecated("v31.gone", "v31.a", "0.1.0"));
		assertThat(itemsOf(source, ConfigurationPropertiesAnalyzer::analyzeDeprecationSince)).isEmpty();
	}

	@Test
	void writesAReportSayingWhereEachProblemIs() {
		File source = write(MetadataFiles.metadata().undescribed("v31.a"));
		Report report = new Report(this.directory.toFile());
		new ConfigurationPropertiesAnalyzer(List.of(source)).analyzePropertyDescription(report, List.of());
		File reportFile = this.directory.resolve("report.txt").toFile();
		report.write(reportFile);
		assertThat(reportFile).content()
			.contains("metadata.json")
			.contains("The following properties have no description:")
			.contains("- v31.a");
	}

	@Test
	void writesAReportSayingSoWhenThereIsNothingWrong() {
		File source = write(MetadataFiles.metadata().property("v31.a", "A"));
		Report report = new Report(this.directory.toFile());
		new ConfigurationPropertiesAnalyzer(List.of(source)).analyzePropertyDescription(report, List.of());
		File reportFile = this.directory.resolve("report.txt").toFile();
		report.write(reportFile);
		assertThat(report.hasProblems()).isFalse();
		assertThat(reportFile).content().contains("No problems found.");
	}

	private List<String> describedItems(File source, List<String> exclusions) {
		Report report = new Report(this.directory.toFile());
		new ConfigurationPropertiesAnalyzer(List.of(source)).analyzePropertyDescription(report, exclusions);
		return items(report, source);
	}

	private List<String> itemsOf(File source, BiConsumer<ConfigurationPropertiesAnalyzer, Report> rule) {
		Report report = new Report(this.directory.toFile());
		rule.accept(new ConfigurationPropertiesAnalyzer(List.of(source)), report);
		return items(report, source);
	}

	private List<String> items(Report report, File source) {
		return report.analysesOf(source).stream().map(Analysis::items).flatMap(List::stream).toList();
	}

	private File write(MetadataFiles metadata) {
		return metadata.writeTo(this.directory.resolve("metadata.json")).toFile();
	}

}
