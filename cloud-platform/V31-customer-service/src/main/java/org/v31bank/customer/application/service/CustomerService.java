package org.v31bank.customer.application.service;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.v31bank.customer.application.dto.CustomerPageQuery;
import org.v31bank.customer.application.port.in.CustomerUseCase;
import org.v31bank.customer.application.port.out.CustomerPort;
import org.v31bank.customer.domain.constant.CustomerStatus;
import org.v31bank.customer.domain.model.Customer;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * Default {@link CustomerUseCase} implementation.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Service
@Transactional
public class CustomerService implements CustomerUseCase {

    private final CustomerPort customerRepository;

    public CustomerService(CustomerPort customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public Customer create(String email, String fullName) {
        Customer customer = new Customer();
        customer.setEmail(email);
        customer.setFullName(fullName);
        return this.customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> get(UUID id) {
        return this.customerRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Customer> page(CustomerPageQuery query) {
        return this.customerRepository.findPage(query.getEmail(), query.getStatus(), query);
    }

    @Override
    public Optional<Customer> update(UUID id, String email, String fullName, CustomerStatus status) {
        return this.customerRepository.findById(id).map((customer) -> {
            customer.setEmail(email);
            customer.setFullName(fullName);
            customer.setStatus(status);
            return this.customerRepository.save(customer);
        });
    }

    @Override
    public boolean delete(UUID id) {
        return this.customerRepository.findById(id).map((customer) -> {
            this.customerRepository.delete(customer);
            return true;
        }).orElse(false);
    }

}
