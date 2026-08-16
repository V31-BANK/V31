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

/**
 * One field of a request that failed validation, reported so that a client can mark the
 * offending input rather than showing a single message for the whole form.
 * <p>
 * The rejected value is deliberately absent. Requests here carry account numbers,
 * personal details and payment instructions, and echoing a value back would copy it into
 * every proxy log, browser console and error tracker that records a response.
 *
 * @param field the path of the offending field, using dots for nesting and brackets for
 * collections, for example {@code beneficiary.iban} or {@code lines[0].amount}
 * @param message what is wrong with it, phrased for the person who typed it
 * @author Xander Wang
 * @since 0.2.0
 */
public record FieldViolation(String field, String message) {

}
