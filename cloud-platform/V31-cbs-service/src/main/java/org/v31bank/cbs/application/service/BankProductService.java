package org.v31bank.cbs.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import org.v31bank.cbs.application.dto.BankProductPageQuery;
import org.v31bank.cbs.application.port.in.BankProductUseCase;
import org.v31bank.cbs.application.port.out.BankProductPort;
import org.v31bank.cbs.domain.constant.BankProductCategory;
import org.v31bank.cbs.domain.constant.BankProductStatus;
import org.v31bank.cbs.domain.model.BankProduct;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.core.util.Uuids;

/**
 * Default {@link BankProductUseCase} implementation.
 * <p>
 * There is no {@code @Transactional} here, unlike the services backed by
 * PostgreSQL. Valkey has no rollback: once a command has run it has happened, and
 * a second command failing does not undo the first. What the adapter can promise
 * is that the writes making up one change are sent as a unit and not interleaved
 * with anyone else's, and that the code claim is a single atomic step. Annotating
 * this class would suggest a guarantee that does not exist.
 * <p>
 * The identifier is issued here rather than by the store, because the code has to
 * be claimed for a product before that product is written, and a claim needs
 * something to point at.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
public class BankProductService implements BankProductUseCase {

    /**
     * What a product code may look like. Constrained because the code becomes
     * part of a Valkey key, and a key whose segments are not bounded is a key a
     * caller can shape.
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z0-9][A-Z0-9_-]{1,31}");

    private final BankProductPort bankProductRepository;

    public BankProductService(BankProductPort bankProductRepository) {
        this.bankProductRepository = bankProductRepository;
    }

    @Override
    public ApiResponse<BankProduct> create(String code, String name, BankProductCategory category,
            BigDecimal interestRate) {
        ApiResponse<BankProduct> rejected = validate(code, category);
        if (rejected != null) {
            return rejected;
        }
        UUID id = Uuids.timeOrdered();
        if (!this.bankProductRepository.claimCode(code, id)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Bank product code '" + code + "' is already in use");
        }
        Instant now = Instant.now();
        BankProduct product = new BankProduct();
        product.setId(id);
        product.setCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setInterestRate(interestRate);
        product.setCreatedDate(now);
        product.setLastModifiedDate(now);
        return ApiResponse.ok(this.bankProductRepository.save(product));
    }

    @Override
    public Optional<BankProduct> get(UUID id) {
        return this.bankProductRepository.findById(id);
    }

    @Override
    public PageResponse<BankProduct> page(BankProductPageQuery query) {
        return this.bankProductRepository.findPage(query);
    }

    @Override
    public ApiResponse<BankProduct> update(UUID id, String code, String name, BankProductCategory category,
            BankProductStatus status, BigDecimal interestRate) {
        ApiResponse<BankProduct> rejected = validate(code, category);
        if (rejected != null) {
            return rejected;
        }
        Optional<BankProduct> found = this.bankProductRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No bank product exists with id " + id);
        }
        BankProduct product = found.get();
        String previousCode = product.getCode();
        if (!previousCode.equals(code) && !this.bankProductRepository.claimCode(code, id)) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Bank product code '" + code + "' is already in use");
        }
        product.setCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setInterestRate(interestRate);
        product.setLastModifiedDate(Instant.now());
        if (status != null) {
            product.setStatus(status);
        }
        BankProduct saved = this.bankProductRepository.save(product);
        if (!previousCode.equals(code)) {
            this.bankProductRepository.releaseCode(previousCode);
        }
        return ApiResponse.ok(saved);
    }

    /**
     * Remove a product, provided nothing was ever sold against it.
     * <p>
     * Only a draft can go. A product that has been active may have accounts
     * opened against it, and those accounts refer to it for their terms — the
     * rate they earn, the notice they need. Withdrawing it stops new accounts;
     * deleting it would leave the existing ones describing terms nobody can
     * produce.
     */
    @Override
    public ApiResponse<BankProduct> delete(UUID id) {
        Optional<BankProduct> found = this.bankProductRepository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(CommonErrorCode.NOT_FOUND, "No bank product exists with id " + id);
        }
        BankProduct product = found.get();
        if (product.getStatus() != BankProductStatus.DRAFT) {
            return ApiResponse.error(CommonErrorCode.CONFLICT, "Bank product " + id
                    + " has been offered and cannot be deleted; withdraw it instead");
        }
        this.bankProductRepository.delete(product);
        this.bankProductRepository.releaseCode(product.getCode());
        return ApiResponse.ok(product);
    }

    /**
     * Check what the store cannot check for itself.
     * @param code the code to check the shape of
     * @param category the category, which a product cannot be without
     * @return the refusal to report, or {@code null} when there is nothing wrong
     */
    private static ApiResponse<BankProduct> validate(String code, BankProductCategory category) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED,
                    "A bank product code is 2 to 32 characters of A-Z, 0-9, '_' or '-'");
        }
        if (category == null) {
            return ApiResponse.error(CommonErrorCode.VALIDATION_FAILED, "A bank product needs a category");
        }
        return null;
    }

}
