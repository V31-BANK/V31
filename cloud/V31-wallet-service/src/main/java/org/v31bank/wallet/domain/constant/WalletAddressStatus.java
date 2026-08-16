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

package org.v31bank.wallet.domain.constant;

/**
 * Lifecycle status of a walletAddress.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum WalletAddressStatus {

	/**
	 * Registered, and waiting for a second pair of eyes before anything can be sent to
	 * it.
	 */
	PENDING,

	/**
	 * Approved. Withdrawals may name it.
	 */
	ACTIVE,

	/**
	 * Refused, or withdrawn after the fact. Nothing may be sent to it.
	 */
	BLOCKED

}
