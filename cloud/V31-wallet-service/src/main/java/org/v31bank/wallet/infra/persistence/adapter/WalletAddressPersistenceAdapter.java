/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import org.v31bank.data.jpa.domain.PageResult;
import org.v31bank.wallet.application.dto.WalletAddressPageQuery;
import org.v31bank.wallet.application.port.out.WalletAddressPort;
import org.v31bank.wallet.domain.model.WalletAddress;
import org.v31bank.wallet.infra.persistence.jpa.JpaWalletAddressRepository;

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
