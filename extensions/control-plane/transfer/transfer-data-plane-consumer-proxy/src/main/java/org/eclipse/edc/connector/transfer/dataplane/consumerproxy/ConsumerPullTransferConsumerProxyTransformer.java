/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.consumerproxy;

import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformer;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataAddressConstants;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import static java.lang.String.format;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.EDC_CONTRACT_ID;

/**
 * Transforms {@link EndpointDataReference} returned by the provider Control Plane so that
 * the consumer Data Plane becomes a Data Proxy to query data.
 * This implies that the data query should first hit the consumer Data Plane, which then forward the
 * call to the provider Data Plane, which finally reach the actual data source.
 */
public class ConsumerPullTransferConsumerProxyTransformer implements EndpointDataReferenceTransformer {

    private final ConsumerPullEndpointDataReferenceResolver resolver;

    public ConsumerPullTransferConsumerProxyTransformer(ConsumerPullEndpointDataReferenceResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean canHandle(@NotNull EndpointDataReference edr) {
        return true;
    }

    /**
     * Convert the consumer Data Plane into a proxy for querying the provider Data Plane.
     *
     * @param edr provider {@link EndpointDataReference}
     * @return consumer {@link EndpointDataReference}
     */
    @Override
    public Result<EndpointDataReference> transform(@NotNull EndpointDataReference edr) {
        var contractId = edr.getProperties().get(EDC_CONTRACT_ID);
        if (contractId == null) {
            return Result.failure(format("Cannot transform endpoint data reference with id %s as contract id is missing", edr.getId()));
        }

        var request = EndpointDataReferenceRequest.Builder.newInstance()
                .id(edr.getId())
                .contractId(contractId)
                .contentAddress(EndpointDataAddressConstants.from(edr))
                .build();

        return resolver.resolve(request);
    }
}
