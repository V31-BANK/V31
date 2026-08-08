package org.v31bank.build;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Fails the build on a migration whose name does not follow the convention in
 * {@code :V31-customer-service:db/migration/README.md}, or that reuses a version another migration already
 * took.
 * <p>
 * A migration is checked here rather than at deployment because that is the last moment
 * it is still cheap. Flyway records a version the first time it applies one; by the time
 * a duplicate or a malformed name is noticed in an environment, the fix is a manual
 * repair of {@code flyway_schema_history} in every environment that got there first.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class ValidateMigrationNames extends DefaultTask {

	/**
	 * Applied once, in version order:
	 * {@code V{yyyyMMddHHmmss}__{table}_{verb}[_{detail}].sql}.
	 */
	private static final Pattern VERSIONED = Pattern.compile("^V\\d{14}__[a-z][a-z0-9_]*"
			+ "_(create|drop|add|alter|rename|index|constraint|seed|backfill)(_[a-z0-9_]+)?\\.sql$");

	/**
	 * Re-applied whenever its checksum changes: {@code R__{object}_{kind}.sql}.
	 */
	private static final Pattern REPEATABLE = Pattern
		.compile("^R__[a-z][a-z0-9_]*_(view|function|procedure|trigger)\\.sql$");

	private static final DateTimeFormatter VERSION_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	/**
	 * The migrations to check.
	 * @return the migration files
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract ConfigurableFileCollection getMigrations();

	@TaskAction
	public void validate() {
		List<String> problems = new ArrayList<>();
		Map<String, String> byVersion = new LinkedHashMap<>();
		for (File migration : getMigrations().getFiles()) {
			String name = migration.getName();
			if (name.startsWith("R__")) {
				if (!REPEATABLE.matcher(name).matches()) {
					problems.add(name + " — a repeatable migration is named R__<object>_<kind>.sql, where <kind> is "
							+ "view, function, procedure or trigger");
				}
				continue;
			}
			if (!VERSIONED.matcher(name).matches()) {
				problems.add(name + " — a versioned migration is named V<yyyyMMddHHmmss>__<table>_<verb>[_<detail>]"
						+ ".sql, where <verb> is create, drop, add, alter, rename, index, constraint, seed or "
						+ "backfill");
				continue;
			}
			String version = name.substring(1, 15);
			if (!isRealInstant(version)) {
				problems.add(name + " — " + version + " is not a real yyyyMMddHHmmss timestamp");
				continue;
			}
			String taken = byVersion.putIfAbsent(version, name);
			if (taken != null) {
				// Flyway applies one and records the version; the other is then
				// silently skipped for the lifetime of the schema.
				problems.add(name + " — version " + version + " is already taken by " + taken);
			}
		}
		if (!problems.isEmpty()) {
			throw new GradleException("Migration names do not follow :V31-customer-service:db/migration/README.md:" + System.lineSeparator()
					+ "  " + String.join(System.lineSeparator() + "  ", problems));
		}
	}

	private static boolean isRealInstant(String version) {
		try {
			VERSION_TIMESTAMP.parse(version);
			return true;
		}
		catch (DateTimeParseException ex) {
			return false;
		}
	}

}
