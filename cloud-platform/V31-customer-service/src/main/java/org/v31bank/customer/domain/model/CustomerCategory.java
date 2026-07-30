package org.v31bank.customer.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.data.jpa.domain.TreeEntity;

/**
 * A node in the customer category hierarchy, for example
 * {@code Retail > High Net Worth > Private Banking}.
 * <p>
 * The parent reference and the ordering among siblings are inherited from
 * {@link TreeEntity}; a node without a parent is a root category.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "customer_category",
        uniqueConstraints = @UniqueConstraint(name = "uk_customer_category_code", columnNames = "code"))
public class CustomerCategory extends TreeEntity<CustomerCategory> {

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CustomerCategoryStatus status = CustomerCategoryStatus.ENABLED;

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

    public CustomerCategoryStatus getStatus() {
        return this.status;
    }

    public void setStatus(CustomerCategoryStatus status) {
        this.status = status;
    }

}
