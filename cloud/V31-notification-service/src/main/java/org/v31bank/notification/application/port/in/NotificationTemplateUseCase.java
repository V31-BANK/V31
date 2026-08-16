/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.notification.application.port.in;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.notification.application.dto.NotificationTemplatePageQuery;
import org.v31bank.notification.domain.constant.NotificationChannel;
import org.v31bank.notification.domain.constant.NotificationTemplateStatus;
import org.v31bank.notification.domain.model.NotificationTemplate;

/**
 * Use cases for managing notification templates.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface NotificationTemplateUseCase {

	/**
	 * Add a notification template, provided its code is not already taken.
	 * @param code the code, unique
	 * @param name the display name
	 * @param channel the channel
	 * @return the outcome of the command
	 */
	ApiResponse<NotificationTemplate> create(String code, String name, NotificationChannel channel);

	Optional<NotificationTemplate> get(UUID id);

	/**
	 * Find a page of notification templates matching the filters carried by the query.
	 * @param query the filters and the pagination request
	 * @return the page of matching records
	 */
	PageResult<NotificationTemplate> page(NotificationTemplatePageQuery query);

	/**
	 * Update the notification template with the given identifier.
	 * @param id the record to update
	 * @param code the code, which must not belong to another record
	 * @param name the display name
	 * @param channel the channel
	 * @param status the new status, or {@code null} to leave it unchanged
	 * @return the outcome of the command
	 */
	ApiResponse<NotificationTemplate> update(UUID id, String code, String name, NotificationChannel channel,
			NotificationTemplateStatus status);

	/**
	 * Delete the notification template with the given identifier.
	 * @param id the record to delete
	 * @return the outcome of the command
	 */
	ApiResponse<NotificationTemplate> delete(UUID id);

}
