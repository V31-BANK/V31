package org.v31bank.build.classpath;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.Input;
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

	private final Map<String, Set<String>> exclusionsByDependencyId = new TreeMap<>();

	private final Map<String, Dependency> dependencyById = new HashMap<>();

	private final DependencyHandler dependencies;

	private final ConfigurationContainer configurations;

	private final Dependency platform;

	public CheckClasspathForUnnecessaryExclusions() {
		this.dependencies = getProject().getDependencies();
		this.configurations = getProject().getConfigurations();
		this.platform = this.dependencies.create(
				this.dependencies.platform(this.dependencies.project(Collections.singletonMap("path", DEPENDENCIES))));
	}

	@Override
	public void setClasspath(Configuration classpath) {
		super.setClasspath(classpath);
		this.exclusionsByDependencyId.clear();
		this.dependencyById.clear();
		classpath.getAllDependencies().all(this::processDependency);
	}

	/**
	 * The exclusions themselves, as an input.
	 * <p>
	 * The resolved classpath does not change when an exclusion is added or removed — that
	 * is the very case being looked for — so without this the check would be considered
	 * up to date across the edit that introduces the problem.
	 * @return the exclusions declared by each dependency
	 */
	@Input
	Map<String, Set<String>> getExclusionsByDependencyId() {
		return this.exclusionsByDependencyId;
	}

	private void processDependency(Dependency dependency) {
		if (!(dependency instanceof ModuleDependency moduleDependency)) {
			return;
		}
		String id = getId(moduleDependency);
		Set<String> exclusions = moduleDependency.getExcludeRules()
			.stream()
			.map(this::getId)
			.collect(Collectors.toCollection(TreeSet::new));
		this.exclusionsByDependencyId.put(id, exclusions);
		if (!exclusions.isEmpty()) {
			this.dependencyById.put(id, this.dependencies.create(id));
		}
	}

	@TaskAction
	void checkForUnnecessaryExclusions() {
		Map<String, Set<String>> unnecessary = new TreeMap<>();
		this.exclusionsByDependencyId.forEach((id, exclusions) -> {
			if (exclusions.isEmpty()) {
				return;
			}
			Set<String> remaining = new TreeSet<>(exclusions);
			this.configurations.detachedConfiguration(this.dependencyById.get(id), this.platform)
				.getIncoming()
				.getArtifacts()
				.getArtifacts()
				.stream()
				.map((artifact) -> artifact.getId().getComponentIdentifier())
				.filter(ModuleComponentIdentifier.class::isInstance)
				.map(ModuleComponentIdentifier.class::cast)
				.map(this::getId)
				.forEach(remaining::remove);
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
