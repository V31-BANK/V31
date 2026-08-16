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

package org.v31bank.notification;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.client.RestTestClient;

import org.v31bank.notification.infra.persistence.jpa.JpaNotificationTemplateRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the NotificationTemplate endpoints.
 * <p>
 * The whole service is started and driven over HTTP against a real PostgreSQL, so what is
 * exercised is what runs: the controller, the use case, JPA, the schema Flyway built, and
 * the response envelope.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationTemplateApiIntegrationTests {

	private static final String PATH = "/api/v1/notification-templates";

	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private static final UUID ABSENT_ID = UUID.fromString("00000000-0000-7000-8000-000000000000");

	@LocalServerPort
	private int port;

	@Autowired
	private JpaNotificationTemplateRepository records;

	private RestTestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		this.records.deleteAll();
	}

	@Test
	void createsARecordAndSaysWhereItWent() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(request("TPL-0001", "First"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", true).containsEntry("code", "OK");
		assertThat(data(body)).containsEntry("code", "TPL-0001")
			.containsEntry("name", "First")
			.containsEntry("status", "DRAFT");
	}

	/**
	 * The identifier and the audit fields come from the JPA starter rather than from
	 * anything in this service, which is why they are asserted rather than trusted.
	 */
	@Test
	void issuesATimeOrderedIdentifierAndStampsTheAuditFields() {
		Map<String, Object> created = create("TPL-0001", "First");
		assertThat(UUID.fromString((String) created.get("id")).version()).isEqualTo(7);
		assertThat(created.get("createdDate")).asString().endsWith("Z");
		assertThat(created).containsEntry("createdDate", created.get("lastModifiedDate"));
	}

	@Test
	void findsARecordItJustCreated() {
		String id = (String) create("TPL-0001", "First").get("id");
		assertThat(data(get(PATH + "/" + id))).containsEntry("code", "TPL-0001");
	}

	@Test
	void refusesADuplicate() {
		create("TPL-0001", "First");
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(request("TPL-0001", "Duplicate"))
			.exchange()
			.expectStatus()
			.isEqualTo(org.springframework.http.HttpStatus.CONFLICT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", false).containsEntry("code", "CONFLICT");
		assertThat(this.records.count()).isEqualTo(1);
	}

	@Test
	void reportsAnAbsentRecordAsNotFound() {
		Map<String, Object> body = this.client.get()
			.uri(PATH + "/" + ABSENT_ID)
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", false).containsEntry("code", "NOT_FOUND");
	}

	@Test
	void pagesNewestFirstAndCountsThemAll() {
		createMany(25);
		Map<String, Object> page = data(get(PATH + "?pageNumber=1&pageSize=10"));
		assertThat(page).containsEntry("total", 25)
			.containsEntry("pageNumber", 1)
			.containsEntry("pageSize", 10)
			.containsEntry("totalPages", 3)
			.containsEntry("hasNext", true);
		assertThat(records(page)).hasSize(10);
	}

	@Test
	void reportsTheLastPageAsTheLastOne() {
		createMany(25);
		Map<String, Object> page = data(get(PATH + "?pageNumber=3&pageSize=10"));
		assertThat(page).containsEntry("hasNext", false);
		assertThat(records(page)).hasSize(5);
	}

	/**
	 * The failure a page query is most likely to have is not the wrong count but the same
	 * row appearing twice, which only shows up when the pages are put back together.
	 */
	@Test
	void doesNotRepeatOrDropARecordAcrossPages() {
		createMany(25);
		Set<Object> seen = new HashSet<>();
		for (int page = 1; page <= 3; page++) {
			records(data(get(PATH + "?pageNumber=" + page + "&pageSize=10")))
				.forEach((record) -> seen.add(record.get("id")));
		}
		assertThat(seen).hasSize(25);
	}

	@Test
	void clampsAPageSizeNobodyShouldAskFor() {
		createMany(3);
		assertThat(data(get(PATH + "?pageSize=99999"))).containsEntry("pageSize", 500);
	}

	@Test
	void updatesARecord() {
		String id = (String) create("TPL-0001", "First").get("id");
		this.client.put().uri(PATH + "/" + id).body(update("TPL-0001", "Renamed")).exchange().expectStatus().isOk();
		assertThat(data(get(PATH + "/" + id))).containsEntry("name", "Renamed").containsEntry("status", "ACTIVE");
	}

	@Test
	void reportsAnUpdateToAnAbsentRecordAsNotFound() {
		this.client.put()
			.uri(PATH + "/" + ABSENT_ID)
			.body(update("TPL-0001", "Renamed"))
			.exchange()
			.expectStatus()
			.isNotFound();
	}

	@Test
	void deletesARecord() {
		String id = (String) create("TPL-0001", "First").get("id");
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isOk();
		this.client.get().uri(PATH + "/" + id).exchange().expectStatus().isNotFound();
		assertThat(this.records.count()).isZero();
	}

	@Test
	void reportsADeleteOfAnAbsentRecordAsNotFound() {
		this.client.delete().uri(PATH + "/" + ABSENT_ID).exchange().expectStatus().isNotFound();
	}

	@Test
	void refusesAnIdentifierThatIsNotOne() {
		this.client.get().uri(PATH + "/not-a-uuid").exchange().expectStatus().isBadRequest();
	}

	private static Map<String, Object> request(String code, String name) {
		return Map.of("code", code, "name", name, "channel", "EMAIL");
	}

	private static Map<String, Object> update(String code, String name) {
		return Map.of("code", code, "name", name, "channel", "EMAIL", "status", "ACTIVE");
	}

	private Map<String, Object> create(String code, String name) {
		return data(this.client.post()
			.uri(PATH)
			.body(request(code, name))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody());
	}

	private void createMany(int count) {
		for (int i = 1; i <= count; i++) {
			create("TPL-%04d".formatted(i), "Record " + i);
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
			.body(Map.of("code", "TPL-1", "name", TOO_LONG, "channel", "EMAIL"))
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(violations(body)).extracting((violation) -> violation.get("field")).contains("name");
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

	private static final String TOO_LONG = "x".repeat(100 + 1);

}
