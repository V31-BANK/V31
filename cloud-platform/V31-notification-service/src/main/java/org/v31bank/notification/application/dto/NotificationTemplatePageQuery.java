package org.v31bank.notification.application.dto;

import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated notification template query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class NotificationTemplatePageQuery extends PageQuery {

    /**
     * Code fragment to match, case-insensitive.
     */
    private String code;

    /**
     * Channel to match.
     */
    private NotificationChannel channel;

    /**
     * Status to match.
     */
    private NotificationTemplateStatus status;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public NotificationChannel getChannel() {
        return this.channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public NotificationTemplateStatus getStatus() {
        return this.status;
    }

    public void setStatus(NotificationTemplateStatus status) {
        this.status = status;
    }

}
