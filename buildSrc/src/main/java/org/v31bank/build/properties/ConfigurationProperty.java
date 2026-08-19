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

import java.util.Map;

/**
 * One property a module lets an application set.
 *
 * @param name the property's name
 * @param type the fully qualified name of its type
 * @param defaultValue the value it has when nothing sets it
 * @param description what it is for, taken from the field's javadoc
 * @param deprecated whether the file carries the older boolean form of the deprecation
 * @param deprecation what the file says about the deprecation
 * @author Xander Wang
 * @since 0.2.0
 */
record ConfigurationProperty(String name, String type, Object defaultValue, String description, boolean deprecated,
		Deprecation deprecation) {

	@SuppressWarnings("unchecked")
	static ConfigurationProperty of(Map<String, Object> json) {
		return new ConfigurationProperty((String) json.get("name"), (String) json.get("type"), json.get("defaultValue"),
				(String) json.get("description"), json.containsKey("deprecated"),
				Deprecation.of((Map<String, Object>) json.get("deprecation")));
	}

	record Deprecation(String reason, String replacement, String since, String level) {

		static Deprecation of(Map<String, Object> json) {
			return (json != null) ? new Deprecation((String) json.get("reason"), (String) json.get("replacement"),
					(String) json.get("since"), (String) json.get("level")) : null;
		}

	}

}
