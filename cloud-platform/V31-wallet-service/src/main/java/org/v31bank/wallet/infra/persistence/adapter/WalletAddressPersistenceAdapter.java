package org.v31bank.wallet.infra.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.application.port.out.WalletAddressPort;
import org.v31bank.wallet.domain.model.WalletAddress;
import org.v31bank.wallet.infra.persistence.jpa.JpaWalletAddressRepository;
import org.v31bank.data.jpa.domain.PageResult;

/**
 * {@link WalletAddressPort} adapter backed by Spring Data JPA.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
@Repository
public class WalletAddressPersistenceAdapter implements WalletAddressPort {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdDate");

    private final JpaWalletAddressRepository jpaRepository;

    public WalletAddressPersistenceAdapter(JpaWalletAddressRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public WalletAddress save(WalletAddress walletAddress) {
        return this.jpaRepository.save(walletAddress);
    }

    @Override
    public Optional<WalletAddress> findById(UUID id) {
        return this.jpaRepository.findById(id);
    }

    @Override
    public PageResult<WalletAddress> findPage(WalletAddressPageQuery query) {
        Specification<WalletAddress> spec = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getAddress())) {
                predicates.add(cb.like(cb.lower(root.get("address")), "%" + query.getAddress().toLowerCase() + "%"));
            }
            if (query.getAsset() != null) {
                predicates.add(cb.equal(root.get("asset"), query.getAsset()));
            }
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResult.of(this.jpaRepository.findAll(spec, query.toPageable(NEWEST_FIRST)));
    }

    @Override
    public boolean existsByAddress(String address) {
        return this.jpaRepository.existsByAddress(address);
    }

    @Override
    public void delete(WalletAddress walletAddress) {
        this.jpaRepository.delete(walletAddress);
    }

}
