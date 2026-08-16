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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PageResponse}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class PageResponseTests {

	@Test
	void countsPagesFromTheTotal() {
		PageResponse<String> page = PageResponse.of(List.of("a", "b"), 21, 1, 10);
		assertThat(page.totalPages()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
	}

	@Test
	void reportsNoNextPageOnTheLastOne() {
		PageResponse<String> page = PageResponse.of(List.of("a"), 21, 3, 10);
		assertThat(page.totalPages()).isEqualTo(3);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void recomputesDerivedMembersRatherThanTrustingThem() {
		PageResponse<String> page = new PageResponse<>(List.of("a"), 21, 1, 10, 99, false);
		assertThat(page.totalPages()).isEqualTo(3);
		assertThat(page.hasNext()).isTrue();
	}

	@Test
	void survivesAPageSizeOfZero() {
		PageResponse<String> page = PageResponse.of(List.of(), 0, 1, 0);
		assertThat(page.totalPages()).isZero();
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void emptyKeepsThePageThatWasAskedFor() {
		PageResponse<String> page = PageResponse.empty(2, 25);
		assertThat(page.records()).isEmpty();
		assertThat(page.total()).isZero();
		assertThat(page.pageNumber()).isEqualTo(2);
		assertThat(page.pageSize()).isEqualTo(25);
		assertThat(page.hasNext()).isFalse();
	}

	@Test
	void mapConvertsTheRecordsAndKeepsThePagination() {
		PageResponse<Integer> lengths = PageResponse.of(List.of("aa", "bbb"), 21, 1, 10).map(String::length);
		assertThat(lengths.records()).containsExactly(2, 3);
		assertThat(lengths.total()).isEqualTo(21);
		assertThat(lengths.totalPages()).isEqualTo(3);
		assertThat(lengths.hasNext()).isTrue();
	}

	@Test
	void recordsAreCopiedAndUnmodifiable() {
		List<String> records = new ArrayList<>(List.of("a"));
		PageResponse<String> page = PageResponse.of(records, 1, 1, 10);
		records.clear();
		assertThat(page.records()).containsExactly("a");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> page.records().add("b"));
	}

	@Test
	void treatsMissingRecordsAsAnEmptyPage() {
		assertThat(new PageResponse<>(null, 0, 1, 10, 0, false).records()).isEmpty();
	}

}
