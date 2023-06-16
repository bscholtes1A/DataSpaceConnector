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

import org.eclipse.edc.connector.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.jetbrains.annotations.NotNull;

public class ConsumerPullTransferDataFlowController implements DataFlowController {

    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return EndpointDataReference.EDR_SIMPLE_TYPE.equals(contentAddress.getType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> initiateFlow(DataRequest dataRequest, DataAddress contentAddress, Policy policy) {
        return StatusResult.success(DataFlowResponse.Builder.newInstance()
                .dataAddress(contentAddress)
                .build());
    }
}
