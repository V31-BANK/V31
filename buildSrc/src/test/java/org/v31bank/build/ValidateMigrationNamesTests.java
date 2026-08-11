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

package org.v31bank.build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Tests for {@link ValidateMigrationNames}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ValidateMigrationNamesTests {

	@TempDir
	private Path directory;

	private ValidateMigrationNames task;

	private File report;

	@BeforeEach
	void setUp() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory.toFile()).build();
		this.task = project.getTasks().register("validateMigrationNames", ValidateMigrationNames.class).get();
		this.report = this.directory.resolve("report.txt").toFile();
		this.task.getReport().set(this.report);
	}

	@Test
	void acceptsAVersionedMigration() {
		givenMigrations("V20260730093000__customer_category_create.sql");
		validate();
	}

	@Test
	void acceptsEveryVerbTheConventionAllows() {
		givenMigrations("V20260730093001__customer_create.sql", "V20260730093002__customer_drop.sql",
				"V20260730093003__customer_add_email.sql", "V20260730093004__customer_alter_audit.sql",
				"V20260730093005__customer_rename.sql", "V20260730093006__customer_index_email.sql",
				"V20260730093007__customer_constraint_unique_code.sql", "V20260730093008__customer_seed.sql",
				"V20260730093009__customer_backfill_status.sql");
		validate();
	}

	@Test
	void acceptsARepeatableMigration() {
		givenMigrations("R__customer_summary_view.sql", "R__customer_risk_score_function.sql",
				"R__customer_audit_procedure.sql", "R__customer_history_trigger.sql");
		validate();
	}

	@Test
	void rejectsAVersionedNameThatDoesNotFollowTheConvention() {
		givenMigrations("V20260730093000__Customer_create.sql");
		assertThat(failure()).contains("a versioned migration is named");
	}

	@Test
	void rejectsAVerbTheConventionDoesNotList() {
		givenMigrations("V20260730093000__customer_delete.sql");
		assertThat(failure()).contains("a versioned migration is named");
	}

	@Test
	void rejectsARepeatableNameThatDoesNotFollowTheConvention() {
		givenMigrations("R__customer_summary_table.sql");
		assertThat(failure()).contains("a repeatable migration is named");
	}

	/**
	 * The pattern only checks that the version is fourteen digits, which
	 * {@code 99999999999999} satisfies.
	 */
	@Test
	void rejectsAVersionThatIsNotARealInstant() {
		givenMigrations("V99999999999999__customer_create.sql");
		assertThat(failure()).contains("is not a real yyyyMMddHHmmss timestamp");
	}

	@Test
	void rejectsAnImpossibleMonthDayHourOrMinute() {
		for (String version : new String[] { "20261330093000", "20260732093000", "20260730253000", "20260730096100" }) {
			setUp();
			givenMigrations("V" + version + "__customer_create.sql");
			assertThat(failure()).as(version).contains("is not a real yyyyMMddHHmmss timestamp");
		}
	}

	/**
	 * Flyway applies one of them, records the version, and silently skips the other for
	 * the lifetime of the schema — which is why this is worth failing a build over.
	 */
	@Test
	void rejectsTwoMigrationsClaimingTheSameVersion() {
		givenMigrations("V20260730093000__customer_create.sql", "V20260730093000__wallet_create.sql");
		String message = failure();
		assertThat(message).contains("is already taken by");
		assertThat(message).contains("V20260730093000__customer_create.sql");
		assertThat(message).contains("V20260730093000__wallet_create.sql");
	}

	/**
	 * Reported together rather than one per run, so a batch of renamed files is fixed in
	 * one pass instead of one build at a time.
	 */
	@Test
	void reportsEveryProblemAtOnce() {
		givenMigrations("V20260730093000__Customer_create.sql", "R__customer_summary_table.sql",
				"V99999999999999__wallet_create.sql");
		String message = failure();
		assertThat(message).contains("a versioned migration is named");
		assertThat(message).contains("a repeatable migration is named");
		assertThat(message).contains("is not a real yyyyMMddHHmmss timestamp");
	}

	/**
	 * A malformed name is reported once. Without the early exit its version would also be
	 * parsed, adding a second complaint about the same file.
	 */
	@Test
	void reportsAMalformedNameOnlyOnce() {
		givenMigrations("V20260730093000__Customer_create.sql");
		assertThat(failure().lines().filter((line) -> line.contains("Customer_create")).count()).isEqualTo(1);
	}

	@Test
	void writesTheReportWhenTheNamesCheckOut() throws IOException {
		givenMigrations("V20260730093000__customer_create.sql", "V20260730093001__wallet_create.sql");
		validate();
		assertThat(Files.readString(this.report.toPath()).strip()).isEqualTo("2 migrations checked");
	}

	@Test
	void writesNoReportWhenAMigrationIsRejected() {
		givenMigrations("V20260730093000__Customer_create.sql");
		failure();
		assertThat(this.report).as("the marker must not survive a failed check").doesNotExist();
	}

	@Test
	void acceptsNoMigrationsAtAll() throws IOException {
		validate();
		assertThat(Files.readString(this.report.toPath()).strip()).isEqualTo("0 migrations checked");
	}

	/**
	 * A versioned migration is written into the directory named for its year, because
	 * where it sits is part of what is being checked. A name too malformed to have a year
	 * is written at the top, and fails on its name before its location is reached.
	 * @param names the migrations to create
	 */
	/**
	 * Flyway scans recursively and orders by version alone, so this never breaks anything
	 * — it breaks reading, which is the only reason the year directories exist.
	 */
	@Test
	void rejectsAVersionedMigrationFiledUnderTheWrongYear() throws IOException {
		Path wrong = this.directory.resolve("2027");
		Files.createDirectories(wrong);
		Path file = wrong.resolve("V20260729010000__customer_create.sql");
		Files.writeString(file, "select 1;");
		this.task.getMigrations().from(file.toFile());
		assertThat(failure()).contains("a versioned migration lives in db/migration/2026, not 2027");
	}

	private void givenMigrations(String... names) {
		for (String name : names) {
			Path parent = (name.startsWith("V") && name.length() > 5) ? this.directory.resolve(name.substring(1, 5))
					: this.directory;
			Path file = parent.resolve(name);
			try {
				Files.createDirectories(parent);
				Files.writeString(file, "select 1;");
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
			this.task.getMigrations().from(file.toFile());
		}
	}

	private void validate() {
		try {
			this.task.validate();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private String failure() {
		return catchThrowableOfType(GradleException.class, this::validate).getMessage();
	}

}
