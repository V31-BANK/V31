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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonErrorCode}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class CommonErrorCodeTests {

	@Test
	void reportsItsNameAsTheWireCode() {
		assertThat(CommonErrorCode.NOT_FOUND.code()).isEqualTo("NOT_FOUND");
		assertThat(CommonErrorCode.NOT_FOUND.httpStatus()).isEqualTo(404);
	}

	@Test
	void findsACodeByItsWireForm() {
		assertThat(CommonErrorCode.find("CONFLICT")).contains(CommonErrorCode.CONFLICT);
		assertThat(CommonErrorCode.find("UNPROCESSABLE")).contains(CommonErrorCode.UNPROCESSABLE);
	}

	@Test
	void doesNotFindWhatItDoesNotDeclare() {
		assertThat(CommonErrorCode.find("CATEGORY_HAS_CHILDREN")).isEmpty();
		assertThat(CommonErrorCode.find("conflict")).isEmpty();
		assertThat(CommonErrorCode.find("")).isEmpty();
		assertThat(CommonErrorCode.find(null)).isEmpty();
	}

}
