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

package org.v31bank.data.valkey.util;

import java.util.Objects;
import java.util.StringJoiner;

/**
 * Builds the keys this application writes, all under one prefix.
 * <p>
 * Valkey has no schemas and no tables: everything one instance holds shares a single flat
 * keyspace. Several services pointed at the same instance will collide the moment two of
 * them cache something they both call {@code
 * customer:1}. The prefix is what keeps them apart, and putting it in one place is what
 * stops it being remembered in some call sites and forgotten in others.
 * <p>
 * A segment may not contain the separator. That is not tidiness: a segment is often built
 * from something a caller supplied, and one containing a colon would let it address a key
 * outside the namespace it was given — a customer identified as {@code 7:session:admin}
 * reaching a key it has no business reading.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ValkeyKeys {

	/**
	 * What separates the parts of a key. Colon by convention, and what every Valkey and
	 * Redis browser expects when it renders keys as a tree.
	 */
	public static final String SEPARATOR = ":";

	private final String prefix;

	/**
	 * Create a builder that puts everything under the given prefix.
	 * @param prefix the prefix every key begins with, typically the service name
	 */
	public ValkeyKeys(String prefix) {
		this.prefix = requireSegment(prefix, "prefix");
	}

	/**
	 * Build a key under this application's prefix.
	 * <p>
	 * {@code keys.of("customer", id)} gives {@code v31:customer:0199-7a}.
	 * @param segments the parts of the key, in order, none of them empty or containing
	 * the separator
	 * @return the key
	 */
	public String of(String... segments) {
		Objects.requireNonNull(segments, "segments must not be null");
		if (segments.length == 0) {
			throw new IllegalArgumentException("A key needs at least one segment beyond the prefix");
		}
		StringJoiner key = new StringJoiner(SEPARATOR);
		key.add(this.prefix);
		for (String segment : segments) {
			key.add(requireSegment(segment, "segment"));
		}
		return key.toString();
	}

	/**
	 * Return the pattern matching every key under a namespace, for the rare operation
	 * that has to sweep one.
	 * <p>
	 * Use it with {@code SCAN}, never with {@code KEYS}: the latter walks the whole
	 * keyspace in one blocking call, and on a production instance that is an outage.
	 * @param segments the namespace to match under
	 * @return the pattern, ending in {@code :*}
	 */
	public String patternUnder(String... segments) {
		return of(segments) + SEPARATOR + "*";
	}

	/**
	 * Return the prefix every key from this builder begins with.
	 * @return the prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}

	private static String requireSegment(String segment, String name) {
		Objects.requireNonNull(segment, name + " must not be null");
		if (segment.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		if (segment.contains(SEPARATOR)) {
			throw new IllegalArgumentException(
					"A key " + name + " must not contain '" + SEPARATOR + "', but was '" + segment + "'");
		}
		return segment;
	}

}
