package org.v31bank.build.classpath;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.tasks.TaskAction;

/**
 * Fails when a starter declares a dependency whose version nothing constrains.
 * <p>
 * A starter's whole promise is that naming it settles a set of versions that were built
 * and tested together. A direct dependency that no platform has an opinion about breaks
 * that quietly: the build resolves <em>something</em>, so nothing fails here, and which
 * version a consumer ends up with depends on the rest of their graph.
 * <p>
 * Constrained means some platform speaks for it — {@code V31-internal-dependencies} for
 * third-party libraries, {@code V31-dependencies} for V31's own artifacts. Where the
 * constraint comes from is not checked, only that one exists.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckClasspathForUnconstrainedDirectDependencies extends ClasspathCheck {

	@TaskAction
	void checkForUnconstrainedDirectDependencies() {
		ResolutionResult resolution = getConfiguration().getIncoming().getResolutionResult();
		Set<String> unconstrained = moduleIds(resolution.getRoot().getDependencies().stream());
		unconstrained
			.removeAll(moduleIds(resolution.getAllDependencies().stream().filter(DependencyResult::isConstraint)));
		if (!unconstrained.isEmpty()) {
			throw new GradleException("Found unconstrained direct dependencies: " + unconstrained);
		}
	}

	/**
	 * Reduces dependency edges to {@code group:module}.
	 * <p>
	 * Project dependencies fall out along the way: they are selectors of another kind and
	 * carry this build's version by construction, so there is nothing for a platform to
	 * constrain.
	 * @param dependencies the edges to reduce
	 * @return their module identifiers
	 */
	private Set<String> moduleIds(Stream<? extends DependencyResult> dependencies) {
		return dependencies.map(DependencyResult::getRequested)
			.filter(ModuleComponentSelector.class::isInstance)
			.map(ModuleComponentSelector.class::cast)
			.map((selector) -> selector.getGroup() + ":" + selector.getModule())
			.collect(Collectors.toCollection(TreeSet::new));
	}

}
