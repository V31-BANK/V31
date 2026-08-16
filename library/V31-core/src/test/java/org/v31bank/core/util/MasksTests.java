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

package org.v31bank.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Masks}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class MasksTests {

	@Test
	void keepsTheLastFourDigitsOfAnAccountNumber() {
		assertThat(Masks.accountNumber("4111111111116789")).isEqualTo("****6789");
		assertThat(Masks.phoneNumber("+41791234567")).isEqualTo("****4567");
	}

	@Test
	void hidesAValueTooShortToMaskSafely() {
		assertThat(Masks.accountNumber("1234567")).isEqualTo("****");
		assertThat(Masks.accountNumber("12")).isEqualTo("****");
		assertThat(Masks.accountNumber("")).isEqualTo("****");
	}

	@Test
	void doesNotDiscloseHowLongTheValueWas() {
		assertThat(Masks.accountNumber("11111111111111116789")).isEqualTo(Masks.accountNumber("11116789"));
		assertThat(Masks.accountNumber("11111111111111116789")).hasSize(8);
	}

	@Test
	void keepsAnEmailRecognisableWithoutHoldingIt() {
		assertThat(Masks.email("xander.wang@v31bank.org")).isEqualTo("x****@v31bank.org");
	}

	@Test
	void hidesAnythingThatIsNotAnEmail() {
		assertThat(Masks.email("not an address")).isEqualTo("****");
		assertThat(Masks.email("@v31bank.org")).isEqualTo("****");
		assertThat(Masks.email("xander@")).isEqualTo("****");
	}

	@Test
	void shortensABlockchainAddressFromBothEnds() {
		assertThat(Masks.cryptoAddress("bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")).isEqualTo("bc1qar****5mdq");
		assertThat(Masks.cryptoAddress("bc1qar")).isEqualTo("****");
	}

	@Test
	void hidesASecretEntirely() {
		assertThat(Masks.secret("sk_live_51H8xQ2eZvKYlo2C")).isEqualTo("****");
	}

	@Test
	void passesNullThroughSoLoggingNeverFails() {
		assertThat(Masks.accountNumber(null)).isNull();
		assertThat(Masks.email(null)).isNull();
		assertThat(Masks.cryptoAddress(null)).isNull();
		assertThat(Masks.secret(null)).isNull();
	}

}
