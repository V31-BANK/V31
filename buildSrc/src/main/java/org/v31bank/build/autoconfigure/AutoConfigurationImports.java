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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;

/**
 * The file a module registers its auto-configurations in.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class AutoConfigurationImports {

	/**
	 * Where Spring Boot looks for the file, relative to the root of a jar or of a
	 * resources directory.
	 */
	public static final String PATH = "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	/**
	 * The file's own name, without the directories it sits in.
	 */
	public static final String FILE_NAME = PATH.substring(PATH.lastIndexOf('/') + 1);

	private static final String COMMENT_START = "#";

	private AutoConfigurationImports() {
	}

	/**
	 * The classes the file registers, in the order it lists them and without the comments
	 * and blank lines Spring Boot's own reader passes over.
	 * @param importsFile the file to read
	 * @return the registered class names
	 */
	public static List<String> read(File importsFile) {
		try {
			return Files.readAllLines(importsFile.toPath())
				.stream()
				.map(AutoConfigurationImports::withoutComment)
				.filter((line) -> !line.isEmpty())
				.toList();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to read " + importsFile, ex);
		}
	}

	private static String withoutComment(String line) {
		int comment = line.indexOf(COMMENT_START);
		return ((comment == -1) ? line : line.substring(0, comment)).trim();
	}

}
