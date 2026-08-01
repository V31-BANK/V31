package org.v31bank.notification.domain.constant;

/**
 * Lifecycle status of a notificationTemplate.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum NotificationTemplateStatus {

    /**
     * Being written, and not sent to anyone.
     */
    DRAFT,

    /**
     * In use.
     */
    ACTIVE,

    /**
     * Replaced. Kept so that a message already sent can still be explained.
     */
    RETIRED

}
