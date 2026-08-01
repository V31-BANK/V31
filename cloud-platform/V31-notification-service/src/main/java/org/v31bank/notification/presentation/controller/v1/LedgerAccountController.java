package org.v31bank.notification.presentation.controller.v1;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.notification.application.dto.LedgerAccountSummary;
import org.v31bank.notification.application.port.in.LedgerAccountUseCase;
import org.v31bank.notification.presentation.dto.LedgerAccountRequest;
import org.v31bank.notification.presentation.dto.LedgerAccountResponse;

/**
 * The chart of accounts, as this service exposes it over HTTP.
 * <p>
 * Every call here becomes a gRPC call to the ledger. Nothing is stored on this
 * side — the point is that a caller cannot tell: the envelope, the pagination and
 * the status codes are the same as if the ledger had answered directly.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(LedgerAccountController.PATH)
public class LedgerAccountController {

    static final String PATH = "/api/v1/ledger-accounts";

    private final LedgerAccountUseCase ledgerAccountInputPort;

    public LedgerAccountController(LedgerAccountUseCase ledgerAccountInputPort) {
        this.ledgerAccountInputPort = ledgerAccountInputPort;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> create(@RequestBody LedgerAccountRequest request) {
        LedgerAccountSummary created = this.ledgerAccountInputPort.create(request.code(), request.name(),
                request.type());
        return ResponseEntity.created(URI.create(PATH + "/" + created.id()))
            .body(ApiResponse.ok(LedgerAccountResponse.from(created)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> get(@PathVariable UUID id) {
        return this.ledgerAccountInputPort.get(id)
            .map((summary) -> ResponseEntity.ok(ApiResponse.ok(LedgerAccountResponse.from(summary))))
            .orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No ledger account exists with id " + id));
    }

    /**
     * Return a page of accounts, newest first, as the ledger ordered them.
     * @param pageNumber the one-based page to fetch
     * @param pageSize the page size
     * @param code fragment matched against the code, or absent for no filter
     * @return the page of matching accounts
     */
    @GetMapping
    public ApiResponse<PageResponse<LedgerAccountResponse>> page(
            @RequestParam(defaultValue = "1") int pageNumber, @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String code) {
        return ApiResponse.ok(this.ledgerAccountInputPort.page(pageNumber, pageSize, code)
            .map(LedgerAccountResponse::from));
    }

    @PutMapping("/{id}")
    public ApiResponse<LedgerAccountResponse> update(@PathVariable UUID id,
            @RequestBody LedgerAccountRequest request) {
        return ApiResponse.ok(LedgerAccountResponse.from(this.ledgerAccountInputPort.update(id, request.code(),
                request.name(), request.type(), request.status())));
    }

    /**
     * Answer a delete with the envelope carrying the account that was removed,
     * rather than a bare {@code 204}, so that this endpoint is parsed like every
     * other one.
     * @param id the account to delete
     * @return the response to send
     */
    @DeleteMapping("/{id}")
    public ApiResponse<LedgerAccountResponse> delete(@PathVariable UUID id) {
        return ApiResponse.ok(LedgerAccountResponse.from(this.ledgerAccountInputPort.delete(id)));
    }

    private static <T> ResponseEntity<ApiResponse<T>> error(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.httpStatus()).body(ApiResponse.error(errorCode, message));
    }

}
