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

package org.v31bank.build.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import org.v31bank.build.util.PropertiesFiles;

/**
 * Writes a properties file naming a module and the auto-configurations it offers, for
 * something else in this build to collect from every module at once.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class AutoConfigurationMetadata extends DefaultTask {

	public AutoConfigurationMetadata() {
		Project project = getProject();
		getStarterName().convention(project.provider(project::getName));
		getStarterDescription().convention(project.provider(project::getDescription));

		getModuleName().convention(getProject().provider(getProject()::getName));
	}

	@Input
	public abstract Property<String> getModuleName();

	@InputFile
	@PathSensitive(PathSensitivity.RELATIVE)
	public abstract RegularFileProperty getAutoConfigurationImports();

	@Classpath
	public abstract ConfigurableFileCollection getClassesDirectories();

	@OutputFile
	public abstract RegularFileProperty getOutputFile();

	/**
	 * Both inputs come from one source set, and the task waits for it to be built.
	 * @param sourceSet the source set to describe
	 */
	public void setSourceSet(SourceSet sourceSet) {
		getAutoConfigurationImports()
			.set(new File(sourceSet.getOutput().getResourcesDir(), AutoConfigurationImports.PATH));
		getClassesDirectories().from(sourceSet.getOutput().getClassesDirs());
		dependsOn(sourceSet.getOutput());
	}

	@TaskAction
	void documentAutoConfiguration() throws IOException {
		Properties metadata = new Properties();
		metadata.setProperty("module", getModuleName().get());
		metadata.setProperty("autoConfigurationClassNames", String.join(",", publicClassNames()));
		Path outputFile = getOutputFile().getAsFile().get().toPath();
		Files.createDirectories(outputFile.getParent());
		Files.write(outputFile, PropertiesFiles.render(metadata));
	}

	/**
	 * Only public classes: nothing outside the module can name a package-private one.
	 * @return the public class names
	 */
	private Set<String> publicClassNames() {
		Set<String> publicClassNames = new LinkedHashSet<>();
		for (String className : AutoConfigurationImports.read(getAutoConfigurationImports().getAsFile().get())) {
			if (AutoConfigurationClass.isPublic(classFileOf(className))) {
				publicClassNames.add(className);
			}
		}
		return publicClassNames;
	}

	private Path classFileOf(String className) {
		return AutoConfigurationClass.classFileOf(className, getClassesDirectories().getFiles())
			.orElseThrow(
					() -> new IllegalStateException("Auto-configuration class '%s' not found.".formatted(className)));
	}

}
