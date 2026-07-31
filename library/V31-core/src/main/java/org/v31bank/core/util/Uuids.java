package org.v31bank.core.util;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Generates time-ordered UUIDv7 identifiers, as specified by RFC 9562.
 * <p>
 * A version 7 identifier leads with a millisecond timestamp, so identifiers
 * generated one after another sort one after another. That matters for anything
 * indexed: a random version 4 identifier lands in a different page of the index
 * on every insert, and a table taking a high write rate spends its time splitting
 * pages that a sequential key would have appended to.
 * <p>
 * Entities persisted through Hibernate get theirs from
 * {@code @UuidGenerator(style = VERSION_7)} on {@code BaseEntity}. This class is
 * for the identifiers nothing else issues: domain events, outbox records,
 * correlation identifiers, and anything created before it reaches a repository.
 * <p>
 * Identifiers issued within the same millisecond are strictly increasing, and the
 * generator does not go backwards if the system clock does. The random parts come
 * from {@link SecureRandom}, so an identifier is not guessable from the ones
 * around it — an identifier that appears in a URL should not tell its holder what
 * the next one will be.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class Uuids {

    /**
     * The counter occupies the 12 bits RFC 9562 calls {@code rand_a}.
     */
    private static final int MAX_COUNTER = 0xFFF;

    /**
     * Each millisecond starts its counter in the bottom quarter of that range, so
     * the value is unpredictable while still leaving room for at least 3072
     * identifiers before the millisecond has to be carried forward.
     */
    private static final int COUNTER_SEED_MASK = 0x3FF;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final AtomicReference<State> STATE = new AtomicReference<>(new State(Long.MIN_VALUE, 0));

    private Uuids() {
    }

    /**
     * Return a new time-ordered identifier.
     * @return a UUIDv7
     */
    public static UUID timeOrdered() {
        State state = nextState();
        long timestamp = state.millis() & 0xFFFFFFFFFFFFL;
        long mostSignificantBits = (timestamp << 16) | (0x7L << 12) | (state.counter() & MAX_COUNTER);
        long leastSignificantBits = (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    /**
     * Return when the given identifier was issued.
     * @param uuid the identifier to read
     * @return the instant it was issued, to the millisecond
     * @throws IllegalArgumentException if the identifier is not a UUIDv7, since no
     * other version carries a timestamp in this position
     */
    public static Instant timestampOf(UUID uuid) {
        if (uuid == null || uuid.version() != 7) {
            throw new IllegalArgumentException("Only a UUIDv7 carries a timestamp, but got " + uuid);
        }
        return Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }

    /**
     * Advance the generator, keeping the sequence strictly increasing.
     * <p>
     * Within one millisecond the counter carries the ordering. When it runs out,
     * or when the clock steps backwards — an NTP correction, a leap second
     * adjustment — the generator borrows from the next millisecond instead of
     * reusing a timestamp it has already issued.
     * @return the timestamp and counter this identifier is built from
     */
    private static State nextState() {
        while (true) {
            State current = STATE.get();
            long millis = System.currentTimeMillis();
            State next;
            if (millis > current.millis()) {
                next = new State(millis, RANDOM.nextInt() & COUNTER_SEED_MASK);
            }
            else if (current.counter() < MAX_COUNTER) {
                next = new State(current.millis(), current.counter() + 1);
            }
            else {
                next = new State(current.millis() + 1, 0);
            }
            if (STATE.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    private record State(long millis, int counter) {

    }

}
