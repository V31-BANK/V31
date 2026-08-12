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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
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
		ResolvedComponentResult root = getRootComponent().get();
		Set<String> unconstrained = moduleIds(root.getDependencies().stream());
		unconstrained.removeAll(moduleIds(everyEdgeFrom(root).filter(DependencyResult::isConstraint)));
		if (!unconstrained.isEmpty()) {
			throw new GradleException("Found unconstrained direct dependencies: " + unconstrained);
		}
	}

	/**
	 * Every edge in the graph, reached by walking it.
	 * <p>
	 * The resolution result would hand these over directly, but it cannot be carried into
	 * a task's execution — only its root can. Walking from there reaches the same edges;
	 * the visited set is what stops a cycle from doing so forever.
	 * @param root where the graph starts
	 * @return every dependency edge in it
	 */
	private Stream<? extends DependencyResult> everyEdgeFrom(ResolvedComponentResult root) {
		Set<ResolvedComponentResult> seen = new HashSet<>();
		List<DependencyResult> edges = new ArrayList<>();
		Deque<ResolvedComponentResult> queue = new ArrayDeque<>(List.of(root));
		while (!queue.isEmpty()) {
			ResolvedComponentResult component = queue.removeFirst();
			if (!seen.add(component)) {
				continue;
			}
			for (DependencyResult edge : component.getDependencies()) {
				edges.add(edge);
				if (edge instanceof ResolvedDependencyResult resolved) {
					queue.addLast(resolved.getSelected());
				}
			}
		}
		return edges.stream();
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
