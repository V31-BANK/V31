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

package org.v31bank.compliance.domain.constant;

/**
 * What a compliance case is investigating.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum ComplianceCaseType {

	/**
	 * Identity and documentation could not be verified automatically.
	 */
	KYC,

	/**
	 * Activity matched a money laundering pattern.
	 */
	AML,

	/**
	 * A party matched a sanctions or watchlist entry.
	 */
	SANCTIONS,

	/**
	 * Activity suggests the account is not under its owner's control.
	 */
	FRAUD

}
