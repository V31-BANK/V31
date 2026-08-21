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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import groovy.json.JsonOutput;

/**
 * Configuration metadata files written for a test to read.
 * <p>
 * The entries keep the order they were added in, because two of the checks are about
 * exactly that.
 *
 * @author Xander Wang
 */
final class MetadataFiles {

	private final List<Map<String, Object>> groups = new ArrayList<>();

	private final List<Map<String, Object>> properties = new ArrayList<>();

	private final List<Map<String, Object>> hints = new ArrayList<>();

	private MetadataFiles() {
	}

	static MetadataFiles metadata() {
		return new MetadataFiles();
	}

	MetadataFiles group(String name) {
		this.groups.add(entry("name", name));
		return this;
	}

	MetadataFiles hint(String name) {
		this.hints.add(entry("name", name));
		return this;
	}

	/**
	 * A property with nothing said about it.
	 * @param name the property's name
	 * @return this
	 */
	MetadataFiles undescribed(String name) {
		this.properties.add(entry("name", name, "type", "java.lang.String"));
		return this;
	}

	MetadataFiles property(String name, String description) {
		this.properties.add(entry("name", name, "type", "java.lang.String", "description", description));
		return this;
	}

	MetadataFiles property(String name, String description, Object defaultValue) {
		Map<String, Object> property = entry("name", name, "type", "java.lang.String", "description", description);
		property.put("defaultValue", defaultValue);
		this.properties.add(property);
		return this;
	}

	/**
	 * A deprecated property, whose deprecation may be missing the version it began in.
	 * @param name the property's name
	 * @param replacement what to use instead, or {@code null}
	 * @param since the version it was deprecated in, or {@code null}
	 * @return this
	 */
	MetadataFiles deprecated(String name, String replacement, String since) {
		Map<String, Object> deprecation = new LinkedHashMap<>();
		if (replacement != null) {
			deprecation.put("replacement", replacement);
		}
		if (since != null) {
			deprecation.put("since", since);
		}
		Map<String, Object> property = entry("name", name, "type", "java.lang.String");
		property.put("deprecated", true);
		property.put("deprecation", deprecation);
		this.properties.add(property);
		return this;
	}

	Path writeTo(Path file) {
		Map<String, Object> json = new LinkedHashMap<>();
		json.put("groups", this.groups);
		json.put("properties", this.properties);
		json.put("hints", this.hints);
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, JsonOutput.toJson(json));
			return file;
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to write " + file, ex);
		}
	}

	private static Map<String, Object> entry(String... keysAndValues) {
		Map<String, Object> entry = new LinkedHashMap<>();
		for (int i = 0; i < keysAndValues.length; i += 2) {
			entry.put(keysAndValues[i], keysAndValues[i + 1]);
		}
		return entry;
	}

}
