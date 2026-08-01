package org.v31bank.notification.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;
import org.v31bank.notification.domain.model.NotificationTemplate;

/**
 * API representation of a notification template.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public record NotificationTemplateResponse(UUID id, String code, String name, NotificationChannel channel, NotificationTemplateStatus status, Instant createdDate,
        Instant lastModifiedDate) {

    public static NotificationTemplateResponse from(NotificationTemplate notificationTemplate) {
        return new NotificationTemplateResponse(notificationTemplate.getId(), notificationTemplate.getCode(), notificationTemplate.getName(), notificationTemplate.getChannel(), notificationTemplate.getStatus(),
                notificationTemplate.getCreatedDate(), notificationTemplate.getLastModifiedDate());
    }

}
