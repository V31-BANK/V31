package org.v31bank.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Marks a project as one that is published, and applies everything that follows from
 * that.
 * <p>
 * Declared by the project itself:
 *
 * <pre class="code">
 * plugins {
 *     id("org.v31bank.deployed")
 * }
 * </pre>
 *
 * Applying it is the decision — there is no rule elsewhere inferring which projects
 * publish from where they sit or what they are called. A {@code -service} is an
 * application and simply does not declare it; nor does the internal platform, whose
 * versions reach a consumer through {@code V31-dependencies} instead.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class DeployedPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		new MavenPublishingConventions().apply(project);
	}

}
