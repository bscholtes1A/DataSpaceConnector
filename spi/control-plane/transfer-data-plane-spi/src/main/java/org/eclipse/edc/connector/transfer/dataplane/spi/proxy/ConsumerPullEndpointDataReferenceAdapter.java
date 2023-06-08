package org.eclipse.edc.connector.transfer.dataplane.spi.proxy;

import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

public interface ConsumerPullEndpointDataReferenceAdapter {

    boolean canHandle(EndpointDataReferenceRequest request);

    Result<EndpointDataReference> convert(EndpointDataReferenceRequest request);
}
