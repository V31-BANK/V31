package org.v31bank.notification.infra.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.v31bank.notification.domain.model.NotificationTemplate;

/**
 * Spring Data JPA repository for {@link NotificationTemplate}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface JpaNotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID>, JpaSpecificationExecutor<NotificationTemplate> {

    boolean existsByCode(String code);

}
