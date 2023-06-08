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
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.flow;

import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerPullTransferDataFlowControllerTest {

    private ConsumerPullEndpointDataReferenceResolver resolverMock;
    private ConsumerPullTransferDataFlowController flowController;

    @BeforeEach
    void setUp() {
        resolverMock = mock(ConsumerPullEndpointDataReferenceResolver.class);
        flowController = new ConsumerPullTransferDataFlowController(resolverMock);
    }

    @Test
    void verifyCanHandle() {
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType(HTTP_PROXY).build(), null)).isTrue();
        assertThat(flowController.canHandle(DataRequest.Builder.newInstance().destinationType("not-http-proxy").build(), null)).isFalse();
    }

    @Test
    void verifyResolveSuccess() {
        var request = createDataRequest();
        var dataAddress = testDataAddress();
        var edr = createEndpointDataReference();
        var proxyUrl = "proxy.test.url";

        var requestCaptor = ArgumentCaptor.forClass(EndpointDataReferenceRequest.class);

        when(resolverMock.resolve(any())).thenReturn(Result.success(edr));

        var result = flowController.initiateFlow(request, dataAddress, Policy.Builder.newInstance().build());

        verify(resolverMock).resolve(requestCaptor.capture());

        assertThat(result.succeeded()).isTrue();

        var dataFlowResponse = result.getContent();
        assertThat(dataFlowResponse.getDataAddress()).isNotNull().satisfies(address -> {
            assertThat(address.getType()).isEqualTo(EndpointDataReference.EDR_SIMPLE_TYPE);
            assertThat(address.getProperty(EndpointDataReference.ENDPOINT)).isEqualTo(edr.getEndpoint());
            assertThat(address.getProperty(EndpointDataReference.AUTH_KEY)).isEqualTo(edr.getAuthKey());
            assertThat(address.getProperty(EndpointDataReference.ID)).isEqualTo(edr.getId());
            assertThat(address.getProperty(EndpointDataReference.AUTH_CODE)).isEqualTo(edr.getAuthCode());
            assertThat(address.getProperties()).containsAllEntriesOf(edr.getProperties());
        });

        var capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getId()).isEqualTo(request.getId());
        assertThat(capturedRequest.getContentAddress()).isEqualTo(dataAddress);
        assertThat(capturedRequest.getContractId()).isEqualTo(request.getContractId());
        assertThat(capturedRequest.getProperties()).isEmpty();
    }

    @Test
    void verifyReturnBadResultIfResolveFails() {
        var request = createDataRequest();
        var dataAddress = testDataAddress();
        var errorMsg = "test-errormsg";

        when(resolverMock.resolve(any())).thenReturn(Result.failure(errorMsg));

        var result = flowController.initiateFlow(request, dataAddress, Policy.Builder.newInstance().build());

        verify(resolverMock).resolve(any());

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureMessages()).allSatisfy(s -> assertThat(s).contains(errorMsg));
    }

    private DataAddress testDataAddress() {
        return DataAddress.Builder.newInstance().type("test-type").build();
    }

    private DataRequest createDataRequest() {
        return DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .protocol("protocol")
                .contractId(UUID.randomUUID().toString())
                .assetId(UUID.randomUUID().toString())
                .connectorAddress("test.connector.address")
                .processId(UUID.randomUUID().toString())
                .destinationType(HTTP_PROXY)
                .build();
    }

    private EndpointDataReference createEndpointDataReference() {
        return EndpointDataReference.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .endpoint("test.endpoint.url")
                .build();
    }

}
