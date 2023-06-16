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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedContentResource;

@JsonDeserialize(builder = ConsumerPullDataPlaneProxyProvisionedResource.Builder.class)
public class ConsumerPullDataPlaneProxyProvisionedResource extends ProvisionedContentResource {

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProvisionedContentResource.Builder<ConsumerPullDataPlaneProxyProvisionedResource, Builder> {

        private Builder() {
            super(new ConsumerPullDataPlaneProxyProvisionedResource());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

    }
}
