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

package org.v31bank.cbs;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the bank product endpoints.
 * <p>
 * Driven over HTTP against a real Valkey. Everything this service does that is worth
 * testing only exists on a real server: a page is a range of a sorted set, a combined
 * filter is an intersection into a temporary key, and the uniqueness of a code rests on
 * {@code SET NX} being atomic. A fake would answer all three without proving any of them.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BankProductApiIntegrationTests {

	private static final String PATH = "/api/v1/bank-products";

	/**
	 * Every endpoint here answers with the same JSON envelope, so the bodies are read
	 * through one type token rather than {@code Map.class}, which would hand back a raw
	 * {@code Map} and force an unchecked conversion at each call site.
	 */
	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private static final UUID ABSENT_ID = UUID.fromString("00000000-0000-7000-8000-000000000000");

	@LocalServerPort
	private int port;

	@Autowired
	private StringRedisTemplate valkey;

	private RestTestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		Set<String> keys = this.valkey.keys("v31it:*");
		if (keys != null && !keys.isEmpty()) {
			this.valkey.delete(keys);
		}
	}

	@Test
	void addsAProductAndSaysWhereItWent() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(request("SAV-BTC-01", "SAVINGS"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", true);
		assertThat(data(body)).containsEntry("code", "SAV-BTC-01")
			.containsEntry("category", "SAVINGS")
			.containsEntry("status", "DRAFT");
	}

	/**
	 * The rate is a {@code BigDecimal} and travels as JSON. A serializer that turned it
	 * into a double would corrupt it silently, which is the whole reason the type was
	 * chosen.
	 */
	@Test
	void keepsTheRateExact() {
		create("SAV-BTC-01", "SAVINGS");
		assertThat(data(get(PATH + "?code=SAV-BTC-01")).get("records")).asInstanceOf(InstanceOfAssertFactories.LIST)
			.first()
			.extracting("interestRate")
			.asString()
			.isEqualTo("0.0275");
	}

	@Test
	void writesTheProductAndItsIndexesWhereTheyCanBeRead() {
		String id = (String) create("SAV-BTC-01", "SAVINGS").get("id");
		assertThat(this.valkey.opsForValue().get("v31it:cbs:product:code:SAV-BTC-01"))
			.as("the claim holds the identifier as plain text, not quoted JSON")
			.isEqualTo(id);
		assertThat(this.valkey.opsForZSet().range("v31it:cbs:product:index", 0, -1)).containsExactly(id);
		assertThat(this.valkey.opsForZSet().range("v31it:cbs:product:index:category:SAVINGS", 0, -1))
			.containsExactly(id);
		assertThat(this.valkey.opsForZSet().range("v31it:cbs:product:index:status:DRAFT", 0, -1)).containsExactly(id);
	}

	@Test
	void refusesADuplicateCode() {
		create("SAV-BTC-01", "SAVINGS");
		this.client.post()
			.uri(PATH)
			.body(request("SAV-BTC-01", "CURRENT"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT);
		assertThat(this.valkey.opsForZSet().zCard("v31it:cbs:product:index")).isEqualTo(1);
	}

	/**
	 * A code becomes part of a key. One containing the separator would address a key
	 * outside this service's namespace, so it is refused — and refused as a bad request
	 * rather than escaping as a server error.
	 */
	@Test
	void refusesACodeThatWouldEscapeTheNamespace() {
		this.client.post()
			.uri(PATH)
			.body(request("EVIL:cbs:product:index", "SAVINGS"))
			.exchange()
			.expectStatus()
			.isBadRequest();
	}

	@Test
	void reportsAnAbsentProductAsNotFound() {
		this.client.get().uri(PATH + "/" + ABSENT_ID).exchange().expectStatus().isNotFound();
	}

	@Test
	void pagesNewestFirstAndCountsThemAll() {
		createMany(25);
		Map<String, Object> page = data(get(PATH + "?pageNumber=1&pageSize=10"));
		assertThat(page).containsEntry("total", 25).containsEntry("totalPages", 3).containsEntry("hasNext", true);
		assertThat(records(page)).hasSize(10);
	}

	@Test
	void doesNotRepeatOrDropAProductAcrossPages() {
		createMany(25);
		Set<Object> seen = new HashSet<>();
		for (int page = 1; page <= 3; page++) {
			records(data(get(PATH + "?pageNumber=" + page + "&pageSize=10")))
				.forEach((record) -> seen.add(record.get("id")));
		}
		assertThat(seen).hasSize(25);
	}

	@Test
	void filtersByCategoryFromItsOwnIndex() {
		create("SAV-1", "SAVINGS");
		create("CUR-1", "CURRENT");
		assertThat(data(get(PATH + "?category=SAVINGS"))).containsEntry("total", 1);
	}

	/**
	 * No single sorted set answers both filters, so the two are intersected into a
	 * temporary key. It has to be cleaned up, or every filtered query leaves one behind.
	 */
	@Test
	void intersectsTwoFiltersAndLeavesNothingBehind() {
		create("SAV-1", "SAVINGS");
		create("CUR-1", "CURRENT");
		assertThat(data(get(PATH + "?category=SAVINGS&status=DRAFT"))).containsEntry("total", 1);
		assertThat(this.valkey.keys("v31it:cbs:product:index:intersection:*")).isEmpty();
	}

	@Test
	void clampsAPageSizeNobodyShouldAskFor() {
		createMany(3);
		assertThat(data(get(PATH + "?pageSize=99999"))).containsEntry("pageSize", 500);
	}

	@Test
	void updatesAProductAndMovesItBetweenIndexes() {
		String id = (String) create("SAV-BTC-01", "SAVINGS").get("id");
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("code", "SAV-BTC-02", "name", "Renamed", "category", "TERM_DEPOSIT", "status", "ACTIVE",
					"interestRate", "0.0275"))
			.exchange()
			.expectStatus()
			.isOk();
		assertThat(this.valkey.hasKey("v31it:cbs:product:code:SAV-BTC-01"))
			.as("the code it gave up has to be free for another product")
			.isFalse();
		assertThat(this.valkey.opsForValue().get("v31it:cbs:product:code:SAV-BTC-02")).isEqualTo(id);
		assertThat(this.valkey.opsForZSet().score("v31it:cbs:product:index:status:DRAFT", id)).isNull();
		assertThat(this.valkey.opsForZSet().score("v31it:cbs:product:index:status:ACTIVE", id)).isNotNull();
	}

	/**
	 * A product that has been offered may have accounts opened against it, and those
	 * accounts refer to it for the terms they earn. Withdrawing it stops new ones;
	 * deleting it would leave the existing accounts describing terms nobody can produce.
	 */
	@Test
	void refusesToDeleteAProductThatHasBeenOffered() {
		String id = (String) create("SAV-BTC-01", "SAVINGS").get("id");
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("code", "SAV-BTC-01", "name", "Live", "category", "SAVINGS", "status", "ACTIVE",
					"interestRate", "0.0275"))
			.exchange()
			.expectStatus()
			.isOk();
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
		assertThat(this.valkey.opsForZSet().zCard("v31it:cbs:product:index")).isEqualTo(1);
	}

	@Test
	void deletesADraftAndLeavesNoKeyBehind() {
		String id = (String) create("SAV-BTC-01", "SAVINGS").get("id");
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isOk();
		assertThat(this.valkey.keys("v31it:cbs:product*")).isEmpty();
	}

	@Test
	void refusesAnIdentifierThatIsNotOne() {
		this.client.get().uri(PATH + "/not-a-uuid").exchange().expectStatus().isBadRequest();
	}

	private static Map<String, Object> request(String code, String category) {
		return Map.of("code", code, "name", "Product " + code, "category", category, "interestRate", "0.0275");
	}

	private Map<String, Object> create(String code, String category) {
		return data(this.client.post()
			.uri(PATH)
			.body(request(code, category))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody());
	}

	private void createMany(int count) {
		for (int i = 1; i <= count; i++) {
			create("PRD-%04d".formatted(i), "SAVINGS");
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
			.body(Map.of("code", "SAV-1", "name", TOO_LONG, "category", "SAVINGS", "interestRate", "0.0275"))
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
