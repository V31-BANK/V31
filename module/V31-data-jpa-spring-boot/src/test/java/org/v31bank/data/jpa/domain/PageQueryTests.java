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

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PageQuery}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class PageQueryTests {

	@Test
	void startsAtTheFirstPage() {
		PageQuery query = new PageQuery();
		assertThat(query.getPageNumber()).isEqualTo(PageQuery.FIRST_PAGE_NUMBER);
		assertThat(query.getPageSize()).isEqualTo(PageQuery.DEFAULT_PAGE_SIZE);
	}

	/**
	 * The API counts pages from one and Spring Data counts them from zero. Getting this
	 * wrong returns the second page to a caller who asked for the first.
	 */
	@Test
	void countsFromOneWhileSpringDataCountsFromZero() {
		assertThat(pageable(1, 10).getPageNumber()).isZero();
		assertThat(pageable(3, 10).getPageNumber()).isEqualTo(2);
	}

	@Test
	void treatsAPageBeforeTheFirstAsTheFirst() {
		assertThat(pageable(0, 10).getPageNumber()).isZero();
		assertThat(pageable(-5, 10).getPageNumber()).isZero();
	}

	/**
	 * The page size arrives from the caller, so it is the one number in a listing request
	 * that can be used to ask for the whole table at once.
	 */
	@Test
	void refusesToReturnMoreThanTheMaximumPage() {
		assertThat(pageable(1, PageQuery.MAX_PAGE_SIZE + 1).getPageSize()).isEqualTo(PageQuery.MAX_PAGE_SIZE);
		assertThat(pageable(1, Integer.MAX_VALUE).getPageSize()).isEqualTo(PageQuery.MAX_PAGE_SIZE);
	}

	@Test
	void refusesAPageOfNothing() {
		assertThat(pageable(1, 0).getPageSize()).isEqualTo(1);
		assertThat(pageable(1, -10).getPageSize()).isEqualTo(1);
	}

	@Test
	void carriesTheSortThrough() {
		Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
		assertThat(new PageQuery().toPageable(sort).getSort()).isEqualTo(sort);
		assertThat(new PageQuery().toPageable().getSort()).isEqualTo(Sort.unsorted());
	}

	private static Pageable pageable(int pageNumber, int pageSize) {
		PageQuery query = new PageQuery();
		query.setPageNumber(pageNumber);
		query.setPageSize(pageSize);
		return query.toPageable();
	}

}
