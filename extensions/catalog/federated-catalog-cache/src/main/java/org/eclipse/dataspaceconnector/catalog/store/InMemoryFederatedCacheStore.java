/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An ephemeral in-memory cache store.
 */
public class InMemoryFederatedCacheStore implements FederatedCacheStore {

    private final Map<String, CachedContractOffer> cache = new ConcurrentHashMap<>();
    private final CriterionConverter<Predicate<ContractOffer>> converter;
    private final LockManager lockManager;
    private final Clock clock;

    public InMemoryFederatedCacheStore(CriterionConverter<Predicate<ContractOffer>> converter, LockManager lockManager, Clock clock) {
        this.converter = converter;
        this.lockManager = lockManager;
        this.clock = clock;
    }

    @Override
    public void save(ContractOffer contractOffer) {
        lockManager.writeLock(() -> cache.put(contractOffer.getAsset().getId(), new CachedContractOffer(clock.instant(), contractOffer)));
    }

    @Override
    public Collection<ContractOffer> query(List<Criterion> query) {
        //AND all predicates
        var rootPredicate = query.stream().map(converter::convert).reduce(x -> true, Predicate::and);
        return lockManager.readLock(() -> cache.values().stream().map(i -> i.contractOffer).filter(rootPredicate).collect(Collectors.toList()));
    }

    @Override
    public void deleteExpired(Duration ttl) {
        var limit = clock.instant().minusSeconds(ttl.toSeconds());
        lockManager.writeLock(() -> cache.values().removeIf(i -> i.createdAt.isBefore(limit)));
    }

    private static final class CachedContractOffer {
        private final Instant createdAt;
        private final ContractOffer contractOffer;

        private CachedContractOffer(Instant createdAt, ContractOffer contractOffer) {
            this.createdAt = createdAt;
            this.contractOffer = contractOffer;
        }
    }
}
