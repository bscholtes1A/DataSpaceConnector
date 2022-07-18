/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.defaults.store;


import com.github.javafaker.Faker;
import org.assertj.core.api.ThrowingConsumer;
import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.catalog.store.InMemoryFederatedCacheStore;
import org.eclipse.dataspaceconnector.common.concurrency.LockManager;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryFederatedCacheStoreTest {

    private static final Faker FAKER = new Faker();

    private final Clock clock = mock(Clock.class);

    private FederatedCacheStore store;

    @BeforeEach
    public void setUp() {
        CriterionConverter<Predicate<ContractOffer>> converter = criterion -> offer -> true;
        store = new InMemoryFederatedCacheStore(converter, new LockManager(new ReentrantReadWriteLock()), clock);
    }

    @Test
    void queryCacheContainingOneElementWithNoCriterion_shouldReturnUniqueElement() {
        var contractOffer = createContractOffer();

        store.save(contractOffer);

        var result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(1)
                .allSatisfy(new ContractOfferConsumer(contractOffer));
    }

    @Test
    void queryCacheAfterInsertingSameAssetTwice_shouldReturnLastInsertedContractOfferOnly() {
        var contractOffer1 = createContractOffer();
        var contractOffer2 = createContractOffer(contractOffer1.getAsset());

        store.save(contractOffer1);
        store.save(contractOffer2);

        var result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(1)
                .allSatisfy(new ContractOfferConsumer(contractOffer2));
    }

    @Test
    void queryCacheContainingTwoDistinctAssets_shouldReturnBothContractOffers() {
        var contractOffer1 = createContractOffer();
        var contractOffer2 = createContractOffer();

        store.save(contractOffer1);
        store.save(contractOffer2);

        var result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(2)
                .anySatisfy(new ContractOfferConsumer(contractOffer1))
                .anySatisfy(new ContractOfferConsumer(contractOffer2));
    }

    @Test
    void deleteExpired() {
        var ttlSeconds = FAKER.random().nextInt(30, 60);

        var contractOffer1 = createContractOffer();
        var contractOffer2 = createContractOffer();
        var contractOffer3 = createContractOffer();

        var now = Instant.now();
        when(clock.instant()).thenReturn(
                now.plusSeconds(FAKER.random().nextInt(100)),
                now.minusSeconds(2L * ttlSeconds),
                now.minusSeconds(ttlSeconds / 2));

        store.save(contractOffer1);
        store.save(contractOffer2);
        store.save(contractOffer3);

        assertThat(store.query(List.of())).hasSize(3);

        store.deleteExpired(Duration.ofSeconds(ttlSeconds));

        assertThat(store.query(List.of()))
                .hasSize(2)
                .anySatisfy(new ContractOfferConsumer(contractOffer1))
                .anySatisfy(new ContractOfferConsumer(contractOffer3));
    }

    private static ContractOffer createContractOffer() {
        var asset = Asset.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .build();
        return createContractOffer(asset);
    }

    private static ContractOffer createContractOffer(Asset asset) {
        return ContractOffer.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .asset(asset)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    private static final class ContractOfferConsumer implements ThrowingConsumer<ContractOffer> {

        private final ContractOffer expected;

        private ContractOfferConsumer(ContractOffer expected) {
            this.expected = expected;
        }

        @Override
        public void acceptThrows(ContractOffer co) throws Throwable {
            assertThat(co.getId()).isEqualTo(expected.getId());
            assertThat(co.getAsset().getId()).isEqualTo(expected.getAsset().getId());
        }
    }
}