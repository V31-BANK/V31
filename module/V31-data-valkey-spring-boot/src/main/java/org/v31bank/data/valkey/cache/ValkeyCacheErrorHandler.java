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

package org.v31bank.data.valkey.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Decides what happens when Valkey cannot be reached while a cached method runs.
 * <p>
 * Spring's default is to let the failure out, which turns every cached call into one more
 * thing that fails when the cache does. A cache exists to spare the database work; a
 * cache that is down should cost latency, not availability.
 *
 * <h2>Reads and writes degrade</h2>
 *
 * A failed lookup is reported and swallowed, so the call falls through to the method the
 * cache was standing in for and the caller gets a correct answer more slowly. A failed
 * write is reported and swallowed too: the value was computed, the caller can have it,
 * and not storing it costs only the next lookup.
 *
 * <h2>Evictions do not</h2>
 *
 * A failed eviction is different in kind, and is allowed out. Eviction is how the
 * application says the cached value is now wrong. Swallowing that failure leaves Valkey
 * serving a value the application knows to be stale — a balance, a limit, a frozen status
 * — with nothing left to correct it until the entry expires on its own. Failing the write
 * that could not invalidate its cache is the lesser outcome: the caller can retry, and
 * nothing downstream has been told a change happened that readers cannot see.
 * <p>
 * Set {@code v31.data.valkey.cache.fail-fast} to let read and write failures out as well,
 * for a service that would rather stop than serve from a degraded path.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ValkeyCacheErrorHandler implements CacheErrorHandler {

	private static final Log logger = LogFactory.getLog(ValkeyCacheErrorHandler.class);

	private final boolean failFast;

	/**
	 * Create a handler.
	 * @param failFast whether a failed read or write is allowed out rather than degrading
	 * to the underlying method
	 */
	public ValkeyCacheErrorHandler(boolean failFast) {
		this.failFast = failFast;
	}

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		degrade("read from", cache, exception);
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		degrade("write to", cache, exception);
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		logger.error(
				"Could not evict from cache '" + cache.getName() + "'; it may now be serving a value known to be stale",
				exception);
		throw exception;
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		logger.error("Could not clear cache '" + cache.getName() + "'; it may now be serving values known to be stale",
				exception);
		throw exception;
	}

	/**
	 * Report a failure the caller can be shielded from.
	 * <p>
	 * The key is deliberately left out of the message. Cache keys are built from whatever
	 * identifies the thing being cached, which is regularly an account number or an email
	 * address, and a warning logged on every request during an outage is the last place
	 * to start copying those.
	 * @param operation what was being attempted, for the message
	 * @param cache the cache it was attempted against
	 * @param exception the failure
	 */
	private void degrade(String operation, Cache cache, RuntimeException exception) {
		if (this.failFast) {
			throw exception;
		}
		logger.warn("Could not " + operation + " cache '" + cache.getName() + "', continuing without it: "
				+ exception.getMessage());
		if (logger.isDebugEnabled()) {
			logger.debug("Cache failure detail for '" + cache.getName() + "'", exception);
		}
	}

}
