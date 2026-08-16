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

package org.v31bank.cbs.infra.persistence.valkey;

import java.util.UUID;

import org.springframework.stereotype.Component;

import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.data.valkey.util.ValkeyKeys;

/**
 * Every key the bank product catalogue occupies, in one place.
 * <p>
 * Valkey has no tables, so the layout is the schema. Written down here it can be read;
 * scattered across the adapter as string concatenation it could only be reconstructed:
 *
 * <pre>
 * v31:cbs:product:&lt;id&gt;                     the product itself, as JSON
 * v31:cbs:product:code:&lt;code&gt;              the code, pointing at the product holding it
 * v31:cbs:product:index                    every product, scored by when it was created
 * v31:cbs:product:index:category:&lt;name&gt;    the same, per category
 * v31:cbs:product:index:status:&lt;name&gt;      the same, per status
 * </pre>
 *
 * The three sorted sets are what make a page cheap. A key-value store cannot offer "the
 * tenth page, newest first" over a keyspace; it can offer a range of a sorted set in
 * logarithmic time, so the ordering is maintained as products are written rather than
 * computed as they are read.
 * <p>
 * Segments go through {@link ValkeyKeys}, which refuses any containing a colon — without
 * that, a product code carrying one would address a key outside this namespace.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Component
public class BankProductValkeyKeys {

	private static final String SERVICE = "cbs";

	private static final String ENTITY = "product";

	private final ValkeyKeys keys;

	public BankProductValkeyKeys(ValkeyKeys keys) {
		this.keys = keys;
	}

	/**
	 * The product itself.
	 * @param id the product identifier
	 * @return the key
	 */
	public String product(UUID id) {
		return this.keys.of(SERVICE, ENTITY, id.toString());
	}

	/**
	 * The claim on a code, holding the identifier of the product that took it.
	 * @param code the product code
	 * @return the key
	 */
	public String code(String code) {
		return this.keys.of(SERVICE, ENTITY, "code", code);
	}

	/**
	 * Every product, newest last.
	 * @return the key
	 */
	public String index() {
		return this.keys.of(SERVICE, ENTITY, "index");
	}

	/**
	 * Every product of one category.
	 * @param category the category
	 * @return the key
	 */
	public String categoryIndex(BankProductCategory category) {
		return this.keys.of(SERVICE, ENTITY, "index", "category", category.name());
	}

	/**
	 * Every product of one status.
	 * @param status the status
	 * @return the key
	 */
	public String statusIndex(BankProductStatus status) {
		return this.keys.of(SERVICE, ENTITY, "index", "status", status.name());
	}

	/**
	 * Where the intersection of two indexes is put while a page is read from it.
	 * @param token something unique to this query
	 * @return the key
	 */
	public String intersection(String token) {
		return this.keys.of(SERVICE, ENTITY, "index", "intersection", token);
	}

}
