package org.v31bank.data.valkey.cache;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Hands Spring's caching infrastructure the {@link ValkeyCacheErrorHandler}.
 * <p>
 * {@link CachingConfigurer} is the only way to replace the error handler, and
 * Spring accepts exactly one of them, so this backs off entirely when the
 * application supplies its own.
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
