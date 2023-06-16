/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.provision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

/**
 * An OAuth2 resource definition
 */
@JsonDeserialize(builder = ConsumerPullDataPlaneProxyResourceDefinition.Builder.class)
@JsonTypeName("dataspaceconnector:consumerpulldataplaneproxyresourcedefinition")
public class ConsumerPullDataPlaneProxyResourceDefinition extends ResourceDefinition {

    private String contractId;
    private DataAddress dataAddress;

    private ConsumerPullDataPlaneProxyResourceDefinition() {
    }

    public DataAddress getDataAddress() {
        return dataAddress;
    }

    @Override
    public Builder toBuilder() {
        return initializeBuilder(new Builder())
                .dataAddress(dataAddress)
                .contractId(contractId);
    }

    @NotNull
    public String getContractId() {
        return contractId;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ResourceDefinition.Builder<ConsumerPullDataPlaneProxyResourceDefinition, Builder> {

        private Builder() {
            super(new ConsumerPullDataPlaneProxyResourceDefinition());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder dataAddress(DataAddress dataAddress) {
            this.resourceDefinition.dataAddress = dataAddress;
            return this;
        }

        public Builder contractId(String contractId) {
            this.resourceDefinition.contractId = contractId;
            return this;
        }
    }
}
