package org.v31bank.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Something that happened, stated in the language of the domain and in the past
 * tense: a wallet was credited, a transfer was settled, a customer was verified.
 * <p>
 * Events are records, and they are facts. Nothing in one is a request or a
 * decision, and nothing that has already happened can be cancelled — a movement
 * that turns out to be wrong is corrected by a further event, never by rewriting
 * or deleting the original. That is what makes the sequence a usable audit trail
 * and lets a balance be rebuilt from it.
 * <p>
 * Implementations carry no messaging dependency, as the domain layer requires.
 * What is common to all of them — an identity, when it happened, what it happened
 * to — is declared here so that the outbox, the publisher, and the consumers can
 * handle any event without knowing which one it is.
 *
 * <pre class="code">
 * public record WalletCredited(UUID eventId, Instant occurredAt, UUID aggregateId, Money amount)
 *         implements DomainEvent {
 * }
 * </pre>
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface DomainEvent {

    /**
     * Return the identity of this occurrence, used to recognise a replay. A
     * consumer will be handed the same event more than once — that is what
     * at-least-once delivery means — and this is what tells it so.
     * <p>
     * Issued with {@link org.v31bank.core.util.Uuids#timeOrdered()}, so events
     * sort by the order they were recorded.
     * @return the event identifier
     */
    UUID eventId();

    /**
     * Return when the fact became true, which is set by the aggregate that
     * recorded it rather than by whatever publishes it later. The two can be far
     * apart when a relay is catching up, and it is the first that the domain
     * means.
     * @return the instant it happened
     */
    Instant occurredAt();

    /**
     * Return what this happened to — the wallet that was credited, the transfer
     * that settled. Events about the same aggregate are kept in order relative to
     * each other, which is what this identifies them by.
     * @return the identifier of the aggregate the event belongs to
     */
    UUID aggregateId();

    /**
     * Return the name this event is published under.
     * <p>
     * Part of the wire contract: consumers route on it, so it survives the class
     * being renamed or moved only if it is pinned by overriding this. The default
     * is the simple class name, which is right until the first consumer exists.
     * @return the event type
     */
    default String eventType() {
        return getClass().getSimpleName();
    }

}
