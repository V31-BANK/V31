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

package org.v31bank.build.properties;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The properties described by one or more metadata files, asked about by name.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
final class ConfigurationProperties {

	private final Map<String, ConfigurationProperty> byName;

	private ConfigurationProperties(Map<String, ConfigurationProperty> byName) {
		this.byName = Collections.unmodifiableMap(byName);
	}

	Stream<ConfigurationProperty> stream() {
		return this.byName.values().stream();
	}

	/**
	 * Later files win, so an aggregate of every module still answers one question per
	 * name.
	 * @param files the metadata files to read
	 * @return the result
	 */
	static ConfigurationProperties of(Iterable<File> files) {
		Map<String, ConfigurationProperty> byName = new LinkedHashMap<>();
		for (File file : files) {
			ConfigurationMetadata.of(file).properties().forEach((property) -> byName.put(property.name(), property));
		}
		return new ConfigurationProperties(byName);
	}

}
