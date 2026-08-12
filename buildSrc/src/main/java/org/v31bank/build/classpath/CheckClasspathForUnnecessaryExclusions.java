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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;

/**
 * Fails when a dependency excludes something it never brings in.
 * <p>
 * An exclusion is a claim about someone else's dependency graph, and that graph moves
 * under it: the upstream release that made the exclusion necessary is followed by one
 * that drops the dependency outright. The exclusion then does nothing, but it still reads
 * as a live decision, and the next person maintaining this file has to work out whether
 * removing it is safe. Nothing else notices, because an exclusion that matches nothing is
 * silent by construction.
 * <p>
 * Each dependency that declares exclusions is resolved on its own against
 * {@code V31-dependencies}, and whatever actually comes back accounts for the exclusions
 * that were doing something. What is left over is not.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class CheckClasspathForUnnecessaryExclusions extends ClasspathCheck {

	/**
	 * The platform the standalone resolutions are done against, so that a dependency
	 * named without a version can be resolved at all.
	 * <p>
	 * The published platform rather than the internal one. Everything reached from a
	 * starter is something a consumer reaches too, and a consumer has only this one — so
	 * a dependency that resolves against the internal platform and not against this one
	 * is a starter nobody outside this build can use. Resolving here is what makes that
	 * fail rather than pass quietly.
	 */
	private static final String DEPENDENCIES = ":platform:V31-dependencies";

	/**
	 * The exclusions themselves, as an input.
	 * <p>
	 * The resolved classpath does not change when an exclusion is added or removed — that
	 * is the very case being looked for — so without this the check would be considered
	 * up to date across the edit that introduces the problem.
	 * @return the exclusions declared by each dependency
	 */
	@Input
	public abstract MapProperty<String, Set<String>> getExclusionsByDependencyId();

	/**
	 * What each of those dependencies actually brings in, resolved on its own.
	 * <p>
	 * Filled in with providers rather than values, so the standalone resolutions happen
	 * when this check runs and not while every build is being configured. Not an input:
	 * it is the answer being checked, and it is derived from the exclusions that already
	 * are one.
	 * @return the modules each dependency resolves to
	 */
	@Internal
	public abstract MapProperty<String, Set<String>> getResolvedByDependencyId();

	/**
	 * Reads the exclusions off the classpath, and arranges for what each of those
	 * dependencies resolves to on its own.
	 * <p>
	 * Both happen here, at configuration time, because both need the
	 * {@link Configuration} and the project behind it — and neither may be carried into
	 * the task's execution. What the execution gets is two maps of plain strings.
	 * @param classpath the configuration to check
	 */
	@Override
	public void setClasspath(Configuration classpath) {
		super.setClasspath(classpath);
		DependencyHandler dependencies = getProject().getDependencies();
		Dependency platform = dependencies
			.create(dependencies.platform(dependencies.project(Collections.singletonMap("path", DEPENDENCIES))));
		classpath.getAllDependencies().all((dependency) -> {
			if (!(dependency instanceof ModuleDependency moduleDependency)) {
				return;
			}
			String id = getId(moduleDependency);
			Set<String> exclusions = moduleDependency.getExcludeRules()
				.stream()
				.map(this::getId)
				.collect(Collectors.toCollection(TreeSet::new));
			getExclusionsByDependencyId().put(id, exclusions);
			if (!exclusions.isEmpty()) {
				getResolvedByDependencyId().put(id, resolveOnItsOwn(dependencies.create(id), platform));
			}
		});
	}

	/**
	 * What one dependency brings in when nothing else is on the classpath.
	 * @param dependency the dependency to resolve
	 * @param platform the versions to resolve it against
	 * @return the modules it resolves to, worked out when asked for
	 */
	private Provider<Set<String>> resolveOnItsOwn(Dependency dependency, Dependency platform) {
		return getProject().getConfigurations()
			.detachedConfiguration(dependency, platform)
			.getIncoming()
			.getArtifacts()
			.getResolvedArtifacts()
			.map((artifacts) -> artifacts.stream()
				.map(ResolvedArtifactResult::getId)
				.map(ComponentArtifactIdentifier::getComponentIdentifier)
				.filter(ModuleComponentIdentifier.class::isInstance)
				.map(ModuleComponentIdentifier.class::cast)
				.map(this::getId)
				.collect(Collectors.toCollection(TreeSet::new)));
	}

	@TaskAction
	void checkForUnnecessaryExclusions() {
		Map<String, Set<String>> unnecessary = new TreeMap<>();
		Map<String, Set<String>> resolved = getResolvedByDependencyId().get();
		getExclusionsByDependencyId().get().forEach((id, exclusions) -> {
			if (exclusions.isEmpty()) {
				return;
			}
			Set<String> remaining = new TreeSet<>(exclusions);
			remaining.removeAll(resolved.getOrDefault(id, Set.of()));
			if (!remaining.isEmpty()) {
				unnecessary.put(id, remaining);
			}
		});
		if (!unnecessary.isEmpty()) {
			throw new GradleException(getExceptionMessage(unnecessary));
		}
	}

	private String getExceptionMessage(Map<String, Set<String>> unnecessary) {
		StringBuilder message = new StringBuilder("Unnecessary exclusions detected:");
		for (Entry<String, Set<String>> entry : unnecessary.entrySet()) {
			message.append(String.format("%n    %s", entry.getKey()));
			for (String exclusion : entry.getValue()) {
				message.append(String.format("%n        %s", exclusion));
			}
		}
		return message.toString();
	}

	private String getId(ModuleComponentIdentifier identifier) {
		return identifier.getGroup() + ":" + identifier.getModule();
	}

	private String getId(ModuleDependency dependency) {
		return dependency.getGroup() + ":" + dependency.getName();
	}

	private String getId(ExcludeRule rule) {
		return rule.getGroup() + ":" + rule.getModule();
	}

}
