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

package org.v31bank.cbs.infra.persistence.adapter;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

import org.v31bank.cbs.application.dto.BankProductPageQuery;
import org.v31bank.cbs.application.port.out.BankProductPort;
import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.cbs.domain.model.BankProduct;
import org.v31bank.cbs.infra.persistence.valkey.BankProductValkeyKeys;
import org.v31bank.core.response.PageResponse;
import org.v31bank.core.util.Uuids;

/**
 * {@link BankProductPort} adapter backed by Valkey.
 *
 * <h2>How a page is produced</h2>
 *
 * A key-value store cannot answer "the third page, newest first" over a keyspace; finding
 * out would mean visiting every key. So the ordering is maintained instead of computed:
 * every write puts the product's identifier into sorted sets scored by creation time, and
 * a page is a range of one of them — logarithmic, with the total coming free from the
 * set's cardinality. Filtering by both category and status intersects the two sets into a
 * short-lived key, which is what a query planner would have done and here has to be said
 * out loud.
 *
 * <h2>Why everything is written as text</h2>
 *
 * The product is serialized to JSON here and stored through the string template, rather
 * than handed to the JSON template the Valkey starter installs. Both write the same bytes
 * for the product — the starter's serializer is what produces them either way, so a
 * stored value still names only a type this platform trusts. The difference is everything
 * around it: that template would serialize the identifiers going into the sorted sets as
 * JSON too, leaving every index member quoted, so an operator reading {@code ZRANGE}
 * during an incident could not paste what they saw into a {@code GET}. Sorted set members
 * share the value serializer, so the only way to keep them readable is to keep the whole
 * write on one string template — which also keeps it on one connection, and therefore in
 * one transaction.
 *
 * <h2>What is atomic and what is not</h2>
 *
 * The writes making up one change are sent as a single transaction, so nothing observes a
 * product whose indexes disagree with it. Valkey does not roll back, though: if one
 * command in that transaction fails the others still stand. The code claim is a separate
 * single command, and is genuinely atomic — that is what stands in for the unique
 * constraint a relational store would have provided.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class BankProductPersistenceAdapter implements BankProductPort {

	/**
	 * The largest page a caller may ask for, so that one request cannot pull the whole
	 * catalogue into memory.
	 */
	private static final int MAX_PAGE_SIZE = 500;

	/**
	 * How long an intersection survives if the request reading it dies before cleaning
	 * up. Long enough to read a page, short enough not to accumulate.
	 */
	private static final Duration INTERSECTION_TTL = Duration.ofSeconds(30);

	private final StringRedisTemplate valkey;

	private final RedisSerializer<Object> serializer;

	private final BankProductValkeyKeys keys;

	public BankProductPersistenceAdapter(StringRedisTemplate valkey,
			@Qualifier("valkeyValueSerializer") RedisSerializer<Object> serializer, BankProductValkeyKeys keys) {
		this.valkey = valkey;
		this.serializer = serializer;
		this.keys = keys;
	}

	/**
	 * Take the code, if it is free.
	 * <p>
	 * {@code SET NX} is one command and atomic on its own, which is the entire reason it
	 * can stand in for a unique constraint. It belongs to no transaction and needs none.
	 */
	@Override
	public boolean claimCode(String code, UUID id) {
		return Boolean.TRUE.equals(this.valkey.opsForValue().setIfAbsent(this.keys.code(code), id.toString()));
	}

	@Override
	public void releaseCode(String code) {
		this.valkey.delete(this.keys.code(code));
	}

	/**
	 * Write the product and rebuild its place in the indexes.
	 * <p>
	 * The identifier is removed from every category and status set before being added to
	 * the two it now belongs to. Removing from all of them rather than from the ones it
	 * used to be in is deliberate: the previous values are not carried into this call,
	 * and with four categories and three statuses the difference is a handful of commands
	 * inside a transaction that was being sent anyway.
	 */
	@Override
	public BankProduct save(BankProduct product) {
		String member = product.getId().toString();
		String json = toJson(product);
		double score = product.getCreatedDate().toEpochMilli();
		BankProductValkeyKeys names = this.keys;
		this.valkey.execute(new SessionCallback<Object>() {
			@Override
			public <K, V> Object execute(RedisOperations<K, V> operations) {
				// SessionCallback is handed the template's operations without saying
				// what they are keyed by; this template is the one built for strings.
				@SuppressWarnings("unchecked")
				RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
				ops.multi();
				ops.opsForValue().set(names.product(product.getId()), json);
				ops.opsForZSet().add(names.index(), member, score);
				for (BankProductCategory category : BankProductCategory.values()) {
					ops.opsForZSet().remove(names.categoryIndex(category), member);
				}
				for (BankProductStatus status : BankProductStatus.values()) {
					ops.opsForZSet().remove(names.statusIndex(status), member);
				}
				ops.opsForZSet().add(names.categoryIndex(product.getCategory()), member, score);
				ops.opsForZSet().add(names.statusIndex(product.getStatus()), member, score);
				return ops.exec();
			}
		});
		return product;
	}

	@Override
	public Optional<BankProduct> findById(UUID id) {
		return Optional.ofNullable(this.valkey.opsForValue().get(this.keys.product(id))).map(this::fromJson);
	}

	@Override
	public PageResponse<BankProduct> findPage(BankProductPageQuery query) {
		int number = Math.max(query.getPageNumber(), BankProductPageQuery.FIRST_PAGE_NUMBER);
		int size = Math.clamp(query.getPageSize(), 1, MAX_PAGE_SIZE);
		String index = indexFor(query);
		try {
			Long total = this.valkey.opsForZSet().zCard(index);
			if (total == null || total == 0) {
				return PageResponse.empty(number, size);
			}
			long offset = (long) (number - 1) * size;
			Set<String> members = this.valkey.opsForZSet().reverseRange(index, offset, offset + size - 1L);
			return PageResponse.of(read(members), total, number, size);
		}
		finally {
			if (isIntersection(query)) {
				this.valkey.delete(index);
			}
		}
	}

	@Override
	public void delete(BankProduct product) {
		String member = product.getId().toString();
		BankProductValkeyKeys names = this.keys;
		this.valkey.execute(new SessionCallback<Object>() {
			@Override
			public <K, V> Object execute(RedisOperations<K, V> operations) {
				// SessionCallback is handed the template's operations without saying
				// what they are keyed by; this template is the one built for strings.
				@SuppressWarnings("unchecked")
				RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
				ops.multi();
				ops.delete(names.product(product.getId()));
				ops.opsForZSet().remove(names.index(), member);
				for (BankProductCategory category : BankProductCategory.values()) {
					ops.opsForZSet().remove(names.categoryIndex(category), member);
				}
				for (BankProductStatus status : BankProductStatus.values()) {
					ops.opsForZSet().remove(names.statusIndex(status), member);
				}
				return ops.exec();
			}
		});
	}

	/**
	 * Choose the sorted set a page comes from, building one where the filters asked for a
	 * combination no single set holds.
	 * @param query the filters
	 * @return the key of the set to page over, temporary when it was intersected
	 */
	private String indexFor(BankProductPageQuery query) {
		if (isIntersection(query)) {
			String destination = this.keys.intersection(Uuids.timeOrdered().toString());
			this.valkey.opsForZSet()
				.intersectAndStore(this.keys.categoryIndex(query.getCategory()),
						this.keys.statusIndex(query.getStatus()), destination);
			this.valkey.expire(destination, INTERSECTION_TTL);
			return destination;
		}
		if (query.getCategory() != null) {
			return this.keys.categoryIndex(query.getCategory());
		}
		if (query.getStatus() != null) {
			return this.keys.statusIndex(query.getStatus());
		}
		return this.keys.index();
	}

	private static boolean isIntersection(BankProductPageQuery query) {
		return query.getCategory() != null && query.getStatus() != null;
	}

	/**
	 * Fetch the products a page of identifiers refers to.
	 * <p>
	 * An identifier with nothing behind it is skipped rather than reported as a gap: it
	 * means the product was deleted between the range being read and the values being
	 * fetched, which is a page one request out of date, not an error.
	 * @param members the identifiers, in the order they should appear
	 * @return the products
	 */
	private List<BankProduct> read(Set<String> members) {
		if (members == null || members.isEmpty()) {
			return List.of();
		}
		List<String> productKeys = members.stream().map((id) -> this.keys.product(UUID.fromString(id))).toList();
		List<String> values = this.valkey.opsForValue().multiGet(productKeys);
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(Objects::nonNull).map(this::fromJson).toList();
	}

	/**
	 * Render a product through the serializer the Valkey starter configured, so that what
	 * is stored names its type and can only be read back as one this platform trusts.
	 * @param product the product to render
	 * @return the JSON to store
	 */
	private String toJson(BankProduct product) {
		return new String(Objects.requireNonNull(this.serializer.serialize(product)), StandardCharsets.UTF_8);
	}

	private BankProduct fromJson(String json) {
		return (BankProduct) this.serializer.deserialize(json.getBytes(StandardCharsets.UTF_8));
	}

}
