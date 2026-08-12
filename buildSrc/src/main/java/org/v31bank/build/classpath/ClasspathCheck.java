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

package org.v31bank.build.classpath;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;

/**
 * A check on what a configuration resolves to.
 * <p>
 * A starter has no code, so nothing about it can be unit tested. What it does have is a
 * dependency graph, and every way a starter can be wrong is a property of that graph.
 * These checks are the tests: they take a configuration, resolve it, and fail the build
 * on what they find.
 * <p>
 * What is kept is what the configuration resolved to, never the configuration itself. A
 * {@link Configuration} cannot be carried into the execution of a task — the
 * configuration cache cannot serialise one — so {@link #setClasspath} takes it apart at
 * configuration time into the two things a check can actually want: the files, and the
 * root of the resolved graph. Both are lazy, so neither costs anything until a check
 * runs.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class ClasspathCheck extends DefaultTask {

	protected ClasspathCheck() {
		// There is nothing to write, and a task that declares no output is never
		// considered up to date. Saying so explicitly leaves the input alone as the
		// thing that decides.
		getOutputs().upToDateWhen((_) -> true);
	}

	/**
	 * The only input, which is what makes these cheap enough to leave attached to
	 * {@code check}: they are skipped until something about the resolved graph changes.
	 * @return the files the configuration resolved to
	 */
	@Classpath
	public abstract ConfigurableFileCollection getClasspathFiles();

	/**
	 * The resolved graph, for the checks that need to ask what depends on what rather
	 * than which files came out.
	 * <p>
	 * Not an input: the files already are one, and they change whenever the graph does.
	 * @return the root of the resolved graph
	 */
	@Internal
	public abstract Property<ResolvedComponentResult> getRootComponent();

	/**
	 * Takes a configuration apart into the parts a check may keep.
	 * <p>
	 * Not a setter for {@link #getClasspathFiles()}, which is why that is not called
	 * {@code getClasspath}: Gradle refuses to manage a property whose setter takes a
	 * different type than its getter returns.
	 * @param classpath the configuration to check
	 */
	public void setClasspath(Configuration classpath) {
		getClasspathFiles().setFrom(classpath);
		getRootComponent().set(classpath.getIncoming().getResolutionResult().getRootComponent());
	}

}
