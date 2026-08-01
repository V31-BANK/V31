package org.v31bank.notification.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.notification.application.dto.NotificationTemplatePageQuery;
import org.v31bank.notification.application.port.out.NotificationTemplatePort;
import org.v31bank.notification.domain.model.NotificationTemplate;
import org.v31bank.notification.infra.persistence.jpa.JpaNotificationTemplateRepository;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * {@link NotificationTemplatePort} adapter backed by Spring Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class NotificationTemplatePersistenceAdapter implements NotificationTemplatePort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    private final JpaNotificationTemplateRepository jpaRepository;

    public NotificationTemplatePersistenceAdapter(JpaNotificationTemplateRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public NotificationTemplate save(NotificationTemplate notificationTemplate) {
        return this.jpaRepository.save(notificationTemplate);
    }

    @Override
    public Optional<NotificationTemplate> findById(UUID id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public PageResult<NotificationTemplate> findPage(NotificationTemplatePageQuery query) {
        Specification<NotificationTemplate> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getCode())) {
                predicates.add(cb.like(cb.lower(root.get("code")), "%" + query.getCode().toLowerCase() + "%"));
            }
            if (query.getChannel() != null) {
                predicates.add(cb.equal(root.get("channel"), query.getChannel()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResult.of(this.jpaRepository.findAll(spec, query.toPageable(NEWEST_FIRST)));
    }

    @Override
    public boolean existsByCode(String code) {
        return this.jpaRepository.existsByCode(code);
    }

    @Override
    public void delete(NotificationTemplate notificationTemplate) {
        this.jpaRepository.delete(notificationTemplate);
    }

}
