package org.eclipse.edc.connector.transfer.dataplane.proxy;

import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultConsumerPullDataPlaneInstanceAdapterTest {

    private static final Long VALIDITY = TimeUnit.MINUTES.toSeconds(10);

    private final DataPlaneSelectorClient selectorClient = mock(DataPlaneSelectorClient.class);
    private final TypeManager typeManager = mock(TypeManager.class);
    private final DataEncrypter dataEncrypter = mock(DataEncrypter.class);
    private final TokenGenerationService tokenGenerationService = mock(TokenGenerationService.class);
    private final Clock clock = mock(Clock.class);


    private final DefaultConsumerPullDataPlaneInstanceAdapter adapter = new DefaultConsumerPullDataPlaneInstanceAdapter(selectorClient, typeManager, dataEncrypter, tokenGenerationService, clock, VALIDITY);

    @Test
    void verifyCanHandle() {
        assertThat(adapter.canHandle(null)).isTrue();
    }

    @Test
    void verifyConvertSuccess() {
        var publicApiUrl = "http://example.com";
        var request = EndpointDataReferenceRequest.Builder.newInstance()
                .id("id")
                .contractId("contract-id")
                .contentAddress(DataAddress.Builder.newInstance().type("test").build())
                .build();
        var instance = DataPlaneInstance.Builder.newInstance()
                .property("publicApiUrl", publicApiUrl)
                .build();

        when(selectorClient.find(eq(request.getContentAddress()), argThat(dest -> dest.getType().equals(HTTP_PROXY))))
                .thenReturn(instance);
    }
}