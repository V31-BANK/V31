package org.v31bank.core.util;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Uuids}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
class UuidsTests {

    @Test
    void generatesVersionSevenIdentifiers() {
        UUID uuid = Uuids.timeOrdered();
        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void ordersIdentifiersByWhenTheyWereIssued() {
        long previous = Long.MIN_VALUE;
        for (int i = 0; i < 20_000; i++) {
            long current = Uuids.timeOrdered().getMostSignificantBits();
            assertTrue(current > previous, "identifier " + i + " did not advance");
            previous = current;
        }
    }

    @Test
    void doesNotRepeatItselfUnderConcurrency() throws Exception {
        int threads = 8;
        int perThread = 5_000;
        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Callable<Set<UUID>>> tasks = IntStream.range(0, threads)
                .<Callable<Set<UUID>>>mapToObj((i) -> () -> generate(perThread))
                .toList();
            Set<UUID> generated = new HashSet<>();
            for (Future<Set<UUID>> future : executor.invokeAll(tasks)) {
                generated.addAll(future.get());
            }
            assertEquals(threads * perThread, generated.size());
        }
    }

    @Test
    void carriesTheInstantItWasIssued() {
        Instant before = Instant.now().minusSeconds(1);
        Instant issued = Uuids.timestampOf(Uuids.timeOrdered());
        assertTrue(issued.isAfter(before), issued + " should be after " + before);
        assertTrue(issued.isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void refusesToReadATimestampThatIsNotThere() {
        assertThrows(IllegalArgumentException.class, () -> Uuids.timestampOf(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> Uuids.timestampOf(null));
    }

    private static Set<UUID> generate(int count) {
        Set<UUID> generated = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            generated.add(Uuids.timeOrdered());
        }
        return generated;
    }

}
