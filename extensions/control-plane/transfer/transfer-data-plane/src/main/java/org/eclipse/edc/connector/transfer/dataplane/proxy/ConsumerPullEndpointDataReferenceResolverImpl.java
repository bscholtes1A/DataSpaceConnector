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

package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceAdapter;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.util.ArrayList;
import java.util.List;

public class ConsumerPullEndpointDataReferenceResolverImpl implements ConsumerPullEndpointDataReferenceResolver {

    private final List<ConsumerPullEndpointDataReferenceAdapter> adapters = new ArrayList<>();

    @Override
    public void registerAdapter(ConsumerPullEndpointDataReferenceAdapter adapter) {
        adapters.add(adapter);
    }

    @Override
    public Result<EndpointDataReference> resolve(EndpointDataReferenceRequest request) {
        return adapters.stream()
                .filter(adapter -> adapter.canHandle(request))
                .findFirst()
                .map(adapter -> adapter.convert(request))
                .orElse(Result.failure("Failed to find endpoint data reference adapter for request: " + request.toString()));
    }

}
