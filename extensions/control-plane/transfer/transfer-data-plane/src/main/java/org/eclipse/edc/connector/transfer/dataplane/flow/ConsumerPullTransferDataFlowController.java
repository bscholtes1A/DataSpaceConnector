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

package org.eclipse.edc.connector.transfer.dataplane.flow;

import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataAddressConstants;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;

public class ConsumerPullTransferDataFlowController implements DataFlowController {

    private final ConsumerPullEndpointDataReferenceResolver resolver;

    public ConsumerPullTransferDataFlowController(ConsumerPullEndpointDataReferenceResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return HTTP_PROXY.equals(dataRequest.getDestinationType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        var proxyCreationRequest = EndpointDataReferenceRequest.Builder.newInstance()
                .id(dataRequest.getId())
                .contractId(dataRequest.getContractId())
                .contentAddress(contentAddress)
                .build();

        return resolver.resolve(proxyCreationRequest)
                .map(this::createResponse)
                .map(StatusResult::success)
                .orElse(failure -> StatusResult.failure(FATAL_ERROR, "Failed to generate proxy: " + failure.getFailureDetail()));
    }

    private DataFlowResponse createResponse(EndpointDataReference edr) {
        return DataFlowResponse.Builder.newInstance().dataAddress(EndpointDataAddressConstants.from(edr)).build();
    }
}
