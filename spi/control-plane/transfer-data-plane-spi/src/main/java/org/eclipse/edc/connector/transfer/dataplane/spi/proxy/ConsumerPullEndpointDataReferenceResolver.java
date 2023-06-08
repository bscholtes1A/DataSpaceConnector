package org.eclipse.edc.connector.transfer.dataplane.spi.proxy;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

public interface ConsumerPullEndpointDataReferenceResolver {

    void registerAdapter(ConsumerPullEndpointDataReferenceAdapter adapter);

    Result<EndpointDataReference> resolve(EndpointDataReferenceRequest request);
}
