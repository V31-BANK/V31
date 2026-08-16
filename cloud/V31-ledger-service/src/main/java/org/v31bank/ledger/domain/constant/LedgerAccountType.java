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

package org.v31bank.ledger.domain.constant;

/**
 * Classification carried by a ledgerAccount.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum LedgerAccountType {

	/**
	 * What the bank owns or is owed. Increases on the debit side.
	 */
	ASSET,

	/**
	 * What the bank owes, including every customer balance. Increases on the credit side.
	 */
	LIABILITY,

	/**
	 * What is left for the owners once liabilities are met.
	 */
	EQUITY,

	/**
	 * What the bank earns — fees, spreads, interest received.
	 */
	REVENUE,

	/**
	 * What the bank spends, including interest paid.
	 */
	EXPENSE

}
