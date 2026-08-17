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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Directory operations {@link Files} leaves to the caller.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class Directories {

	private Directories() {
	}

	/**
	 * Delete a directory and everything under it.
	 * <p>
	 * {@link Files#delete} takes only an empty directory, so the tree goes deepest first.
	 * A path that is not there is already in the wanted state. A symbolic link is removed
	 * rather than followed, so it does not take its target with it.
	 * @param root the directory to remove, which need not exist
	 * @throws UncheckedIOException if any part of the tree cannot be read or removed
	 */
	public static void deleteRecursively(Path root) {
		if (root == null || !Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Failed to clear " + root, ex);
		}
	}

}
