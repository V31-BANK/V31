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

package org.v31bank.core.response;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * A page of results on its way to a caller, using one-based page numbering.
 * <p>
 * This carries no persistence dependency, so a service that pages over a message log, a
 * cache, or another service's API returns the same shape as one paging over a table. It
 * matches the wire contract of {@code org.v31bank.data.jpa.domain.PageResult} member for
 * member, so an endpoint can move onto it without its clients noticing.
 * <p>
 * {@code totalPages} and {@code hasNext} are derived rather than trusted, so a page
 * cannot describe itself inconsistently.
 *
 * @param records the content of this page
 * @param total the number of records matching the query, across all pages
 * @param pageNumber the one-based number of this page
 * @param pageSize the maximum number of records a page holds
 * @param totalPages how many pages the total divides into
 * @param hasNext whether a further page exists
 * @param <T> the type of the page content
 * @author Xander Wang
 * @since 0.2.0
 */
public record PageResponse<T>(List<T> records, long total, int pageNumber, int pageSize, int totalPages,
		boolean hasNext) {

	/**
	 * The number of the first page.
	 */
	public static final int FIRST_PAGE_NUMBER = 1;

	public PageResponse {
		records = (records != null) ? List.copyOf(records) : List.of();
		totalPages = (pageSize == 0) ? 0 : (int) Math.ceilDiv(total, pageSize);
		hasNext = pageNumber < totalPages;
	}

	/**
	 * Create a page from an already paginated list.
	 * @param records the content of the current page
	 * @param total the total number of matching records
	 * @param pageNumber the one-based page number
	 * @param pageSize the page size
	 * @param <T> the type of the page content
	 * @return the page
	 */
	public static <T> PageResponse<T> of(List<T> records, long total, int pageNumber, int pageSize) {
		return new PageResponse<>(records, total, pageNumber, pageSize, 0, false);
	}

	/**
	 * Create an empty page, for a query that matched nothing.
	 * @param pageNumber the one-based page number that was asked for
	 * @param pageSize the page size that was asked for
	 * @param <T> the type of the page content
	 * @return the empty page
	 */
	public static <T> PageResponse<T> empty(int pageNumber, int pageSize) {
		return of(List.of(), 0, pageNumber, pageSize);
	}

	/**
	 * Return a page with each record converted by the given function, keeping the
	 * pagination information — the step that turns a page of domain objects into a page
	 * of response objects.
	 * @param converter the conversion to apply to each record
	 * @param <R> the target record type
	 * @return the converted page
	 */
	public <R> PageResponse<R> map(Function<? super T, ? extends R> converter) {
		Objects.requireNonNull(converter, "converter must not be null");
		return of(this.records.stream().<R>map(converter).toList(), this.total, this.pageNumber, this.pageSize);
	}

}
