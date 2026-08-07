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
	}

}
