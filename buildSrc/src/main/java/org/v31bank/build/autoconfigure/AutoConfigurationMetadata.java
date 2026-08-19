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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import org.v31bank.build.util.PropertiesFiles;

/**
 * Writes a properties file naming a module and every auto-configuration it offers.
 * <p>
 * Nothing in the module reads this. It is published through a configuration of its own so
 * that something else in this build can collect the same fact from every module at once —
 * a list of what V31 auto-configures, which no single module is in a position to state.
 * What a consumer gets at runtime is a different file with a similar name,
 * {@code META-INF/spring-autoconfigure-metadata.properties}, written into the jar by
 * Spring Boot's auto-configuration processor.
 * <p>
 * Only public classes are listed, because a package-private auto-configuration is Spring
 * Boot's business and nothing outside the module can name it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public abstract class AutoConfigurationMetadata extends AutoConfigurationImportsTask {

	public AutoConfigurationMetadata() {
		getModuleName().convention(getProject().provider(getProject()::getName));
	}

	/**
	 * What the module is called, which is the only way a collected file says where it
	 * came from.
	 * @return the module name
	 */
	@Input
	public abstract Property<String> getModuleName();

	@OutputFile
	public abstract RegularFileProperty getDestination();

	@TaskAction
	void generateMetadata() throws IOException {
		Properties properties = new Properties();
		properties.setProperty("module", getModuleName().get());
		properties.setProperty("autoConfigurationClassNames", String.join(",", publicClassNames()));
		Path destination = getDestination().getAsFile().get().toPath();
		Files.createDirectories(destination.getParent());
		Files.write(destination, PropertiesFiles.render(properties));
	}

	/**
	 * The registered classes that something else could name, in the order the module
	 * registered them.
	 * @return the public class names
	 */
	private List<String> publicClassNames() {
		return loadImports().stream()
			.filter((className) -> AutoConfigurationClass.isPublic(classFileOf(className)))
			.toList();
	}

	private Path classFileOf(String className) {
		return findClassFile(className).orElseThrow(() -> new GradleException(
				"'%s' is registered in %s but was not compiled. Run %s to be told which of the two is wrong."
					.formatted(className, IMPORTS_FILE, AutoConfigurationPlugin.CHECK_IMPORTS_TASK_NAME)));
	}

}
