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
 *       Mercedes-Benz Tech Innovation GmbH - DataEncrypter can be provided by extensions
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.consumerproxy;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.spi.client.DataPlaneClient;
import org.eclipse.edc.connector.transfer.dataplane.api.ConsumerPullTransferTokenValidationApiController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.flow.ProviderPushTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.provision.ConsumerPullDataPlaneProxyProvisionedResource;
import org.eclipse.edc.connector.transfer.dataplane.provision.ConsumerPullDataPlaneProxyProvisioner;
import org.eclipse.edc.connector.transfer.dataplane.provision.ConsumerPullDataPlaneProxyResourceDefinition;
import org.eclipse.edc.connector.transfer.dataplane.provision.ConsumerPullDataPlaneProxyResourceDefinitionGenerator;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullDataPlaneProxyAddressResolver;
import org.eclipse.edc.connector.transfer.dataplane.security.PublicKeyParser;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.validation.ContractValidationRule;
import org.eclipse.edc.connector.transfer.dataplane.validation.ExpirationDateValidationRule;
import org.eclipse.edc.connector.transfer.spi.callback.ControlPlaneApiUrl;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.connector.transfer.spi.flow.DataFlowManager;
import org.eclipse.edc.connector.transfer.spi.provision.ProvisionManager;
import org.eclipse.edc.connector.transfer.spi.provision.ResourceManifestGenerator;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.TokenValidationServiceImpl;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.jwt.spi.TokenValidationService;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.security.PrivateKeyResolver;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.web.spi.WebService;

import java.security.PrivateKey;
import java.time.Clock;
import java.util.Objects;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DEFAULT_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_SIGNER_PRIVATE_KEY_ALIAS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VERIFIER_PUBLIC_KEY_ALIAS;

@Extension(value = ConsumerPullDataPlaneConsumerProxyExtension.NAME)
public class ConsumerPullDataPlaneConsumerProxyExtension implements ServiceExtension {

    public static final String NAME = "Transfer Data Plane Core";

    @Inject
    private EndpointDataReferenceTransformerRegistry transformerRegistry;

    @Inject
    private WebService webService;

    @Inject
    private DataFlowManager dataFlowManager;

    @Inject
    private PrivateKeyResolver privateKeyResolver;

    @Inject
    private Vault vault;

    @Inject
    private Clock clock;

    @Inject
    private DataEncrypter dataEncrypter;

    @Inject
    private DataPlaneClient dataPlaneClient;

    @Inject
    private ControlApiConfiguration controlApiConfiguration;

    @Inject
    private ResourceManifestGenerator resourceManifestGenerator;

    @Inject
    private ProvisionManager provisionManager;

    @Inject
    private DataPlaneSelectorClient selectorClient;

    @Inject(required = false)
    private ControlPlaneApiUrl callbackUrl;

    @Inject
    private TypeManager typeManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new ConsumerPullTransferTokenValidationApiController(tokenValidationService(context), dataEncrypter, typeManager);
        webService.registerResource(controlApiConfiguration.getContextAlias(), controller);

        registerProvisioner(context);

        dataFlowManager.register(new ConsumerPullTransferDataFlowController());
        dataFlowManager.register(new ProviderPushTransferDataFlowController(callbackUrl, dataPlaneClient));
    }

    private void registerProvisioner(ServiceExtensionContext context) {
        typeManager.registerTypes(ConsumerPullDataPlaneProxyResourceDefinition.class, ConsumerPullDataPlaneProxyProvisionedResource.class);

        resourceManifestGenerator.registerGenerator(new ConsumerPullDataPlaneProxyResourceDefinitionGenerator());

        var validitySeconds = context.getSetting(TOKEN_VALIDITY_SECONDS, DEFAULT_TOKEN_VALIDITY_SECONDS);
        var tokenGenerationService = tokenGenerationService(context);
        var resolver = new ConsumerPullDataPlaneProxyAddressResolver(dataEncrypter, typeManager, clock, validitySeconds, tokenGenerationService);
        var provisioner = new ConsumerPullDataPlaneProxyProvisioner(selectorClient, resolver);
        provisionManager.register(provisioner);
    }

    private TokenGenerationService tokenGenerationService(ServiceExtensionContext context) {
        var alias = context.getConfig().getString(TOKEN_SIGNER_PRIVATE_KEY_ALIAS);
        var privateKey = privateKeyResolver.resolvePrivateKey(alias, PrivateKey.class);
        Objects.requireNonNull(privateKey, "Failed to retrieve private key with alias: " + alias);
        return new TokenGenerationServiceImpl(privateKey);
    }

    /**
     * Service in charge of validating access token sent by the Data Plane.
     */
    private TokenValidationService tokenValidationService(ServiceExtensionContext context) {
        var alias = context.getSetting(TOKEN_VERIFIER_PUBLIC_KEY_ALIAS, null);
        var pem = vault.resolveSecret(alias);
        Objects.requireNonNull(pem, "Failed to retrieve public key with alias: " + alias);
        var publicKey = PublicKeyParser.from(pem);
        var registry = new TokenValidationRulesRegistryImpl();
        registry.addRule(new ContractValidationRule(contractNegotiationStore, clock));
        registry.addRule(new ExpirationDateValidationRule(clock));
        return new TokenValidationServiceImpl(id -> publicKey, registry);
    }
}
