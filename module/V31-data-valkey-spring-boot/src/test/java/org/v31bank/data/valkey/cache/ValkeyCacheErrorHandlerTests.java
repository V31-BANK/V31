package org.v31bank.data.valkey.cache;

import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.dao.QueryTimeoutException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ValkeyCacheErrorHandler}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class ValkeyCacheErrorHandlerTests {

    private static final Cache CACHE = new ConcurrentMapCache("customers");

    private final ValkeyCacheErrorHandler handler = new ValkeyCacheErrorHandler(false);

    private final ValkeyCacheErrorHandler failFast = new ValkeyCacheErrorHandler(true);

    @Test
    void letsAFailedLookupFallThroughToTheMethod() {
        assertDoesNotThrow(() -> this.handler.handleCacheGetError(unreachable(), CACHE, "7"));
    }

    @Test
    void letsAFailedWriteGoUnstored() {
        assertDoesNotThrow(() -> this.handler.handleCachePutError(unreachable(), CACHE, "7", "value"));
    }

    @Test
    void refusesToSwallowAFailedEviction() {
        QueryTimeoutException failure = unreachable();
        assertSame(failure, assertThrows(QueryTimeoutException.class,
                () -> this.handler.handleCacheEvictError(failure, CACHE, "7")),
                "an eviction that did not happen leaves a value the application knows is wrong");
    }

    @Test
    void refusesToSwallowAFailedClear() {
        QueryTimeoutException failure = unreachable();
        assertSame(failure,
                assertThrows(QueryTimeoutException.class, () -> this.handler.handleCacheClearError(failure, CACHE)));
    }

    @Test
    void letsEverythingOutWhenAskedToFailFast() {
        assertThrows(QueryTimeoutException.class, () -> this.failFast.handleCacheGetError(unreachable(), CACHE, "7"));
        assertThrows(QueryTimeoutException.class,
                () -> this.failFast.handleCachePutError(unreachable(), CACHE, "7", "value"));
        assertThrows(QueryTimeoutException.class, () -> this.failFast.handleCacheEvictError(unreachable(), CACHE, "7"));
    }

    private static QueryTimeoutException unreachable() {
        return new QueryTimeoutException("Valkey command timed out");
    }

}
