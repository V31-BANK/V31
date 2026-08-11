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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Fails the build on a migration whose name does not follow the convention in
 * {@code :V31-customer-service:db/migration/README.md}, or that reuses a version another
 * migration already took.
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
	 * <p>
	 * Fingerprinted by path relative to the migration directory rather than by absolute
	 * path, so a checkout in a different directory — a build agent, another developer —
	 * produces the same fingerprint and can reuse the same result.
	 * @return the migration files
	 */
	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract ConfigurableFileCollection getMigrations();

	/**
	 * Written when the names check out, and otherwise the only reason this task can be
	 * skipped.
	 * <p>
	 * A task that declares no output is never up to date: Gradle has nothing to verify a
	 * previous run by, so it re-runs on every build regardless of what
	 * {@link #getMigrations()} was fingerprinted as. This file is what lets the
	 * fingerprint be used for what it was declared for.
	 * @return the marker file
	 */
	@OutputFile
	public abstract RegularFileProperty getReport();

	@TaskAction
	public void validate() throws IOException {
		List<String> problems = new ArrayList<>();
		Map<String, String> byVersion = new HashMap<>();
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
			String year = version.substring(0, 4);
			String directory = migration.getParentFile().getName();
			if (!year.equals(directory)) {
				// Flyway scans recursively and orders by version alone, so this never
				// breaks anything. It breaks reading: the year directory is how the
				// history is browsed, and one file in the wrong one is invisible there.
				problems.add(name + " — a versioned migration lives in db/migration/" + year + ", not " + directory);
			}
			String taken = byVersion.putIfAbsent(version, name);
			if (taken != null) {
				// Flyway applies one and records the version; the other is then
				// silently skipped for the lifetime of the schema.
				problems.add(name + " — version " + version + " is already taken by " + taken);
			}
		}
		if (!problems.isEmpty()) {
			throw new GradleException("Migration names do not follow :V31-customer-service:db/migration/README.md:"
					+ System.lineSeparator() + "  " + String.join(System.lineSeparator() + "  ", problems));
		}
		Files.writeString(getReport().get().getAsFile().toPath(),
				byVersion.size() + " migrations checked" + System.lineSeparator());
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
