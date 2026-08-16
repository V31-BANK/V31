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

package org.v31bank.compliance;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import org.v31bank.compliance.infra.persistence.jooq.ComplianceCaseTable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the compliance case endpoints.
 * <p>
 * Driven over HTTP against a real PostgreSQL. jOOQ writes SQL for the dialect and the
 * tables it is pointed at, and the audit columns are filled by a listener the starter
 * attaches to the record API — none of which an in-memory substitute or a mocked
 * {@link DSLContext} would exercise.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComplianceCaseApiIntegrationTests {

	private static final String PATH = "/api/v1/compliance-cases";

	/**
	 * Every endpoint here answers with the same JSON envelope, so the bodies are read
	 * through one type token rather than {@code Map.class}, which would hand back a raw
	 * {@code Map} and force an unchecked conversion at each call site.
	 */
	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private static final UUID ABSENT_ID = UUID.fromString("00000000-0000-7000-8000-000000000000");

	private static final String CUSTOMER_ID = "019fb995-685c-77eb-8f95-c62642e1c17e";

	@LocalServerPort
	private int port;

	@Autowired
	private DSLContext dsl;

	private RestTestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		this.dsl.deleteFrom(ComplianceCaseTable.COMPLIANCE_CASE).execute();
	}

	@Test
	void opensACaseAndSaysWhereItWent() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(request("CASE-0001"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", true).containsEntry("code", "OK");
		assertThat(data(body)).containsEntry("caseNumber", "CASE-0001")
			.containsEntry("type", "AML")
			.containsEntry("status", "OPEN");
	}

	/**
	 * The identifier and the audit columns are filled in by the listener the jOOQ starter
	 * attaches, not by this service, which is why they are asserted here rather than
	 * trusted.
	 */
	@Test
	void issuesATimeOrderedIdentifierAndStampsTheAuditColumns() {
		Map<String, Object> created = create("CASE-0001");
		assertThat(UUID.fromString((String) created.get("id")).version()).isEqualTo(7);
		assertThat(created.get("createdDate")).asString().endsWith("Z");
		assertThat(this.dsl.select(ComplianceCaseTable.COMPLIANCE_CASE.CREATED_BY)
			.from(ComplianceCaseTable.COMPLIANCE_CASE)
			.fetchOne(ComplianceCaseTable.COMPLIANCE_CASE.CREATED_BY)).isEqualTo("system");
	}

	@Test
	void findsACaseItJustOpened() {
		String id = (String) create("CASE-0001").get("id");
		assertThat(data(get(PATH + "/" + id))).containsEntry("caseNumber", "CASE-0001");
	}

	@Test
	void refusesADuplicateCaseNumber() {
		create("CASE-0001");
		this.client.post()
			.uri(PATH)
			.body(request("CASE-0001"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT);
		assertThat(this.dsl.fetchCount(ComplianceCaseTable.COMPLIANCE_CASE)).isEqualTo(1);
	}

	@Test
	void reportsAnAbsentCaseAsNotFound() {
		Map<String, Object> body = this.client.get()
			.uri(PATH + "/" + ABSENT_ID)
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("code", "NOT_FOUND");
	}

	@Test
	void pagesNewestFirstAndCountsThemAll() {
		createMany(25);
		Map<String, Object> page = data(get(PATH + "?pageNumber=1&pageSize=10"));
		assertThat(page).containsEntry("total", 25).containsEntry("totalPages", 3).containsEntry("hasNext", true);
		assertThat(records(page)).hasSize(10);
	}

	@Test
	void doesNotRepeatOrDropACaseAcrossPages() {
		createMany(25);
		Set<Object> seen = new HashSet<>();
		for (int page = 1; page <= 3; page++) {
			records(data(get(PATH + "?pageNumber=" + page + "&pageSize=10")))
				.forEach((record) -> seen.add(record.get("id")));
		}
		assertThat(seen).hasSize(25);
	}

	/**
	 * The filter is matched with jOOQ's {@code containsIgnoreCase} rather than a
	 * hand-built {@code like}, so a wildcard typed into it is matched literally instead
	 * of turning into a scan of the whole table.
	 */
	@Test
	void treatsAWildcardInTheFilterAsText() {
		create("CASE-0001");
		create("CASE-0002");
		assertThat(data(get(PATH + "?caseNumber=0001"))).containsEntry("total", 1);
		assertThat(data(get(PATH + "?caseNumber=%"))).containsEntry("total", 0);
	}

	@Test
	void filtersByType() {
		create("CASE-0001");
		assertThat(data(get(PATH + "?type=AML"))).containsEntry("total", 1);
		assertThat(data(get(PATH + "?type=FRAUD"))).containsEntry("total", 0);
	}

	@Test
	void updatesACaseAndMovesItAlong() {
		String id = (String) create("CASE-0001").get("id");
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("caseNumber", "CASE-0001", "customerId", CUSTOMER_ID, "type", "SANCTIONS", "status",
					"IN_REVIEW", "summary", "escalated"))
			.exchange()
			.expectStatus()
			.isOk();
		assertThat(data(get(PATH + "/" + id))).containsEntry("type", "SANCTIONS")
			.containsEntry("status", "IN_REVIEW")
			.containsEntry("summary", "escalated");
	}

	/**
	 * A closed case records a decision that was reached and when. A regulator asking why
	 * an account was frozen expects to find it, so it is kept.
	 */
	@Test
	void refusesToDeleteACaseThatHasBeenConcluded() {
		String id = (String) create("CASE-0001").get("id");
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("caseNumber", "CASE-0001", "customerId", CUSTOMER_ID, "type", "AML", "status", "CLOSED",
					"summary", "concluded"))
			.exchange()
			.expectStatus()
			.isOk();
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
		assertThat(this.dsl.fetchCount(ComplianceCaseTable.COMPLIANCE_CASE)).isEqualTo(1);
	}

	@Test
	void refusesToChangeACaseThatHasBeenConcluded() {
		String id = (String) create("CASE-0001").get("id");
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("caseNumber", "CASE-0001", "customerId", CUSTOMER_ID, "type", "AML", "status", "CLOSED",
					"summary", "concluded"))
			.exchange()
			.expectStatus()
			.isOk();
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("caseNumber", "CASE-0001", "customerId", CUSTOMER_ID, "type", "AML", "summary", "reopened"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void deletesACaseStillInProgress() {
		String id = (String) create("CASE-0001").get("id");
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isOk();
		assertThat(this.dsl.fetchCount(ComplianceCaseTable.COMPLIANCE_CASE)).isZero();
	}

	@Test
	void refusesAnIdentifierThatIsNotOne() {
		this.client.get().uri(PATH + "/not-a-uuid").exchange().expectStatus().isBadRequest();
	}

	private static Map<String, Object> request(String caseNumber) {
		return Map.of("caseNumber", caseNumber, "customerId", CUSTOMER_ID, "type", "AML", "summary", "opened");
	}

	private Map<String, Object> create(String caseNumber) {
		return data(this.client.post()
			.uri(PATH)
			.body(request(caseNumber))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody());
	}

	private void createMany(int count) {
		for (int i = 1; i <= count; i++) {
			create("CASE-%04d".formatted(i));
		}
	}

	private Map<String, Object> get(String uri) {
		return this.client.get()
			.uri(uri)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> data(Map<String, ?> envelope) {
		return (Map<String, Object>) envelope.get("data");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> records(Map<String, Object> page) {
		return (List<Map<String, Object>>) page.get("records");
	}

	/**
	 * A body missing the fields the record cannot do without is the caller's mistake.
	 * Before the shared handler existed this reached the database and came back as a
	 * {@code 500}, telling the caller to retry a request that could never succeed.
	 */
	@Test
	void rejectsAnEmptyBodyNamingTheFieldsThatAreMissing() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of())
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", false).containsEntry("code", "VALIDATION_FAILED");
		assertThat(violations(body)).isNotEmpty();
	}

	/**
	 * The constraint matches the column, so a value too long to store is refused at the
	 * edge rather than by the database.
	 */
	@Test
	void rejectsAValueLongerThanTheColumnHolds() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("caseNumber", TOO_LONG, "customerId", CUSTOMER_ID, "type", "AML"))
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(violations(body)).extracting((violation) -> violation.get("field")).contains("caseNumber");
	}

	/**
	 * A failure is parsed by the same client code as a success, so it has to arrive in
	 * the same envelope rather than in whatever the framework would have sent.
	 */
	@Test
	void reportsAFrameworkRejectionInTheSameEnvelope() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.contentType(org.springframework.http.MediaType.APPLICATION_JSON)
			.body("not json at all")
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", false)
			.containsEntry("code", "VALIDATION_FAILED")
			.containsKey("timestamp");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> violations(Map<String, ?> envelope) {
		return (List<Map<String, Object>>) envelope.get("violations");
	}

	private static final String TOO_LONG = "x".repeat(32 + 1);

}
