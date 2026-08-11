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

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Applies the conventions every V31 project is held to.
 * <p>
 * Applied once from the root build to every subproject. Each set of conventions decides
 * for itself whether it has anything to do, by reacting to the plugins the project
 * actually has — so an aggregator with no code, a {@code java-platform}, and a Spring
 * Boot application can all be given the same treatment without any of them being
 * special-cased here.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		new JavaConventions().apply(project);
		new FlywayConventions().apply(project);
	}

}
