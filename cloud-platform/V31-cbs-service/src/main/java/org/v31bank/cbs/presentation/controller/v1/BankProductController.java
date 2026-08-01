package org.v31bank.cbs.presentation.controller.v1;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.cbs.application.dto.BankProductPageQuery;
import org.v31bank.cbs.application.port.in.BankProductUseCase;
import org.v31bank.cbs.domain.model.BankProduct;
import org.v31bank.cbs.presentation.dto.BankProductRequest;
import org.v31bank.cbs.presentation.dto.BankProductResponse;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;

/**
 * REST endpoints for managing the bank product catalogue.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already carrying
 * the verdict, so this layer converts the payload from the domain model to the
 * wire record and puts the matching status on the response.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(BankProductController.PATH)
public class BankProductController {

    static final String PATH = "/api/v1/bank-products";

    /**
     * Status for a code this service does not recognise, matching the default an
     * {@link ErrorCode} declares.
     */
    private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

    private final BankProductUseCase bankProductInputPort;

    public BankProductController(BankProductUseCase bankProductInputPort) {
        this.bankProductInputPort = bankProductInputPort;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankProductResponse>> create(@RequestBody BankProductRequest request) {
        ApiResponse<BankProduct> result = this.bankProductInputPort.create(request.code(), request.name(),
                request.category(), request.interestRate());
        if (!result.success()) {
            return toResponseEntity(result);
        }
        return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
            .body(result.map(BankProductResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankProductResponse>> get(@PathVariable UUID id) {
        return this.bankProductInputPort.get(id)
            .map((product) -> ResponseEntity.ok(ApiResponse.ok(BankProductResponse.from(product))))
            .orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No bank product exists with id " + id));
    }

    /**
     * Return a page of products, newest first. Pass {@code category} or
     * {@code status} to narrow it.
     * @param query the filters and the pagination request
     * @return the page of matching products
     */
    @GetMapping
    public ApiResponse<PageResponse<BankProductResponse>> page(BankProductPageQuery query) {
        return ApiResponse.ok(this.bankProductInputPort.page(query).map(BankProductResponse::from));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankProductResponse>> update(@PathVariable UUID id,
            @RequestBody BankProductRequest request) {
        return toResponseEntity(this.bankProductInputPort.update(id, request.code(), request.name(),
                request.category(), request.status(), request.interestRate()));
    }

    /**
     * Answer a delete with the envelope carrying the product that was removed,
     * rather than a bare {@code 204}, so that this endpoint is parsed like every
     * other one.
     * @param id the product to delete
     * @return the response to send
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BankProductResponse>> delete(@PathVariable UUID id) {
        return toResponseEntity(this.bankProductInputPort.delete(id));
    }

    private static ResponseEntity<ApiResponse<BankProductResponse>> toResponseEntity(
            ApiResponse<BankProduct> result) {
        return ResponseEntity.status(statusOf(result)).body(result.map(BankProductResponse::from));
    }

    /**
     * Recover the HTTP status belonging to an outcome. The envelope carries the
     * code as text, since that is what goes on the wire, so the status has to be
     * looked back up rather than read off it.
     * @param result the outcome to place
     * @return the status to answer with
     */
    private static int statusOf(ApiResponse<?> result) {
        if (result.success()) {
            return HttpStatus.OK.value();
        }
        return CommonErrorCode.find(result.code()).map(ErrorCode::httpStatus).orElse(UNRECOGNISED_CODE_STATUS);
    }

    private static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.error(errorCode, message));
    }

}
