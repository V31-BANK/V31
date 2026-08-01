package org.v31bank.notification.presentation.dto;

import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;

/**
 * Request body for creating or updating a notification template.
 *
 * @param code the code, unique
 * @param name the display name
 * @param channel the channel
 * @param status the status; ignored on create, where a record always starts
 * {@code DRAFT}, and left unchanged on update when {@code null}
 * @author Xander Wang
 * @since 0.2.0
 */
public record NotificationTemplateRequest(String code, String name, NotificationChannel channel, NotificationTemplateStatus status) {

}
