package org.eclipse.edc.connector.transfer.dataplane.spi.proxy;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Map;
import java.util.Objects;

public class EndpointDataReferenceRequest {

    private String id;
    private String contractId;
    private DataAddress contentAddress;
    private Map<String, String> properties;

    private EndpointDataReferenceRequest() {
    }

    public String getId() {
        return id;
    }

    public String getContractId() {
        return contractId;
    }

    public DataAddress getContentAddress() {
        return contentAddress;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "EndpointDataReferenceRequest{" +
                "id='" + id + '\'' +
                ", contractId='" + contractId + '\'' +
                ", contentAddress=" + contentAddress +
                ", properties=" + properties +
                '}';
    }

    public static class Builder {
        private final EndpointDataReferenceRequest request = new EndpointDataReferenceRequest();

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            request.id = id;
            return this;
        }

        public Builder contractId(String contractId) {
            request.contractId = contractId;
            return this;
        }

        public Builder contentAddress(DataAddress contentAddress) {
            request.contentAddress = contentAddress;
            return this;
        }

        public EndpointDataReferenceRequest build() {
            Objects.requireNonNull(request.id, "id");
            Objects.requireNonNull(request.contractId, "contractId");
            Objects.requireNonNull(request.contentAddress, "contentAddress");
            return request;
        }
    }
}
