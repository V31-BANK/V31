package org.v31bank.data.valkey.lock;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ValkeyLock}.
 * <p>
 * Stubbing generic Spring Data types means matching on raw {@code RedisScript}
 * and {@code ValueOperations}, which is unavoidable with Mockito and confined to
 * this class.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class ValkeyLockTests {

    private static final Duration LEASE = Duration.ofSeconds(30);

    private StringRedisTemplate template;

    private ValueOperations<String, String> values;

    private ValkeyLock lock;

    @BeforeEach
    void setUp() {
        this.template = mock(StringRedisTemplate.class);
        this.values = mock(ValueOperations.class);
        when(this.template.opsForValue()).thenReturn(this.values);
        this.lock = new ValkeyLock(this.template);
    }

    @Test
    void takesTheLockWhenItIsFree() {
        when(this.values.setIfAbsent(eq("v31:batch"), any(), eq(LEASE))).thenReturn(true);
        Optional<String> token = this.lock.acquire("v31:batch", LEASE);
        assertTrue(token.isPresent());
        assertEquals(7, UUID.fromString(token.get()).version(), "the token should be a time-ordered identifier");
    }

    @Test
    void doesNotTakeALockSomebodyElseHolds() {
        when(this.values.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);
        assertTrue(this.lock.acquire("v31:batch", LEASE).isEmpty());
    }

    @Test
    void givesEachHolderADifferentToken() {
        when(this.values.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
        assertNotEquals(this.lock.acquire("v31:batch", LEASE), this.lock.acquire("v31:batch", LEASE));
    }

    @Test
    void refusesALeaseThatWouldExpireImmediately() {
        assertThrows(IllegalArgumentException.class, () -> this.lock.acquire("v31:batch", Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> this.lock.acquire("v31:batch", Duration.ofSeconds(-1)));
    }

    @Test
    void releasesWithTheTokenItWasGiven() {
        when(this.template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        assertTrue(this.lock.release("v31:batch", "token-a"));
        ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
        verify(this.template).execute(any(RedisScript.class), eq(List.of("v31:batch")), args.capture());
        assertEquals("token-a", args.getValue());
    }

    @Test
    void reportsAReleaseThatChangedNothing() {
        when(this.template.execute(any(RedisScript.class), anyList(), any())).thenReturn(0L);
        assertFalse(this.lock.release("v31:batch", "token-a"),
                "a lease that already expired is not this caller's to release");
    }

    @Test
    void extendsALeaseItStillHolds() {
        when(this.template.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(1L);
        assertTrue(this.lock.extend("v31:batch", "token-a", LEASE));
        verify(this.template).execute(any(RedisScript.class), eq(List.of("v31:batch")), eq("token-a"),
                eq(Long.toString(LEASE.toMillis())));
    }

    @Test
    void reportsALeaseThatIsNoLongerItsToExtend() {
        when(this.template.execute(any(RedisScript.class), anyList(), any(), any())).thenReturn(0L);
        assertFalse(this.lock.extend("v31:batch", "token-a", LEASE),
                "a holder that lost its lease has to stop, not carry on");
    }

    @Test
    void refusesToExtendByNothing() {
        assertThrows(IllegalArgumentException.class, () -> this.lock.extend("v31:batch", "token-a", Duration.ZERO));
    }

    @Test
    void runsTheActionWhileHoldingTheLockAndGivesItBack() {
        when(this.values.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
        when(this.template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        Optional<String> result = this.lock.runExclusively("v31:batch", LEASE, () -> "done");
        assertEquals(Optional.of("done"), result);
        verify(this.template).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void givesTheLockBackWhenTheActionFails() {
        when(this.values.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
        when(this.template.execute(any(RedisScript.class), anyList(), any())).thenReturn(1L);
        assertThrows(IllegalStateException.class, () -> this.lock.runExclusively("v31:batch", LEASE, () -> {
            throw new IllegalStateException("boom");
        }));
        verify(this.template).execute(any(RedisScript.class), anyList(), any());
    }

    @Test
    void doesNotRunTheActionWhenTheLockIsHeld() {
        when(this.values.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);
        AtomicBoolean ran = new AtomicBoolean();
        Optional<String> result = this.lock.runExclusively("v31:batch", LEASE, () -> {
            ran.set(true);
            return "done";
        });
        assertTrue(result.isEmpty());
        assertFalse(ran.get());
        verify(this.template, never()).execute(any(RedisScript.class), anyList(), any());
    }

}
