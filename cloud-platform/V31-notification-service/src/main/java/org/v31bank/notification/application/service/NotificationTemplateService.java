package org.v31bank.notification.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.application.dto.NotificationTemplatePageQuery;
import org.v31bank.notification.application.port.in.NotificationTemplateUseCase;
import org.v31bank.notification.application.port.out.NotificationTemplatePort;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;
import org.v31bank.notification.domain.model.NotificationTemplate;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Default {@link NotificationTemplateUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class NotificationTemplateService implements NotificationTemplateUseCase {

    private final NotificationTemplatePort notificationTemplateRepository;

    public NotificationTemplateService(NotificationTemplatePort notificationTemplateRepository) {
        this.notificationTemplateRepository = notificationTemplateRepository;
    }

    @Override
    public ApiResponse<NotificationTemplate> create(String code, String name, NotificationChannel channel) {
        if (this.notificationTemplateRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        NotificationTemplate notificationTemplate = new NotificationTemplate();
        notificationTemplate.setCode(code);
        notificationTemplate.setName(name);
        notificationTemplate.setChannel(channel);
        return ApiResponse.ok(this.notificationTemplateRepository.save(notificationTemplate));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTemplate> get(UUID id) {
        return this.notificationTemplateRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationTemplate> page(NotificationTemplatePageQuery query) {
        return this.notificationTemplateRepository.findPage(query);
    }

    @Override
    public ApiResponse<NotificationTemplate> update(UUID id, String code, String name, NotificationChannel channel, NotificationTemplateStatus status) {
        Optional<NotificationTemplate> found = this.notificationTemplateRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No notification template exists with id " + id);
        }
        NotificationTemplate notificationTemplate = found.get();
        if (!notificationTemplate.getCode().equals(code) && this.notificationTemplateRepository.existsByCode(code)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Code '" + code + "' is already in use");
        }
        notificationTemplate.setCode(code);
        notificationTemplate.setName(name);
        notificationTemplate.setChannel(channel);
        if (status != null) {
            notificationTemplate.setStatus(status);
        }
        return ApiResponse.ok(this.notificationTemplateRepository.save(notificationTemplate));
    }

    @Override
    public ApiResponse<NotificationTemplate> delete(UUID id) {
        Optional<NotificationTemplate> found = this.notificationTemplateRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No notification template exists with id " + id);
        }
        NotificationTemplate notificationTemplate = found.get();
        this.notificationTemplateRepository.delete(notificationTemplate);
        return ApiResponse.ok(notificationTemplate);
    }

}
