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
 * A starter has no code to unit test. What it has is a dependency graph, and every way a
 * starter can be wrong is a property of that graph. The configuration cache cannot
 * serialise a {@link Configuration}, so {@link #setClasspath} takes one apart at
 * configuration time into the parts a check may keep.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class ClasspathCheck extends DefaultTask {

	protected ClasspathCheck() {
		// A task that declares no output is never up to date, so the input never gets to
		// decide.
		getOutputs().upToDateWhen((_) -> true);
	}

	@Classpath
	public abstract ConfigurableFileCollection getClasspathFiles();

	/**
	 * Not an input: the files already are one, and they change whenever the graph does.
	 * @return the root of the resolved graph
	 */
	@Internal
	public abstract Property<ResolvedComponentResult> getRootComponent();

	/**
	 * Not a setter for {@link #getClasspathFiles()}: Gradle refuses to manage a property
	 * whose setter takes a different type than its getter returns.
	 * @param classpath the configuration to check
	 */
	public void setClasspath(Configuration classpath) {
		getClasspathFiles().setFrom(classpath);
		getRootComponent().set(classpath.getIncoming().getResolutionResult().getRootComponent());
	}

}
