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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.v31bank.build.task.TaskDependencies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Tests for {@link FlywayConventions}.
 * <p>
 * The conventions react to the Spring Boot plugin rather than being applied outright, so
 * each test applies them first and the plugin afterwards — the order the real build uses,
 * where the root applies conventions before a subproject's own build file has run.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class FlywayConventionsTests {

	private static final String TASK_NAME = "validateMigrationNames";

	private static final String MIGRATION = "V20260730093000__customer_category_create.sql";

	@TempDir
	private File directory;

	/**
	 * A library, a starter and an auto-configuration module have no database. Registering
	 * the task for them would put a name in {@code ./gradlew tasks} that never has
	 * anything to do, and Flyway on a classpath with no use for it.
	 */
	@Test
	void leavesAProjectThatIsNotASpringBootApplicationAlone() {
		givenAMigration();
		Project project = javaProject();
		assertThat(project.getTasks().findByName(TASK_NAME)).isNull();
		assertThat(runtimeOnly(project)).isEmpty();
	}

	@Test
	void registersTheCheckOnASpringBootApplication() {
		assertThat(springBootProject().getTasks().findByName(TASK_NAME)).isNotNull();
	}

	@Test
	void presentsTheCheckAsAVerificationTask() {
		Task check = springBootProject().getTasks().getByName(TASK_NAME);
		assertThat(check.getGroup()).isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
		assertThat(check.getDescription()).isEqualTo("Checks migration file names against db/migration/README.md.");
	}

	@Test
	void checksTheMigrationsTheProjectShips() {
		givenAMigration();
		assertThat(migrationsOf(springBootProject())).singleElement()
			.satisfies((file) -> assertThat(file).hasName(MIGRATION));
	}

	/**
	 * Flyway scans {@code db/migration} recursively and V31 groups the migrations into a
	 * directory per year, so a check that only looked one level down would pass on
	 * everything written after the first January.
	 */
	@Test
	void looksInsideTheYearDirectories() {
		givenAMigration();
		givenResource("src/main/resources/db/migration/2027/V20270102030405__wallet_create.sql");
		assertThat(migrationsOf(springBootProject())).hasSize(2);
	}

	/**
	 * The README beside the migrations is the convention they are checked against, and it
	 * is not one of them.
	 */
	@Test
	void ignoresWhateverElseSitsBesideTheMigrations() {
		givenAMigration();
		givenResource("src/main/resources/db/migration/README.md");
		assertThat(migrationsOf(springBootProject())).singleElement()
			.satisfies((file) -> assertThat(file).hasName(MIGRATION));
	}

	@Test
	void ignoresSqlThatIsNotAMigration() {
		givenResource("src/main/resources/sql/report.sql");
		assertThat(migrationsOf(springBootProject())).isEmpty();
	}

	/**
	 * Matched against the main resources rather than against a hard-coded
	 * {@code src/main/resources}. A project that adds a resource directory would
	 * otherwise leave the check scanning nothing, and a check that finds nothing passes.
	 */
	@Test
	void followsTheResourcesWhereverTheProjectPutsThem() {
		Project project = springBootProject();
		mainSourceSet(project).getResources().srcDir("src/main/sql");
		givenResource("src/main/sql/db/migration/2026/" + MIGRATION);
		assertThat(migrationsOf(project)).singleElement().satisfies((file) -> assertThat(file).hasName(MIGRATION));
	}

	/**
	 * A migration reaches an environment by being on the classpath, and this is where it
	 * gets there — so checking first is what keeps a broken name out of the jar.
	 */
	@Test
	void runsBeforeTheMigrationsAreCopiedIntoTheJar() {
		Task processResources = springBootProject().getTasks().getByName(JavaPlugin.PROCESS_RESOURCES_TASK_NAME);
		assertThat(TaskDependencies.namesOf(processResources.getDependsOn())).contains(TASK_NAME);
	}

	/**
	 * A failure that only ever surfaces while packaging reads as a packaging problem,
	 * when it is a verification one.
	 */
	@Test
	void runsAsPartOfCheck() {
		Task check = springBootProject().getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME);
		assertThat(TaskDependencies.namesOf(check.getDependsOn())).contains(TASK_NAME);
	}

	/**
	 * The task declares no other output, and one that declares none is never considered
	 * up to date.
	 */
	@Test
	void writesItsReportIntoTheBuildDirectory() {
		Project project = springBootProject();
		ValidateMigrationNames check = (ValidateMigrationNames) project.getTasks().getByName(TASK_NAME);
		Path report = check.getReport().get().getAsFile().toPath();
		assertThat(project.getProjectDir().toPath().relativize(report))
			.isEqualTo(Path.of("build", "reports", "migration-names.txt"));
	}

	@Test
	void givesAServiceThatShipsMigrationsWhatItTakesToApplyThem() {
		givenAMigration();
		assertThat(runtimeOnly(springBootProject())).extracting(Dependency::getGroup, Dependency::getName)
			.containsExactlyInAnyOrder(tuple("org.springframework.boot", "spring-boot-flyway"),
					tuple("org.flywaydb", "flyway-database-postgresql"));
	}

	/**
	 * Flyway would start, find nothing to apply, and leave the service carrying a
	 * migration tool it has no use for.
	 */
	@Test
	void leavesFlywayOffAServiceThatShipsNone() {
		assertThat(runtimeOnly(springBootProject())).isEmpty();
	}

	/**
	 * Whether there are migrations is answered when the classpath is resolved rather than
	 * while the build is being configured, so the first one a service ever writes takes
	 * effect without a sync.
	 */
	@Test
	void noticesAMigrationWrittenAfterTheProjectWasConfigured() {
		Project project = springBootProject();
		assertThat(runtimeOnly(project)).isEmpty();
		givenAMigration();
		assertThat(runtimeOnly(project)).isNotEmpty();
	}

	private void givenAMigration() {
		givenResource("src/main/resources/db/migration/2026/" + MIGRATION);
	}

	private void givenResource(String path) {
		Path file = this.directory.toPath().resolve(path);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, "select 1;");
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static List<File> migrationsOf(Project project) {
		ValidateMigrationNames check = (ValidateMigrationNames) project.getTasks().getByName(TASK_NAME);
		return List.copyOf(check.getMigrations().getFiles());
	}

	private static List<Dependency> runtimeOnly(Project project) {
		return List
			.copyOf(project.getConfigurations().getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME).getDependencies());
	}

	private static SourceSet mainSourceSet(Project project) {
		return project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
	}

	private Project javaProject() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory).build();
		new FlywayConventions().apply(project);
		project.getPlugins().apply("java");
		return project;
	}

	private Project springBootProject() {
		Project project = javaProject();
		project.getPlugins().apply("org.springframework.boot");
		return project;
	}

}
