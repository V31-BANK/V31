package org.v31bank.customer.presentation.controller.v1;

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
import org.springframework.web.bind.annotation.RestController;

import org.v31bank.customer.application.dto.CustomerPageQuery;
import org.v31bank.customer.application.port.in.CustomerUseCase;
import org.v31bank.customer.domain.model.Customer;
import org.v31bank.customer.presentation.dto.CustomerRequest;
import org.v31bank.customer.presentation.dto.CustomerResponse;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * REST endpoints for managing customers.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerUseCase customerInputPort;

    public CustomerController(CustomerUseCase customerInputPort) {
        this.customerInputPort = customerInputPort;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@RequestBody CustomerRequest request) {
        Customer customer = this.customerInputPort.create(request.email(), request.fullName());
        URI location = URI.create("/api/v1/customers/" + customer.getId());
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> get(@PathVariable UUID id) {
        return this.customerInputPort.get(id)
            .map((customer) -> ResponseEntity.ok(CustomerResponse.from(customer)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public PageResult<CustomerResponse> page(CustomerPageQuery query) {
        return this.customerInputPort.page(query).map(CustomerResponse::from);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable UUID id, @RequestBody CustomerRequest request) {
        return this.customerInputPort.update(id, request.email(), request.fullName(), request.status())
            .map((customer) -> ResponseEntity.ok(CustomerResponse.from(customer)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return this.customerInputPort.delete(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

}
