package org.v31bank.notification.domain.constant;

/**
 * Classification carried by a notificationTemplate.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public enum NotificationChannel {

    /**
     * Sent to the address on the customer's file.
     */
    EMAIL,

    /**
     * Sent to the verified phone number. Short, and never carrying anything secret.
     */
    SMS,

    /**
     * Delivered to the mobile application.
     */
    PUSH,

    /**
     * Posted to an endpoint a business customer registered.
     */
    WEBHOOK

}
