package org.v31bank.build.test;

import java.io.File;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.gradle.plugins.ide.idea.model.IdeaModel;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrationTestPlugin}.
 * <p>
 * The plugin reacts to the java plugin rather than requiring it first, because the root
 * build applies conventions before a project's own build file has run. Each test
 * therefore applies this plugin first and {@code java} second, which is the order that
 * actually occurs.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class IntegrationTestPluginTests {

	@TempDir
	private File directory;

	@org.junit.jupiter.api.Test
	void addsNothingToAProjectThatCompilesNoJava() {
		Project project = project();
		assertThat(project.getTasks().findByName(IntegrationTestPlugin.INT_TEST_TASK_NAME)).isNull();
	}

	@org.junit.jupiter.api.Test
	void addsTheSourceSetWhenTheJavaPluginArrivesAfterwards() {
		Project project = javaProject();
		assertThat(sourceSets(project).findByName(IntegrationTestPlugin.INT_TEST_SOURCE_SET_NAME)).isNotNull();
	}

	/**
	 * Declared, not created: a source directory that does not exist is simply empty.
	 */
	@org.junit.jupiter.api.Test
	void looksForSourcesUnderTheSourceSetsOwnDirectory() {
		SourceSet intTest = intTestSourceSet(javaProject());
		assertThat(intTest.getJava().getSrcDirs()).singleElement()
				.satisfies((dir) -> assertThat(dir).hasName("java").hasParent(new File(this.directory, "src/intTest")));
		assertThat(intTest.getResources().getSrcDirs()).singleElement()
				.satisfies((dir) -> assertThat(dir).hasName("resources"));
	}

	/**
	 * Without this an IDE imports the source set as production code, and the analysers
	 * that run there report every assertion in it as an assertion in production code.
	 */
	@org.junit.jupiter.api.Test
	void tellsTheIdeTheSourcesAreTests() {
		Project project = javaProject();
		SourceSet intTest = intTestSourceSet(project);
		IdeaModule module = project.getExtensions().getByType(IdeaModel.class).getModule();
		assertThat(module.getTestSources().getFiles()).containsAll(intTest.getJava().getSrcDirs());
		assertThat(module.getTestResources().getFiles()).containsAll(intTest.getResources().getSrcDirs());
	}

	/**
	 * A new source set does not see {@code main} the way {@code test} does, so it has to
	 * be given it.
	 */
	@org.junit.jupiter.api.Test
	void compilesAgainstTheProjectsOwnClasses() {
		Project project = javaProject();
		SourceSet main = sourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
		assertThat(intTestSourceSet(project).getCompileClasspath().getFiles()).containsAll(main.getOutput().getFiles());
	}

	@org.junit.jupiter.api.Test
	void registersATaskThatRunsTheSourceSet() {
		Project project = javaProject();
		Test task = intTestTask(project);
		SourceSet intTest = intTestSourceSet(project);
		assertThat(task.getTestClassesDirs().getFiles()).isEqualTo(intTest.getOutput().getClassesDirs().getFiles());
		assertThat(task.getClasspath()).isSameAs(intTest.getRuntimeClasspath());
	}

	@org.junit.jupiter.api.Test
	void presentsTheTaskAsAVerificationTask() {
		Test task = intTestTask(javaProject());
		assertThat(task.getGroup()).isEqualTo(LifecycleBasePlugin.VERIFICATION_GROUP);
		assertThat(task.getDescription()).isEqualTo("Runs integration tests.");
	}

	/**
	 * Otherwise it is a task somebody has to remember, which is the same as one that
	 * never runs.
	 */
	@org.junit.jupiter.api.Test
	void runsAsPartOfCheck() {
		Project project = javaProject();
		assertThat(taskNames(project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).getDependsOn()))
				.contains(IntegrationTestPlugin.INT_TEST_TASK_NAME);
	}

	/**
	 * Ordered after the unit tests rather than depending on them: these are the slow
	 * ones, so the cheap failure should surface first — but asking for them alone must
	 * not drag the unit tests along.
	 */
	@org.junit.jupiter.api.Test
	void yieldsToTheUnitTestsWithoutRequiringThem() {
		Test task = intTestTask(javaProject());
		assertThat(taskNames(task.getShouldRunAfter().getDependencies(task)))
				.contains(org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME);
		assertThat(taskNames(task.getDependsOn())).doesNotContain(org.gradle.api.plugins.JavaPlugin.TEST_TASK_NAME);
	}

	/**
	 * Gradle discovers nothing without it, and reports that as "no tests found" rather
	 * than as a missing dependency. The conventions add it to {@code test} only, which is
	 * a different source set with a configuration of its own.
	 */
	@org.junit.jupiter.api.Test
	void putsTheLauncherOnTheRuntimeClasspath() {
		Project project = javaProject();
		String configuration = intTestSourceSet(project).getRuntimeOnlyConfigurationName();
		assertThat(project.getConfigurations().getByName(configuration).getDependencies()).anySatisfy((dependency) -> {
			assertThat(dependency.getGroup()).isEqualTo("org.junit.platform");
			assertThat(dependency.getName()).isEqualTo("junit-platform-launcher");
		});
	}

	/**
	 * A task's declared dependencies hold whatever was passed to {@code dependsOn} — a
	 * task, or a provider of one — so both shapes have to be unwrapped.
	 * @param tasks the declared dependencies
	 * @return their names
	 */
	private static List<String> taskNames(Iterable<?> tasks) {
		return java.util.stream.StreamSupport.stream(tasks.spliterator(), false).map((task) -> {
			Object resolved = (task instanceof org.gradle.api.provider.Provider<?> provider) ? provider.get() : task;
			return (resolved instanceof org.gradle.api.Task named) ? named.getName() : String.valueOf(resolved);
		}).toList();
	}

	private static org.gradle.api.tasks.SourceSetContainer sourceSets(Project project) {
		return project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
	}

	private static SourceSet intTestSourceSet(Project project) {
		return sourceSets(project).getByName(IntegrationTestPlugin.INT_TEST_SOURCE_SET_NAME);
	}

	private static Test intTestTask(Project project) {
		return (Test) project.getTasks().getByName(IntegrationTestPlugin.INT_TEST_TASK_NAME);
	}

	private Project project() {
		Project project = ProjectBuilder.builder().withProjectDir(this.directory).build();
		project.getPlugins().apply(IntegrationTestPlugin.class);
		return project;
	}

	private Project javaProject() {
		Project project = project();
		project.getPlugins().apply("java");
		return project;
	}

}
