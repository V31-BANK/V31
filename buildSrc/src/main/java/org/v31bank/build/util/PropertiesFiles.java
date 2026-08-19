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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Properties rendered as a file that only changes when the properties do.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class PropertiesFiles {

	private PropertiesFiles() {
	}

	/**
	 * {@link Properties#store} writes the current time as a comment and the platform's
	 * line separator, either of which would make unchanged input produce a different
	 * file, and {@link Properties} itself keeps no order. The stream overload is used
	 * because it escapes anything outside Latin-1, so what comes back is ASCII and can be
	 * decoded to drop the comment and sort what is left.
	 * @param properties the properties to render
	 * @return the file's contents
	 * @throws IOException if rendering fails
	 */
	public static byte[] render(Properties properties) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		properties.store(buffer, null);
		String content = buffer.toString(StandardCharsets.ISO_8859_1)
			.lines()
			.filter((line) -> !line.startsWith("#"))
			.sorted()
			.collect(Collectors.joining("\n", "", "\n"));
		return content.getBytes(StandardCharsets.ISO_8859_1);
	}

}
