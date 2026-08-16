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

package org.v31bank.customer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the customer category endpoints.
 * <p>
 * The whole service is started and driven over HTTP against a real PostgreSQL, so what is
 * exercised is what runs: the controller, the use case, JPA, the self-referencing foreign
 * key Flyway built, and the response envelope.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CustomerCategoryApiIntegrationTests {

	private static final String PATH = "/api/v1/customer-categories";

	private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {
	};

	private static final UUID ABSENT_ID = UUID.fromString("00000000-0000-7000-8000-000000000000");

	@LocalServerPort
	private int port;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private RestTestClient client;

	@BeforeEach
	void setUp() {
		this.client = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		// One statement rather than a row-at-a-time delete: the parent column
		// references this same table, so deleting a parent before its children
		// would trip the foreign key. Expressed in JPQL so it reaches the schema
		// the service is mapped to rather than whatever the connection defaults to.
		new TransactionTemplate(this.transactionManager).executeWithoutResult(
				(status) -> this.entityManager.createQuery("delete from CustomerCategory").executeUpdate());
	}

	@Test
	void createsACategoryAndSaysWhereItWent() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "RETAIL", "name", "Retail"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", true).containsEntry("code", "OK");
		assertThat(data(body)).containsEntry("code", "RETAIL")
			.containsEntry("name", "Retail")
			.containsEntry("status", "ENABLED");
		assertThat(UUID.fromString((String) data(body).get("id")).version()).isEqualTo(7);
	}

	@Test
	void findsACategoryItJustCreated() {
		String id = create("RETAIL", "Retail", null);
		assertThat(data(get(PATH + "/" + id))).containsEntry("code", "RETAIL");
	}

	@Test
	void refusesADuplicateCode() {
		create("RETAIL", "Retail", null);
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "RETAIL", "name", "Retail again"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("success", false).containsEntry("code", "CONFLICT");
	}

	/**
	 * The parent is a foreign key. Left to the database this surfaces as a constraint
	 * violation; the caller is told which parent was not found instead.
	 */
	@Test
	void refusesAParentThatDoesNotExist() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "SUB", "name", "Sub", "parentId", ABSENT_ID.toString()))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("code", "UNPROCESSABLE");
		assertThat((String) body.get("message")).contains(ABSENT_ID.toString());
	}

	@Test
	void buildsTheTreeFromTheFlatRows() {
		String root = create("RETAIL", "Retail", null);
		String child = create("RETAIL_PREMIUM", "Premium", root);
		create("RETAIL_PREMIUM_GOLD", "Gold", child);

		List<Map<String, Object>> tree = treeData(get(PATH + "/tree"));

		assertThat(tree).hasSize(1);
		assertThat(tree.get(0)).containsEntry("code", "RETAIL");
		List<Map<String, Object>> children = children(tree.get(0));
		assertThat(children).hasSize(1);
		assertThat(children.get(0)).containsEntry("code", "RETAIL_PREMIUM");
		assertThat(children(children.get(0))).hasSize(1)
			.first()
			.satisfies((gold) -> assertThat(gold).containsEntry("code", "RETAIL_PREMIUM_GOLD"));
	}

	@Test
	void ordersSiblingsByTheirPosition() {
		String root = create("RETAIL", "Retail", null);
		createWithOrder("B", "Second", root, 20);
		createWithOrder("A", "First", root, 10);

		List<Map<String, Object>> children = children(treeData(get(PATH + "/tree")).get(0));

		assertThat(children).extracting((child) -> child.get("code")).containsExactly("A", "B");
	}

	@Test
	void listsAPageOfCategories() {
		for (int i = 1; i <= 3; i++) {
			create("CAT" + i, "Category " + i, null);
		}
		Map<String, Object> page = (Map<String, Object>) data(get(PATH + "?pageNumber=1&pageSize=2"));
		assertThat(page).containsEntry("total", 3).containsEntry("pageSize", 2).containsEntry("hasNext", true);
		assertThat((List<?>) page.get("records")).hasSize(2);
	}

	@Test
	void updatesACategory() {
		String id = create("RETAIL", "Retail", null);
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("code", "RETAIL", "name", "Retail Banking", "status", "DISABLED"))
			.exchange()
			.expectStatus()
			.isOk();
		assertThat(data(get(PATH + "/" + id))).containsEntry("name", "Retail Banking")
			.containsEntry("status", "DISABLED");
	}

	/**
	 * The one rule in this service that cannot be enforced by a constraint. A node moved
	 * under its own descendant makes a ring: the branch drops out of every tree query and
	 * a walk up the parents never terminates.
	 */
	@Test
	void refusesToMoveACategoryUnderItsOwnDescendant() {
		String root = create("RETAIL", "Retail", null);
		String child = create("PREMIUM", "Premium", root);

		Map<String, Object> body = this.client.put()
			.uri(PATH + "/" + root)
			.body(Map.of("code", "RETAIL", "name", "Retail", "parentId", child))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();

		assertThat(body).containsEntry("code", "CONFLICT");
		// Omitted rather than null: the service is configured to leave absent
		// fields out, so a root category carries no parent key at all.
		assertThat(data(get(PATH + "/" + root))).doesNotContainKey("parentId");
	}

	@Test
	void refusesToMakeACategoryItsOwnParent() {
		String id = create("RETAIL", "Retail", null);
		this.client.put()
			.uri(PATH + "/" + id)
			.body(Map.of("code", "RETAIL", "name", "Retail", "parentId", id))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void refusesToMoveACategoryUnderADescendantSeveralLevelsDown() {
		String root = create("RETAIL", "Retail", null);
		String child = create("PREMIUM", "Premium", root);
		String grandchild = create("GOLD", "Gold", child);

		this.client.put()
			.uri(PATH + "/" + root)
			.body(Map.of("code", "RETAIL", "name", "Retail", "parentId", grandchild))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void deletesALeaf() {
		String id = create("RETAIL", "Retail", null);
		this.client.delete().uri(PATH + "/" + id).exchange().expectStatus().isOk();
		this.client.get().uri(PATH + "/" + id).exchange().expectStatus().isNotFound();
	}

	/**
	 * Deleting a parent would leave its children pointing at a row that is gone.
	 */
	@Test
	void refusesToDeleteACategoryThatStillHasChildren() {
		String root = create("RETAIL", "Retail", null);
		create("PREMIUM", "Premium", root);

		Map<String, Object> body = this.client.delete()
			.uri(PATH + "/" + root)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.CONFLICT)
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();

		assertThat(body).containsEntry("code", "CONFLICT");
		assertThat(data(get(PATH + "/" + root))).containsEntry("code", "RETAIL");
	}

	@Test
	void reportsAnAbsentCategoryAsNotFound() {
		this.client.get().uri(PATH + "/" + ABSENT_ID).exchange().expectStatus().isNotFound();
		this.client.delete().uri(PATH + "/" + ABSENT_ID).exchange().expectStatus().isNotFound();
		this.client.put()
			.uri(PATH + "/" + ABSENT_ID)
			.body(Map.of("code", "X", "name", "X"))
			.exchange()
			.expectStatus()
			.isNotFound();
	}

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
		assertThat(body).containsEntry("code", "VALIDATION_FAILED");
		assertThat(violations(body)).extracting((violation) -> violation.get("field")).contains("code", "name");
	}

	@Test
	void rejectsAValueLongerThanTheColumnHolds() {
		Map<String, Object> body = this.client.post()
			.uri(PATH)
			.body(Map.of("code", "x".repeat(65), "name", "Retail"))
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
		assertThat(violations(body)).extracting((violation) -> violation.get("field")).contains("code");
	}

	@Test
	void rejectsANegativePosition() {
		this.client.post()
			.uri(PATH)
			.body(Map.of("code", "RETAIL", "name", "Retail", "sortOrder", -1))
			.exchange()
			.expectStatus()
			.isBadRequest();
	}

	private String create(String code, String name, String parentId) {
		Map<String, Object> body = (parentId != null) ? Map.of("code", code, "name", name, "parentId", parentId)
				: Map.of("code", code, "name", name);
		return (String) data(post(body)).get("id");
	}

	private String createWithOrder(String code, String name, String parentId, int sortOrder) {
		return (String) data(post(Map.of("code", code, "name", name, "parentId", parentId, "sortOrder", sortOrder)))
			.get("id");
	}

	private Map<String, Object> post(Map<String, Object> body) {
		return this.client.post()
			.uri(PATH)
			.body(body)
			.exchange()
			.expectStatus()
			.isCreated()
			.expectBody(JSON_OBJECT)
			.returnResult()
			.getResponseBody();
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
	private static List<Map<String, Object>> treeData(Map<String, ?> envelope) {
		return (List<Map<String, Object>>) envelope.get("data");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> children(Map<String, Object> node) {
		return (List<Map<String, Object>>) node.get("children");
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String, Object>> violations(Map<String, ?> envelope) {
		return (List<Map<String, Object>>) envelope.get("violations");
	}

}
