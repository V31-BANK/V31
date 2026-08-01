package org.v31bank.ledger.presentation.controller.v1;

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

import org.v31bank.ledger.application.dto.LedgerAccountPageQuery;
import org.v31bank.ledger.application.port.in.LedgerAccountUseCase;
import org.v31bank.ledger.domain.model.LedgerAccount;
import org.v31bank.ledger.presentation.dto.LedgerAccountRequest;
import org.v31bank.ledger.presentation.dto.LedgerAccountResponse;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing ledger accounts.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already
 * carrying the verdict, so this layer converts the payload to the wire record and
 * puts the matching status on the response.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(LedgerAccountController.PATH)
public class LedgerAccountController {

    static final String PATH = "/api/v1/ledger-accounts";

    /**
     * Status for a code this service does not recognise, matching the default an
     * {@link ErrorCode} declares.
     */
    private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

    private final LedgerAccountUseCase ledgerAccountInputPort;

    public LedgerAccountController(LedgerAccountUseCase ledgerAccountInputPort) {
        this.ledgerAccountInputPort = ledgerAccountInputPort;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> create(@RequestBody LedgerAccountRequest request) {
        ApiResponse<LedgerAccount> result = this.ledgerAccountInputPort.create(request.code(), request.name(), request.type());
        if (!result.success()) {
            return toResponseEntity(result);
        }
        return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
            .body(result.map(LedgerAccountResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> get(@PathVariable UUID id) {
        return this.ledgerAccountInputPort.get(id)
            .map((ledgerAccount) -> ResponseEntity.ok(ApiResponse.ok(LedgerAccountResponse.from(ledgerAccount))))
            .orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No ledger account exists with id " + id));
    }

    /**
     * Return a page of records, newest first.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    @GetMapping
    public ApiResponse<PageResponse<LedgerAccountResponse>> page(LedgerAccountPageQuery query) {
        PageResult<LedgerAccountResponse> page = this.ledgerAccountInputPort.page(query).map(LedgerAccountResponse::from);
        return ApiResponse
            .ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> update(@PathVariable UUID id, @RequestBody LedgerAccountRequest request) {
        return toResponseEntity(this.ledgerAccountInputPort.update(id, request.code(), request.name(), request.type(),
                request.status()));
    }

    /**
     * Answer a delete with the envelope carrying the record that was removed,
     * rather than a bare {@code 204}, so that this endpoint is parsed like every
     * other one.
     * @param id the record to delete
     * @return the response to send
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerAccountResponse>> delete(@PathVariable UUID id) {
        return toResponseEntity(this.ledgerAccountInputPort.delete(id));
    }

    private static ResponseEntity<ApiResponse<LedgerAccountResponse>> toResponseEntity(ApiResponse<LedgerAccount> result) {
        return ResponseEntity.status(statusOf(result)).body(result.map(LedgerAccountResponse::from));
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
