package org.v31bank.core.response;

/**
 * One field of a request that failed validation, reported so that a client can
 * mark the offending input rather than showing a single message for the whole
 * form.
 * <p>
 * The rejected value is deliberately absent. Requests here carry account
 * numbers, personal details and payment instructions, and echoing a value back
 * would copy it into every proxy log, browser console and error tracker that
 * records a response.
 *
 * @param field the path of the offending field, using dots for nesting and
 * brackets for collections, for example {@code beneficiary.iban} or
 * {@code lines[0].amount}
 * @param message what is wrong with it, phrased for the person who typed it
 * @author Xander Wang
 * @since 0.2.0
 */
public record FieldViolation(String field, String message) {

}
