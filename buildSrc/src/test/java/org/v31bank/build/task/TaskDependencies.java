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

package org.v31bank.build.task;

import java.util.List;
import java.util.stream.StreamSupport;

import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

/**
 * Reads the names out of a task's declared dependencies.
 * <p>
 * {@code dependsOn} hands back whatever it was given, so a dependency arrives as a task,
 * as a provider of one, or as a bare name.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class TaskDependencies {

	private TaskDependencies() {
	}

	public static List<String> namesOf(Iterable<?> dependencies) {
		return StreamSupport.stream(dependencies.spliterator(), false).map(TaskDependencies::nameOf).toList();
	}

	private static String nameOf(Object dependency) {
		Object resolved = (dependency instanceof Provider<?> provider) ? provider.get() : dependency;
		return (resolved instanceof Task task) ? task.getName() : String.valueOf(resolved);
	}

}
