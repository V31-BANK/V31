package org.v31bank.transfer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import org.v31bank.transfer.application.dto.TransferLimitPageQuery;
import org.v31bank.transfer.domain.model.TransferLimit;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Output port for {@link TransferLimit} persistence, implemented by the infrastructure
 * layer.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public interface TransferLimitPort {

    TransferLimit save(TransferLimit transferLimit);

    Optional<TransferLimit> findById(UUID id);

    /**
     * Find a page of records matching the filters carried by the query.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    PageResult<TransferLimit> findPage(TransferLimitPageQuery query);

    /**
     * Whether any record already uses the given code.
     * @param code the value to check
     * @return {@code true} if it is taken
     */
    boolean existsByCode(String code);

    void delete(TransferLimit transferLimit);

}
