/*
 *  Copyright (c) 2021 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.connector.contract.spi.types.negotiation;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.contract.spi.types.protocol.ContractRemoteMessage;
import org.jetbrains.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Object that wraps the contract offer and provides additional information about e.g. protocol and recipient.
 */
public class ContractRequestMessage implements ContractRemoteMessage {

    private Type type = Type.COUNTER_OFFER;
    private String protocol;
    @Deprecated(forRemoval = true)
    private String connectorId;
    private String callbackAddress;
    private String processId;
    private ContractOffer contractOffer;
    private String contractOfferId;
    private String dataSet;

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getCallbackAddress() {
        return callbackAddress;
    }

    @Deprecated
    public String getConnectorId() {
        return connectorId;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    public Type getType() {
        return type;
    }

    @Nullable
    public ContractOffer getContractOffer() {
        return contractOffer;
    }

    @Nullable
    public String getContractOfferId() {
        return contractOfferId;
    }

    public String getDataSet() {
        return dataSet;
    }

    public enum Type {
        INITIAL,
        COUNTER_OFFER
    }

    public static class Builder {
        private final ContractRequestMessage contractRequestMessage;

        private Builder() {
            contractRequestMessage = new ContractRequestMessage();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder protocol(String protocol) {
            contractRequestMessage.protocol = protocol;
            return this;
        }

        @Deprecated
        public Builder connectorId(String connectorId) {
            contractRequestMessage.connectorId = connectorId;
            return this;
        }

        public Builder callbackAddress(String callbackAddress) {
            contractRequestMessage.callbackAddress = callbackAddress;
            return this;
        }

        public Builder processId(String processId) {
            contractRequestMessage.processId = processId;
            return this;
        }

        public Builder contractOffer(ContractOffer contractOffer) {
            contractRequestMessage.contractOffer = contractOffer;
            return this;
        }

        public Builder contractOfferId(String id) {
            contractRequestMessage.contractOfferId = id;
            return this;
        }

        public Builder type(Type type) {
            contractRequestMessage.type = type;
            return this;
        }

        public Builder dataSet(String dataSet) {
            contractRequestMessage.dataSet = dataSet;
            return this;
        }

        public ContractRequestMessage build() {
            requireNonNull(contractRequestMessage.protocol, "protocol");
            requireNonNull(contractRequestMessage.processId, "processId");
            if (contractRequestMessage.contractOfferId == null) {
                requireNonNull(contractRequestMessage.contractOffer, "contractOffer");
            } else {
                requireNonNull(contractRequestMessage.contractOfferId, "contractOfferId");
            }
            return contractRequestMessage;
        }
    }
}
