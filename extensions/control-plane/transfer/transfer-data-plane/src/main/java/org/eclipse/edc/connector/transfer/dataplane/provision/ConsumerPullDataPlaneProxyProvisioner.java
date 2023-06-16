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

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyAddressResolver;
import org.eclipse.edc.connector.transfer.spi.provision.Provisioner;
import org.eclipse.edc.connector.transfer.spi.types.DeprovisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionResponse;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;

/**
 * Require an OAuth2 token and stores it in the vault to make data-plane include it in the request
 */
public class ConsumerPullDataPlaneProxyProvisioner implements Provisioner<ConsumerPullDataPlaneProxyResourceDefinition, ConsumerPullDataPlaneProxyProvisionedResource> {

    private final DataPlaneSelectorClient selectorClient;
    private final ConsumerPullDataPlaneProxyAddressResolver resolver;

    public ConsumerPullDataPlaneProxyProvisioner(DataPlaneSelectorClient selectorClient, ConsumerPullDataPlaneProxyAddressResolver resolver) {
        this.selectorClient = selectorClient;
        this.resolver = resolver;
    }

    @Override
    public boolean canProvision(ResourceDefinition resourceDefinition) {
        return resourceDefinition instanceof ConsumerPullDataPlaneProxyResourceDefinition;
    }

    @Override
    public boolean canDeprovision(ProvisionedResource resourceDefinition) {
        return resourceDefinition instanceof ConsumerPullDataPlaneProxyProvisionedResource;
    }

    @Override
    public CompletableFuture<StatusResult<ProvisionResponse>> provision(ConsumerPullDataPlaneProxyResourceDefinition resourceDefinition, Policy policy) {
        return Optional.ofNullable(selectorClient.find(resourceDefinition.getDataAddress(), destinationAddress()))
                .map(instance -> resolver.toDataAddress(resourceDefinition.getDataAddress(), resourceDefinition.getContractId(), instance)
                        .map(address -> completedFuture(StatusResult.success(toProvisionResponse(resourceDefinition, address))))
                        .orElse(failure -> fatalErrorResponse(String.format("Failed to resolve DataPlaneInstance into data address: " + failure.getFailureDetail()))))
                .orElse(fatalErrorResponse("Failed to find DataPlaneInstance for proxying data from source: " + resourceDefinition.getDataAddress().getType()));
    }

    @Override
    public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ConsumerPullDataPlaneProxyProvisionedResource provisionedResource, Policy policy) {
        var deprovisionedResource = DeprovisionedResource.Builder.newInstance()
                .provisionedResourceId(provisionedResource.getId())
                .build();
        return completedFuture(StatusResult.success(deprovisionedResource));
    }

    private ProvisionResponse toProvisionResponse(ResourceDefinition resourceDefinition, DataAddress address) {
        var resourceName = resourceDefinition.getId() + "-proxy";
        var provisioned = ConsumerPullDataPlaneProxyProvisionedResource.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .resourceDefinitionId(resourceDefinition.getId())
                .transferProcessId(resourceDefinition.getTransferProcessId())
                .dataAddress(address)
                .resourceName(resourceName)
                .hasToken(false)
                .build();
        return ProvisionResponse.Builder.newInstance()
                .resource(provisioned)
                .build();
    }

    private static CompletableFuture<StatusResult<ProvisionResponse>> fatalErrorResponse(String message) {
        return completedFuture(StatusResult.failure(ResponseStatus.FATAL_ERROR, message));
    }

    private static DataAddress destinationAddress() {
        return DataAddress.Builder.newInstance().type(HTTP_PROXY).build();
    }
}
