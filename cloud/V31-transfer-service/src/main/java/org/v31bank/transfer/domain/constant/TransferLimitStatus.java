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

package org.v31bank.transfer.domain.constant;

/**
 * Lifecycle status of a transferLimit.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum TransferLimitStatus {

	/**
	 * Enforced on every transfer it applies to.
	 */
	ACTIVE,

	/**
	 * Kept for the record but not enforced. A suspended limit is a deliberate act and
	 * should be short-lived.
	 */
	SUSPENDED

}
