package org.v31bank.wallet.presentation.controller.v1;

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

import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.application.port.in.WalletAddressUseCase;
import org.v31bank.wallet.domain.model.WalletAddress;
import org.v31bank.wallet.presentation.dto.WalletAddressRequest;
import org.v31bank.wallet.presentation.dto.WalletAddressResponse;
import org.v31bank.core.response.ApiResponse;
import org.v31bank.core.response.CommonErrorCode;
import org.v31bank.core.response.ErrorCode;
import org.v31bank.core.response.PageResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing wallet addresss.
 * <p>
 * Commands come back from the use case as an {@link ApiResponse} already
 * carrying the verdict, so this layer converts the payload to the wire record and
 * puts the matching status on the response.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(WalletAddressController.PATH)
public class WalletAddressController {

    static final String PATH = "/api/v1/wallet-addresses";

    /**
     * Status for a code this service does not recognise, matching the default an
     * {@link ErrorCode} declares.
     */
    private static final int UNRECOGNISED_CODE_STATUS = HttpStatus.UNPROCESSABLE_CONTENT.value();

    private final WalletAddressUseCase walletAddressInputPort;

    public WalletAddressController(WalletAddressUseCase walletAddressInputPort) {
        this.walletAddressInputPort = walletAddressInputPort;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WalletAddressResponse>> create(@RequestBody WalletAddressRequest request) {
        ApiResponse<WalletAddress> result = this.walletAddressInputPort.create(request.address(), request.label(), request.asset());
        if (!result.success()) {
            return toResponseEntity(result);
        }
        return ResponseEntity.created(URI.create(PATH + "/" + result.data().getId()))
            .body(result.map(WalletAddressResponse::from));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletAddressResponse>> get(@PathVariable UUID id) {
        return this.walletAddressInputPort.get(id)
            .map((walletAddress) -> ResponseEntity.ok(ApiResponse.ok(WalletAddressResponse.from(walletAddress))))
            .orElseGet(() -> error(CommonErrorCode.NOT_FOUND, "No wallet address exists with id " + id));
    }

    /**
     * Return a page of records, newest first.
     * @param query the filters and the pagination request
     * @return the page of matching records
     */
    @GetMapping
    public ApiResponse<PageResponse<WalletAddressResponse>> page(WalletAddressPageQuery query) {
        PageResult<WalletAddressResponse> page = this.walletAddressInputPort.page(query).map(WalletAddressResponse::from);
        return ApiResponse
            .ok(PageResponse.of(page.getRecords(), page.getTotal(), page.getPageNumber(), page.getPageSize()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WalletAddressResponse>> update(@PathVariable UUID id, @RequestBody WalletAddressRequest request) {
        return toResponseEntity(this.walletAddressInputPort.update(id, request.address(), request.label(), request.asset(),
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
    public ResponseEntity<ApiResponse<WalletAddressResponse>> delete(@PathVariable UUID id) {
        return toResponseEntity(this.walletAddressInputPort.delete(id));
    }

    private static ResponseEntity<ApiResponse<WalletAddressResponse>> toResponseEntity(ApiResponse<WalletAddress> result) {
        return ResponseEntity.status(statusOf(result)).body(result.map(WalletAddressResponse::from));
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
