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

package org.v31bank.build.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPlugin;

import org.v31bank.build.util.SourceSets;

/**
 * Adds Maven's optional dependencies, which Gradle has no equivalent of.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.optional-dependencies")
 * }
 *
 * dependencies {
 *     optional("io.lettuce:lettuce-core")
 * }
 * </pre>
 *
 * The dependency is compiled and tested against here and left out of what a consumer
 * resolves. That is what an auto-configuration needs: it has to see the class it backs
 * off from, while a consumer that never turns the feature on must not be made to download
 * it. {@code compileOnly} would say the first half and lose the second at test time.
 * <p>
 * The configuration is left out of dependency management on purpose: it declares rather
 * than resolves, and every classpath it reaches already carries the platform, so an
 * optional dependency is named without a version like any other.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

	/**
	 * Name of the {@code optional} configuration.
	 */
	public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

	@Override
	public void apply(Project project) {
		ConfigurationContainer configurations = project.getConfigurations();
		Configuration optional = configurations.create(OPTIONAL_CONFIGURATION_NAME, (configuration) -> {
			configuration.setCanBeConsumed(true);
			configuration.setCanBeResolved(false);
		});
		project.getPlugins().withType(JavaPlugin.class, (_) -> SourceSets.of(project).unwrap().all((sourceSet) -> {
			configurations.getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(optional);
			configurations.getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(optional);
		}));
	}

}
