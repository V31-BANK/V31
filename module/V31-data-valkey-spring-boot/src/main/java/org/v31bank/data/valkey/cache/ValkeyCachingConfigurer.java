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

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Hands Spring's caching infrastructure the {@link ValkeyCacheErrorHandler}.
 * <p>
 * {@link CachingConfigurer} is the only way to replace the error handler, and Spring
 * accepts exactly one of them, so this backs off entirely when the application supplies
 * its own.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class ValkeyCachingConfigurer implements CachingConfigurer {

	private final CacheErrorHandler errorHandler;

	public ValkeyCachingConfigurer(CacheErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public CacheErrorHandler errorHandler() {
		return this.errorHandler;
	}

}
