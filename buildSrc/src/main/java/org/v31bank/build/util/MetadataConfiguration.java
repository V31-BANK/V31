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

package org.v31bank.build.util;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.model.ObjectFactory;

/**
 * The configuration through which a project offers a file describing itself, for
 * something else in the build to collect from every project at once.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class MetadataConfiguration {

	private MetadataConfiguration() {
	}

	/**
	 * The attributes are what let a consumer collecting from many projects select the
	 * description rather than the jar.
	 * @param project the project to configure
	 * @param name what to call the configuration
	 * @param usage what a consumer asks for it by
	 * @return the configuration to publish into
	 */
	public static Configuration create(Project project, String name, String usage) {
		return project.getConfigurations().create(name, (configuration) -> {
			configuration.setCanBeConsumed(true);
			configuration.setCanBeResolved(false);
			ObjectFactory objects = project.getObjects();
			configuration.attributes((attributes) -> {
				attributes.attribute(Category.CATEGORY_ATTRIBUTE,
						objects.named(Category.class, Category.DOCUMENTATION));
				attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, usage));
			});
		});
	}

}
