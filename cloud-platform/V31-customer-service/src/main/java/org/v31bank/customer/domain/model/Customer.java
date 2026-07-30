package org.v31bank.customer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.data.jpa.domain.BaseEntity;

/**
 * A customer of the bank.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "customer", uniqueConstraints = @UniqueConstraint(name = "uk_customer_email", columnNames = "email"))
public class Customer extends BaseEntity {

    @Column(name = "email", length = 320, nullable = false)
    private String email;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public CustomerStatus getStatus() {
        return this.status;
    }

    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

}
