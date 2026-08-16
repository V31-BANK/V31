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

package org.v31bank.jooq.audit;

/**
 * Names of the columns {@link AuditRecordListener} maintains.
 * <p>
 * jOOQ works from generated classes rather than a mapped superclass, so there is no
 * {@code BaseEntity} to inherit these from and nothing forces a table to carry them. The
 * listener therefore stamps whichever of them a record happens to declare, and skips the
 * rest.
 * <p>
 * The names match the columns {@code org.v31bank.data.jpa.domain.BaseEntity} maps, so a
 * table can be reached through either mapper and read the same way. Changing one of these
 * means changing every table already carrying it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class AuditColumns {

	/**
	 * Primary key, a time-ordered UUIDv7.
	 */
	public static final String ID = "id";

	/**
	 * Who inserted the row.
	 */
	public static final String CREATED_BY = "created_by";

	/**
	 * When the row was inserted, as an instant.
	 */
	public static final String CREATED_DATE = "created_date";

	/**
	 * Who last changed the row.
	 */
	public static final String LAST_MODIFIED_BY = "last_modified_by";

	/**
	 * When the row was last changed, as an instant.
	 */
	public static final String LAST_MODIFIED_DATE = "last_modified_date";

	private AuditColumns() {
	}

}
