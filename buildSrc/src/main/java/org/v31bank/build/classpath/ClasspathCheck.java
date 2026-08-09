package org.v31bank.build.classpath;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
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
 * The classpath is the only input, so declaring it as one is what makes these cheap
 * enough to leave attached to {@code check} — they are skipped entirely until something
 * about the resolved graph changes.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class ClasspathCheck extends DefaultTask {

	private Configuration classpath;

	protected ClasspathCheck() {
		// There is nothing to write, and a task that declares no output is never
		// considered up to date. Saying so explicitly leaves the input alone as the
		// thing that decides.
		getOutputs().upToDateWhen((task) -> true);
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	public void setClasspath(Configuration classpath) {
		this.classpath = classpath;
	}

	/**
	 * The classpath as the configuration it came from, for the checks that need to ask
	 * about the graph rather than the files it resolved to.
	 * @return the configuration
	 */
	@Internal
	protected Configuration getConfiguration() {
		return this.classpath;
	}

}
