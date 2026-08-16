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

package org.v31bank.data.valkey.lock;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import org.v31bank.core.util.Uuids;

/**
 * Keeps two nodes from doing the same piece of work at the same time.
 * <p>
 * A lock is a key set only if it is absent, carrying a token unique to whoever took it
 * and an expiry so that a node dying while holding one does not block the work forever.
 * Releasing compares the token before deleting, in a script so that the comparison and
 * the delete cannot be interleaved — without that, a holder whose lock had already
 * expired would delete the one its successor is now holding.
 *
 * <h2>What this is not</h2>
 *
 * It is not a guarantee that the work happens once. If the holder stalls longer than the
 * lease — a long garbage collection, a paused container, a network partition — the lease
 * expires, another node takes it, and both believe they hold it. No lock built on a
 * single expiring key avoids this.
 * <p>
 * So it is worth having for work that is wasteful to repeat: a nightly reconciliation, a
 * batch of notifications, a cache rebuild. It is not what makes a payment happen once.
 * That has to come from somewhere the outcome is recorded atomically — an idempotency key
 * with a unique constraint behind it — and the lock only saves the second attempt from
 * doing the work before losing the race.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ValkeyLock {

	/**
	 * Deletes the key only if it still carries the token the caller was given. Returns
	 * the number of keys deleted, so nothing is 0 and success is 1.
	 */
	private static final RedisScript<Long> RELEASE = new DefaultRedisScript<>("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			    return redis.call('del', KEYS[1])
			else
			    return 0
			end
			""", Long.class);

	/**
	 * Pushes the expiry out, again only if the key still carries the caller's token.
	 * Returns 1 when the expiry was reset and 0 when the lock is no longer the caller's
	 * to extend.
	 */
	private static final RedisScript<Long> EXTEND = new DefaultRedisScript<>("""
			if redis.call('get', KEYS[1]) == ARGV[1] then
			    return redis.call('pexpire', KEYS[1], ARGV[2])
			else
			    return 0
			end
			""", Long.class);

	private final StringRedisTemplate template;

	public ValkeyLock(StringRedisTemplate template) {
		this.template = Objects.requireNonNull(template, "template must not be null");
	}

	/**
	 * Take the lock if it is free, without waiting for it.
	 * <p>
	 * The lease has to outlast the work. A lease shorter than the work does not fail — it
	 * silently lets a second holder in, which is the failure this is meant to prevent.
	 * @param key the key identifying what is being locked
	 * @param lease how long the lock is held before it expires on its own
	 * @return the token to release it with, or empty if somebody else holds it
	 */
	public Optional<String> acquire(String key, Duration lease) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(lease, "lease must not be null");
		if (lease.isNegative() || lease.isZero()) {
			throw new IllegalArgumentException("A lease must be positive, or the lock expires before it is used");
		}
		String token = Uuids.timeOrdered().toString();
		return Boolean.TRUE.equals(this.template.opsForValue().setIfAbsent(key, token, lease)) ? Optional.of(token)
				: Optional.empty();
	}

	/**
	 * Give the lock back, provided it is still the one that was taken.
	 * @param key the key the lock was taken on
	 * @param token the token {@link #acquire} returned
	 * @return {@code true} if this call released it, {@code false} if the lease had
	 * already expired and the lock is gone or held by someone else
	 */
	public boolean release(String key, String token) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(token, "token must not be null");
		Long released = this.template.execute(RELEASE, List.of(key), token);
		return released != null && released > 0;
	}

	/**
	 * Push the expiry out on a lock still held, for work that is taking longer than its
	 * lease allowed for.
	 * <p>
	 * A caller whose work has no bound should be extending as it goes rather than taking
	 * a very long lease at the start: a long lease is also how long the work stays
	 * blocked after the node holding it dies.
	 * @param key the key the lock was taken on
	 * @param token the token {@link #acquire} returned
	 * @param lease how much longer to hold it, from now
	 * @return {@code true} if the lease was extended, {@code false} if it had already
	 * expired and the lock is gone or held by someone else — in which case the caller is
	 * no longer the holder and should stop
	 */
	public boolean extend(String key, String token, Duration lease) {
		Objects.requireNonNull(key, "key must not be null");
		Objects.requireNonNull(token, "token must not be null");
		Objects.requireNonNull(lease, "lease must not be null");
		if (lease.isNegative() || lease.isZero()) {
			throw new IllegalArgumentException("A lease must be positive, or the lock expires before it is used");
		}
		Long extended = this.template.execute(EXTEND, List.of(key), token, Long.toString(lease.toMillis()));
		return extended != null && extended > 0;
	}

	/**
	 * Run the action if the lock is free, and give the lock back afterwards whether or
	 * not the action succeeded.
	 * @param key the key identifying what is being locked
	 * @param lease how long the lock is held before it expires on its own
	 * @param action the work to do while holding it
	 * @param <T> what the action produces
	 * @return what the action returned, or empty if the lock was already held
	 */
	public <T> Optional<T> runExclusively(String key, Duration lease, Supplier<T> action) {
		Objects.requireNonNull(action, "action must not be null");
		Optional<String> token = acquire(key, lease);
		if (token.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.ofNullable(action.get());
		}
		finally {
			release(key, token.get());
		}
	}

}
