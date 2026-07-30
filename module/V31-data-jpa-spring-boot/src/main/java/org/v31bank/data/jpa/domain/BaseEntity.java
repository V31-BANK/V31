package org.v31bank.data.jpa.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for all entities, providing a time-ordered UUIDv7 primary key and
 * audit fields populated automatically by Spring Data JPA auditing.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 64)
    private String lastModifiedBy;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCreatedBy() {
        return this.createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getLastModifiedBy() {
        return this.lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedDate() {
        return this.lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

}
