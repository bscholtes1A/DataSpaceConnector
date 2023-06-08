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

import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceResolver;
import org.eclipse.edc.connector.transfer.spi.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

@Extension(value = ConsumerPullTransferConsumerProxyExtension.NAME)
public class ConsumerPullTransferConsumerProxyExtension implements ServiceExtension {

    public static final String NAME = "Consumer Pull Transfer Consumer Proxy";

    @Inject
    private EndpointDataReferenceTransformerRegistry transformerRegistry;

    @Inject
    private ConsumerPullEndpointDataReferenceResolver referenceResolver;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var transformer = new ConsumerPullTransferConsumerProxyTransformer(referenceResolver);
        transformerRegistry.registerTransformer(transformer);
    }
}
