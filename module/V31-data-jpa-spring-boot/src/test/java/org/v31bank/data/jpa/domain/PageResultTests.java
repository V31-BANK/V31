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

package org.v31bank.data.jpa.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PageResult}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class PageResultTests {

	@Test
	void reportsThePageTheCallerAskedFor() {
		PageResult<String> result = PageResult
			.of(new PageImpl<>(List.of("a", "b", "c", "d", "e"), PageRequest.of(2, 10), 25));
		assertThat(result.getPageNumber()).isEqualTo(3);
		assertThat(result.getPageSize()).isEqualTo(10);
		assertThat(result.getTotal()).isEqualTo(25);
		assertThat(result.getRecords()).containsExactly("a", "b", "c", "d", "e");
	}

	/**
	 * A partial last page still counts: 25 records at 10 a page is 3 pages, not 2.
	 */
	@Test
	void countsAPartialLastPage() {
		assertThat(result(25, 1, 10).getTotalPages()).isEqualTo(3);
		assertThat(result(20, 1, 10).getTotalPages()).isEqualTo(2);
		assertThat(result(0, 1, 10).getTotalPages()).isZero();
		assertThat(result(1, 1, 10).getTotalPages()).isEqualTo(1);
	}

	@Test
	void saysWhetherThereIsMoreToFetch() {
		assertThat(result(25, 1, 10).hasNext()).isTrue();
		assertThat(result(25, 3, 10).hasNext()).isFalse();
		assertThat(result(0, 1, 10).hasNext()).isFalse();
	}

	/**
	 * Dividing by it would throw, and a page of nothing is what an empty result of a
	 * badly-formed query looks like.
	 */
	@Test
	void survivesAPageSizeOfZero() {
		assertThat(result(10, 1, 0).getTotalPages()).isZero();
		assertThat(result(10, 1, 0).hasNext()).isFalse();
	}

	@Test
	void convertsTheRecordsAndKeepsTheCount() {
		PageResult<Integer> lengths = result(25, 2, 10).map(String::length);
		assertThat(lengths.getRecords()).containsExactly(1, 1);
		assertThat(lengths.getTotal()).isEqualTo(25);
		assertThat(lengths.getPageNumber()).isEqualTo(2);
		assertThat(lengths.getPageSize()).isEqualTo(10);
	}

	@Test
	void keepsTheQuerysPagingOnAnEmptyResult() {
		PageQuery query = new PageQuery();
		query.setPageNumber(4);
		query.setPageSize(25);
		PageResult<String> empty = PageResult.empty(query);
		assertThat(empty.getRecords()).isEmpty();
		assertThat(empty.getTotal()).isZero();
		assertThat(empty.getPageNumber()).isEqualTo(4);
		assertThat(empty.getPageSize()).isEqualTo(25);
	}

	/**
	 * The list a caller hands over is theirs to keep changing.
	 */
	@Test
	void doesNotShareTheListItWasGiven() {
		List<String> source = new ArrayList<>(List.of("a"));
		PageResult<String> result = PageResult.of(source, 1, 1, 10);
		source.add("b");
		assertThat(result.getRecords()).containsExactly("a");
	}

	private static PageResult<String> result(long total, int pageNumber, int pageSize) {
		return PageResult.of(List.of("a", "b"), total, pageNumber, pageSize);
	}

}
