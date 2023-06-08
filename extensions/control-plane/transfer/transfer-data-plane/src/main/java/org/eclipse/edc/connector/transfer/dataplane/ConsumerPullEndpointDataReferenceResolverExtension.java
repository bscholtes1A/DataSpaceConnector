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

package org.eclipse.edc.connector.transfer.dataplane;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullEndpointDataReferenceResolverImpl;
import org.eclipse.edc.connector.transfer.dataplane.proxy.DefaultConsumerPullDataPlaneInstanceAdapter;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceAdapter;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.KeyPairWrapper;
import org.eclipse.edc.jwt.TokenGenerationServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.TypeManager;

import java.time.Clock;

import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.DEFAULT_TOKEN_VALIDITY_SECONDS;
import static org.eclipse.edc.connector.transfer.dataplane.TransferDataPlaneConfig.TOKEN_VALIDITY_SECONDS;

@Extension(value = ConsumerPullEndpointDataReferenceResolverExtension.NAME)
public class ConsumerPullEndpointDataReferenceResolverExtension implements ServiceExtension {

    public static final String NAME = "Consumer Pull Endpoint Data Reference";

    @Inject
    private DataPlaneSelectorClient selectorClient;

    @Inject
    private TypeManager typeManager;

    @Inject
    private DataEncrypter dataEncrypter;

    @Inject
    private Clock clock;

    @Inject
    private KeyPairWrapper keyPairWrapper;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public ConsumerPullEndpointDataReferenceResolver consumerPullEndpointDataReferenceResolver(ServiceExtensionContext context) {
        var resolver = new ConsumerPullEndpointDataReferenceResolverImpl();
        resolver.registerAdapter(createDefaultAdapter(context.getConfig()));
        return resolver;
    }

    private ConsumerPullEndpointDataReferenceAdapter createDefaultAdapter(Config config) {
        var tokenGenerationService = new TokenGenerationServiceImpl(keyPairWrapper.get().getPrivate());
        var tokenValiditySeconds = config.getLong(TOKEN_VALIDITY_SECONDS, DEFAULT_TOKEN_VALIDITY_SECONDS);
        return new DefaultConsumerPullDataPlaneInstanceAdapter(selectorClient, typeManager, dataEncrypter, tokenGenerationService, clock, tokenValiditySeconds);
    }
}
