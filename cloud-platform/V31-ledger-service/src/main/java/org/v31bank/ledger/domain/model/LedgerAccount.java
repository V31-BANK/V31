package org.v31bank.ledger.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.v31bank.ledger.domain.constant.LedgerAccountType;
import org.v31bank.ledger.domain.constant.LedgerAccountStatus;
import org.v31bank.data.jpa.domain.BaseEntity;

/**
 * An account in the chart of accounts — the set of buckets every posting
 * lands in.
 * <p>
 * The chart is what makes a balance sheet possible: each account has a side it
 * normally sits on, and the whole ledger balances only because every posting
 * touches one of each.
 * @author Xander Wang
 * @since 0.2.0
 */
@Entity
@Table(name = "ledger_account",
        uniqueConstraints = @UniqueConstraint(name = "uk_ledger_account_code", columnNames = "code"))
public class LedgerAccount extends BaseEntity {

    /**
     * The account code, unique and quoted in every posting.
     */
    @Column(name = "code", length = 32, nullable = false)
    private String code;

    /**
     * The display name.
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * Which side of the balance sheet it belongs to.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private LedgerAccountType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LedgerAccountStatus status = LedgerAccountStatus.ACTIVE;

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

    public LedgerAccountType getType() {
        return this.type;
    }

    public void setType(LedgerAccountType type) {
        this.type = type;
    }

    public LedgerAccountStatus getStatus() {
        return this.status;
    }

    public void setStatus(LedgerAccountStatus status) {
        this.status = status;
    }

}
