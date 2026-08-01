package org.v31bank.notification.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;
import org.v31bank.data.jpa.domain.BaseEntity;

/**
 * The wording of a message the bank sends, held apart from the code that
 * sends it.
 * <p>
 * Keeping it here means the text a customer reads can be corrected, translated
 * or put through legal review without touching the service that delivers it.
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "notification_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_template_code", columnNames = "code"))
public class NotificationTemplate extends BaseEntity {

    /**
     * The code the template is known by, unique.
     */
    @Column(name = "code", length = 32, nullable = false)
    private String code;

    /**
     * The display name.
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * How the message reaches the customer.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private NotificationTemplateStatus status = NotificationTemplateStatus.DRAFT;

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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
