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

import java.util.Collections;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.FileTree;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Conventions for the services whose schema is owned by Flyway.
 * <p>
 * A migration is checked here because here is the last moment it is still cheap. Flyway
 * records a version the first time it applies one, so by the time a duplicate or a
 * malformed name is noticed in an environment, the fix is a manual repair of
 * {@code flyway_schema_history} in every environment that got there first. Nor does a
 * test catch it, because it is not about what the SQL says — it is about the files as a
 * set, which is what a build can see and a running service cannot.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class FlywayConventions {

	private static final String SPRING_BOOT = "org.springframework.boot";

	private static final String MIGRATIONS = "db/migration/**/*.sql";

	private static final List<String> RUNTIME = List.of("org.springframework.boot:spring-boot-flyway",
			"org.flywaydb:flyway-database-postgresql");

	void apply(Project project) {
		project.getPluginManager().withPlugin(SPRING_BOOT, (_) -> {
			addTheRuntimeAMigrationNeeds(project);
			runAsPartOfCheck(project, registerValidateMigrationNames(project));
		});
	}

	/**
	 * Gives a service that ships migrations what it takes to apply them.
	 * <p>
	 * Added through a provider, so that whether there are any is answered when the
	 * classpath is resolved rather than while the build is being configured. A directory
	 * created after configuration would otherwise be missed until the next sync, and
	 * missed silently — the service would start, find no Flyway, and run against whatever
	 * schema it was pointed at.
	 * @param project the project to configure
	 */
	private void addTheRuntimeAMigrationNeeds(Project project) {
		Provider<List<Dependency>> dependencies = project.provider(() -> migrations(project).isEmpty()
				? Collections.emptyList() : RUNTIME.stream().map(project.getDependencies()::create).toList());
		project.getConfigurations()
			.getByName(JavaPlugin.RUNTIME_ONLY_CONFIGURATION_NAME)
			.getDependencies()
			.addAllLater(dependencies);
	}

	private TaskProvider<ValidateMigrationNames> registerValidateMigrationNames(Project project) {
		return project.getTasks().register("validateMigrationNames", ValidateMigrationNames.class, (task) -> {
			task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
			task.setDescription("Checks migration file names against db/migration/README.md.");
			task.getMigrations().from(migrations(project));
			task.getReport().set(project.getLayout().getBuildDirectory().file("reports/migration-names.txt"));
		});
	}

	private FileTree migrations(Project project) {
		return project.getExtensions()
			.getByType(JavaPluginExtension.class)
			.getSourceSets()
			.getByName(SourceSet.MAIN_SOURCE_SET_NAME)
			.getResources()
			.matching((sql) -> sql.include(MIGRATIONS));
	}

	/**
	 * Hangs the check off {@code processResources} and off {@code check}.
	 * <p>
	 * Both, for different reasons. A migration reaches an environment by being on the
	 * classpath and {@code processResources} is where it gets there, so checking first is
	 * what keeps a broken name out of the jar. And {@code check} is what a developer and
	 * CI run to ask whether anything is wrong — a failure that only ever surfaces while
	 * packaging reads as a packaging problem, when it is a verification one.
	 * @param project the project to configure
	 * @param check the check to attach
	 */
	private void runAsPartOfCheck(Project project, TaskProvider<ValidateMigrationNames> check) {
		project.getTasks()
			.named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME)
			.configure((processResources) -> processResources.dependsOn(check));
		project.getTasks()
			.named(LifecycleBasePlugin.CHECK_TASK_NAME)
			.configure((lifecycle) -> lifecycle.dependsOn(check));
	}

}
