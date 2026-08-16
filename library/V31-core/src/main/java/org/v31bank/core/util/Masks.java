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

/**
 * Shortens sensitive values so they can be logged.
 * <p>
 * Logs outlive the request that wrote them and travel further: they are shipped to an
 * aggregator, replicated, retained for years, and read by people who have no business
 * seeing an account number. Anything identifying goes through here first, leaving only
 * enough for a human to confirm they are looking at the right record.
 * <p>
 * The mask is a fixed length rather than one character per hidden character, so the
 * masked form does not disclose how long the original was.
 * <p>
 * A value too short to mask safely is hidden completely: revealing the last four digits
 * of a six digit value gives away most of it.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class Masks {

	/**
	 * What replaces the hidden part of a value.
	 */
	public static final String MASK = "****";

	private static final int MIN_HIDDEN = 4;

	private static final int ADDRESS_PREFIX = 6;

	private static final int ADDRESS_SUFFIX = 4;

	private Masks() {
	}

	/**
	 * Hide all but the last few characters of a value.
	 * @param value the value to mask, which may be {@code null}
	 * @param visible how many trailing characters to keep
	 * @return the masked value, or {@code null} if the value was
	 */
	public static String tail(String value, int visible) {
		if (value == null) {
			return null;
		}
		if (visible < 0) {
			throw new IllegalArgumentException("visible must not be negative");
		}
		if (value.length() < visible + MIN_HIDDEN) {
			return MASK;
		}
		return MASK + value.substring(value.length() - visible);
	}

	/**
	 * Mask an account or card number down to its last four digits, which is what a
	 * customer is shown to confirm which account is meant.
	 * @param value the number to mask, which may be {@code null}
	 * @return the masked number, for example {@code ****6789}
	 */
	public static String accountNumber(String value) {
		return tail(value, 4);
	}

	/**
	 * Mask a phone number down to its last four digits.
	 * @param value the number to mask, which may be {@code null}
	 * @return the masked number
	 */
	public static String phoneNumber(String value) {
		return tail(value, 4);
	}

	/**
	 * Mask an email address, keeping the first character and the domain so that a support
	 * conversation can confirm the address without the log holding it.
	 * @param value the address to mask, which may be {@code null}
	 * @return the masked address, for example {@code x****@example.com}
	 */
	public static String email(String value) {
		if (value == null) {
			return null;
		}
		int at = value.indexOf('@');
		if (at < 1 || at == value.length() - 1) {
			return MASK;
		}
		return value.charAt(0) + MASK + value.substring(at);
	}

	/**
	 * Shorten a blockchain address the way a wallet does, keeping both ends so it can be
	 * recognised at a glance.
	 * <p>
	 * An address is public, so this is for readability rather than secrecy — a full
	 * address is forty odd characters of noise that makes a log line unreadable. It is
	 * not a substitute for the care an address deserves elsewhere: the middle is exactly
	 * where an address-swapping attack hides, so an address a customer is asked to verify
	 * must be shown in full.
	 * @param value the address to shorten, which may be {@code null}
	 * @return the shortened address, for example {@code bc1qar****5mdq}
	 */
	public static String cryptoAddress(String value) {
		if (value == null) {
			return null;
		}
		if (value.length() < ADDRESS_PREFIX + ADDRESS_SUFFIX + MIN_HIDDEN) {
			return MASK;
		}
		return value.substring(0, ADDRESS_PREFIX) + MASK + value.substring(value.length() - ADDRESS_SUFFIX);
	}

	/**
	 * Hide a value completely, for the ones with no readable part at all — an API key, a
	 * token, a private key, a password.
	 * @param value the value to hide, which may be {@code null}
	 * @return the mask, or {@code null} if the value was
	 */
	public static String secret(String value) {
		return (value != null) ? MASK : null;
	}

}
