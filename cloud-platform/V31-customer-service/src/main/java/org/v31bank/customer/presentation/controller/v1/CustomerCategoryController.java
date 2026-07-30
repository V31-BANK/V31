package org.v31bank.customer.presentation.controller.v1;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import org.v31bank.customer.application.dto.CustomerCategoryPageQuery;
import org.v31bank.customer.application.dto.CustomerCategoryResult;
import org.v31bank.customer.application.port.in.CustomerCategoryUseCase;
import org.v31bank.customer.domain.constant.CustomerCategoryStatus;
import org.v31bank.customer.domain.model.CustomerCategory;
import org.v31bank.customer.presentation.dto.CustomerCategoryRequest;
import org.v31bank.customer.presentation.dto.CustomerCategoryResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing the customer category hierarchy.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping(CustomerCategoryController.PATH)
public class CustomerCategoryController {

    static final String PATH = "/api/v1/customer-categories";

    private final CustomerCategoryUseCase customerCategoryInputPort;

    public CustomerCategoryController(CustomerCategoryUseCase customerCategoryInputPort) {
        this.customerCategoryInputPort = customerCategoryInputPort;
    }

    @PostMapping
    public ResponseEntity<Object> create(@RequestBody CustomerCategoryRequest request) {
        CustomerCategoryResult result = this.customerCategoryInputPort.create(request.code(), request.name(),
                request.parentId(), request.sortOrder(), request.status());
        if (!(result instanceof CustomerCategoryResult.Success(CustomerCategory category))) {
            return toResponse(result);
        }
        return ResponseEntity.created(URI.create(PATH + "/" + category.getId()))
            .body(CustomerCategoryResponse.from(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerCategoryResponse> get(@PathVariable UUID id) {
        return this.customerCategoryInputPort.get(id)
            .map((category) -> ResponseEntity.ok(CustomerCategoryResponse.from(category)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Return a flat page of categories across the hierarchy. Pass
     * {@code rootOnly} or {@code parentId} to page through a single level.
     * @param query the filters and the pagination request
     * @return the page of matching categories
     */
    @GetMapping
    public PageResult<CustomerCategoryResponse> page(CustomerCategoryPageQuery query) {
        return this.customerCategoryInputPort.page(query).map(CustomerCategoryResponse::from);
    }

    /**
     * Return the hierarchy as nested nodes. Not paginated: a page of a tree would
     * cut arbitrary branches, so callers that need pagination use the flat
     * endpoint with {@code parentId} and expand one level at a time.
     * @param rootId the subtree to return, or {@code null} for every root
     * @param status status to match, or {@code null} for no filter
     * @return the root nodes, with descendants attached
     */
    @GetMapping("/tree")
    public List<CustomerCategoryResponse> tree(@RequestParam(required = false) UUID rootId,
            @RequestParam(required = false) CustomerCategoryStatus status) {
        return this.customerCategoryInputPort.tree(rootId, status)
            .stream()
            .map(CustomerCategoryResponse::fromTree)
            .toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> update(@PathVariable UUID id, @RequestBody CustomerCategoryRequest request) {
        return toResponse(this.customerCategoryInputPort.update(id, request.code(), request.name(), request.parentId(),
                request.sortOrder(), request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable UUID id) {
        CustomerCategoryResult result = this.customerCategoryInputPort.delete(id);
        return (result instanceof CustomerCategoryResult.Success) ? ResponseEntity.noContent().build()
                : toResponse(result);
    }

    /**
     * Translate a command outcome into an HTTP response, reporting every failure
     * as a {@link ProblemDetail}.
     * @param result the outcome to translate
     * @return the response to send
     */
    private static ResponseEntity<Object> toResponse(CustomerCategoryResult result) {
        return switch (result) {
            case CustomerCategoryResult.Success(CustomerCategory category) ->
                ResponseEntity.ok(CustomerCategoryResponse.from(category));
            case CustomerCategoryResult.NotFound(UUID id) ->
                problem(HttpStatus.NOT_FOUND, "No customer category exists with id " + id);
            case CustomerCategoryResult.ParentNotFound(UUID parentId) ->
                problem(HttpStatus.UNPROCESSABLE_CONTENT, "No parent customer category exists with id " + parentId);
            case CustomerCategoryResult.DuplicateCode(String code) ->
                problem(HttpStatus.CONFLICT, "Customer category code '" + code + "' is already in use");
            case CustomerCategoryResult.CyclicParent(UUID id, UUID parentId) -> problem(HttpStatus.CONFLICT,
                    "Customer category " + id + " cannot be moved under itself or one of its descendants " + parentId);
            case CustomerCategoryResult.HasChildren(UUID id) ->
                problem(HttpStatus.CONFLICT, "Customer category " + id + " still has children and cannot be deleted");
        };
    }

    private static ResponseEntity<Object> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }

}
