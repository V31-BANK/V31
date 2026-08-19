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

package org.v31bank.build.util;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * A project's source sets, which hang off the java extension rather than off the project.
 * <p>
 * Walked a step at a time, each step narrowing what is being asked about:
 *
 * <pre class="code">
 * SourceSets.of(project).main().java().directory()
 * SourceSets.of(project).named("intTest").resources().srcDirs()
 * </pre>
 *
 * Every step answers {@code unwrap()} with the Gradle type it stands for, so anything not
 * covered here is one call away rather than walled off.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class SourceSets {

	/**
	 * The source root a generator owns, kept apart from handwritten code so that the
	 * checks and the formatter can skip it by path.
	 */
	public static final String GENERATED_SOURCES = "src/main/generated/sources";

	private final Project project;

	private SourceSets(Project project) {
		this.project = project;
	}

	/**
	 * Starts from a project.
	 * @param project the project to look at
	 * @return its source sets
	 */
	public static SourceSets of(Project project) {
		return new SourceSets(project);
	}

	public SourceSetContainer unwrap() {
		return this.project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
	}

	public Sources main() {
		return named(SourceSet.MAIN_SOURCE_SET_NAME);
	}

	public Directory generatedSources() {
		return this.project.getLayout().getProjectDirectory().dir(GENERATED_SOURCES);
	}

	public Sources named(String name) {
		return new Sources(unwrap().getByName(name));
	}

	/**
	 * One source set, and the kinds of source it is made of.
	 */
	public static final class Sources {

		private final SourceSet sourceSet;

		private Sources(SourceSet sourceSet) {
			this.sourceSet = sourceSet;
		}

		public SourceSet unwrap() {
			return this.sourceSet;
		}

		public Directories java() {
			return new Directories(this.sourceSet.getJava());
		}

		public Directories resources() {
			return new Directories(this.sourceSet.getResources());
		}

		/**
		 * Every Java that gets compiled: {@link #java()} plus whatever a plugin
		 * contributed.
		 * @return main's {@code allJava}
		 */
		public Directories allJava() {
			return new Directories(this.sourceSet.getAllJava());
		}

		/**
		 * Everything the source set is made of, {@link #allJava()} and
		 * {@link #resources()} together.
		 * @return main's {@code allSource}
		 */
		public Directories allSource() {
			return new Directories(this.sourceSet.getAllSource());
		}

	}

	/**
	 * One kind of source, and where it is rooted.
	 */
	public static final class Directories {

		private final SourceDirectorySet sources;

		private Directories(SourceDirectorySet sources) {
			this.sources = sources;
		}

		public SourceDirectorySet unwrap() {
			return this.sources;
		}

		public Set<File> srcDirs() {
			return this.sources.getSrcDirs();
		}

		/**
		 * The only directory this kind of source is rooted at.
		 * @return that directory
		 * @throws GradleException if it is rooted at none or at several, since a source
		 * set rooted at several — generated code beside handwritten, say — has to be told
		 * which
		 */
		public File directory() {
			return directory((_) -> true);
		}

		/**
		 * The one directory the filter picks out, for a source set rooted at several.
		 * @param filter which of its directories is wanted
		 * @return the one directory the filter leaves
		 * @throws GradleException if the filter leaves none or several
		 */
		public File directory(Predicate<File> filter) {
			List<File> matches = srcDirs().stream().filter(filter).toList();
			if (matches.size() != 1) {
				throw new GradleException(this.sources.getName() + " has " + matches.size()
						+ " source directories to choose from " + matches + ", so none of them is the one meant.");
			}
			return matches.getFirst();
		}

	}

}
