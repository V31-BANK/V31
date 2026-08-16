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

package org.v31bank.cbs.domain.constant;

/**
 * What kind of account a bank product opens.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum BankProductCategory {

	/**
	 * Balance held on demand, withdrawable at any time.
	 */
	SAVINGS,

	/**
	 * Balance used for day-to-day movement, with no interest.
	 */
	CURRENT,

	/**
	 * Balance committed for a fixed term in exchange for a higher rate.
	 */
	TERM_DEPOSIT,

	/**
	 * A line the customer draws on rather than a balance they hold.
	 */
	CREDIT

}
