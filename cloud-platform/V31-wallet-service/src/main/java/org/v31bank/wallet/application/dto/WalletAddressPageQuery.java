package org.v31bank.wallet.application.dto;

import org.v31bank.wallet.domain.constant.WalletAddressStatus;
import org.v31bank.data.jpa.domain.PageQuery;

/**
 * Paginated wallet address query with optional filters.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public class WalletAddressPageQuery extends PageQuery {

    /**
     * Address fragment to match, case-insensitive.
     */
    private String address;

    /**
     * Asset to match.
     */
    private String asset;

    /**
     * Status to match.
     */
    private WalletAddressStatus status;

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAsset() {
        return this.asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public WalletAddressStatus getStatus() {
        return this.status;
    }

    public void setStatus(WalletAddressStatus status) {
        this.status = status;
    }

}
