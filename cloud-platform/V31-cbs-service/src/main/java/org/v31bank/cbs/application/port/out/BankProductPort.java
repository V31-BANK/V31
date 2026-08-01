package org.v31bank.cbs.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.cbs.application.dto.BankProductPageQuery;
import org.v31bank.cbs.domain.model.BankProduct;
import org.v31bank.core.response.PageResponse;

/**
 * Output port for {@link BankProduct} persistence, implemented by the
 * infrastructure layer.
 * <p>
 * The code claim is a port operation rather than a check the application makes
 * and then hopes holds. A relational store gives uniqueness away for free with a
 * constraint, and both other services in this platform lean on that: they ask
 * whether a code exists, then write, and the database is what stops two callers
 * that both got "no". Valkey has no constraints, so the claim itself has to be
 * the atomic step, and the port says so instead of pretending otherwise.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface BankProductPort {

    /**
     * Take the code for a product, if no other product holds it.
     * <p>
     * Atomic: of two callers claiming the same code at the same moment, exactly
     * one is told it succeeded.
     * @param code the code to take
     * @param id the product taking it
     * @return {@code true} if the code was free and now belongs to that product
     */
    boolean claimCode(String code, UUID id);

    /**
     * Give a code back, so another product may take it.
     * @param code the code to release
     */
    void releaseCode(String code);

    /**
     * Write the product and bring the indexes that page over it up to date.
     * @param product the product to write, carrying an identifier
     * @return the product as it now stands
     */
    BankProduct save(BankProduct product);

    Optional<BankProduct> findById(UUID id);

    /**
     * Find a page of products, newest first.
     * @param query the filters and the pagination request
     * @return the page of matching products
     */
    PageResponse<BankProduct> findPage(BankProductPageQuery query);

    void delete(BankProduct product);

}
