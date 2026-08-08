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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		assertTrue(failure().contains("a versioned migration is named"));
	}

	@Test
	void rejectsAVerbTheConventionDoesNotList() {
		givenMigrations("V20260730093000__customer_delete.sql");
		assertTrue(failure().contains("a versioned migration is named"));
	}

	@Test
	void rejectsARepeatableNameThatDoesNotFollowTheConvention() {
		givenMigrations("R__customer_summary_table.sql");
		assertTrue(failure().contains("a repeatable migration is named"));
	}

	/**
	 * The pattern only checks that the version is fourteen digits, which
	 * {@code 99999999999999} satisfies.
	 */
	@Test
	void rejectsAVersionThatIsNotARealInstant() {
		givenMigrations("V99999999999999__customer_create.sql");
		assertTrue(failure().contains("is not a real yyyyMMddHHmmss timestamp"));
	}

	@Test
	void rejectsAnImpossibleMonthDayHourOrMinute() {
		for (String version : new String[] { "20261330093000", "20260732093000", "20260730253000", "20260730096100" }) {
			setUp();
			givenMigrations("V" + version + "__customer_create.sql");
			assertTrue(failure().contains("is not a real yyyyMMddHHmmss timestamp"), version);
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
		assertTrue(message.contains("is already taken by"), message);
		assertTrue(message.contains("V20260730093000__customer_create.sql"), message);
		assertTrue(message.contains("V20260730093000__wallet_create.sql"), message);
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
		assertTrue(message.contains("a versioned migration is named"), message);
		assertTrue(message.contains("a repeatable migration is named"), message);
		assertTrue(message.contains("is not a real yyyyMMddHHmmss timestamp"), message);
	}

	/**
	 * A malformed name is reported once. Without the early exit its version would also be
	 * parsed, adding a second complaint about the same file.
	 */
	@Test
	void reportsAMalformedNameOnlyOnce() {
		givenMigrations("V20260730093000__Customer_create.sql");
		assertEquals(1, failure().lines().filter((line) -> line.contains("Customer_create")).count());
	}

	@Test
	void writesTheReportWhenTheNamesCheckOut() throws IOException {
		givenMigrations("V20260730093000__customer_create.sql", "V20260730093001__wallet_create.sql");
		validate();
		assertEquals("2 migrations checked", Files.readString(this.report.toPath()).strip());
	}

	@Test
	void writesNoReportWhenAMigrationIsRejected() {
		givenMigrations("V20260730093000__Customer_create.sql");
		failure();
		assertTrue(!this.report.exists(), "the marker must not survive a failed check");
	}

	@Test
	void acceptsNoMigrationsAtAll() throws IOException {
		validate();
		assertEquals("0 migrations checked", Files.readString(this.report.toPath()).strip());
	}

	private void givenMigrations(String... names) {
		for (String name : names) {
			Path file = this.directory.resolve(name);
			try {
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
		return assertThrows(GradleException.class, this::validate).getMessage();
	}

}
