package org.eclipse.edc.connector.transfer.dataplane.proxy;

import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneSelectorClient;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullEndpointDataReferenceAdapter;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.EndpointDataReferenceRequest;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.time.Clock;
import java.util.Date;

import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.EDC_CONTRACT_ID;
import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;

public class DefaultConsumerPullDataPlaneInstanceAdapter implements ConsumerPullEndpointDataReferenceAdapter {

    private static final String PUBLIC_API_URL_PROPERTY = "publicApiUrl";

    private final DataPlaneSelectorClient selectorClient;
    private final TypeManager typeManager;
    private final DataEncrypter dataEncrypter;
    private final TokenGenerationService tokenGenerationService;
    private final Clock clock;
    private final Long tokenValiditySeconds;

    public DefaultConsumerPullDataPlaneInstanceAdapter(DataPlaneSelectorClient selectorClient, TypeManager typeManager, DataEncrypter dataEncrypter, TokenGenerationService tokenGenerationService, Clock clock, Long tokenValiditySeconds) {
        this.selectorClient = selectorClient;
        this.typeManager = typeManager;
        this.dataEncrypter = dataEncrypter;
        this.tokenGenerationService = tokenGenerationService;
        this.clock = clock;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    @Override
    public boolean canHandle(EndpointDataReferenceRequest request) {
        return true;
    }

    @Override
    public Result<EndpointDataReference> convert(EndpointDataReferenceRequest request) {
        var instance = selectorClient.find(request.getContentAddress(), destinationAddress());
        if (instance == null) {
            return Result.failure("Failed to find data plane instance supporting source type: " + request.getContentAddress().getType());
        }

        var publicApiUrl = (String) instance.getProperties().get(PUBLIC_API_URL_PROPERTY);
        if (publicApiUrl == null) {
            return Result.failure(String.format("Missing property `%s` in DataPlaneInstance", PUBLIC_API_URL_PROPERTY));
        }

        return generateToken(request.getContractId(), request.getContentAddress())
                .compose(token -> Result.success(EndpointDataReference.Builder.newInstance()
                        .id(request.getId())
                        .endpoint(publicApiUrl)
                        .authKey(HttpHeaders.AUTHORIZATION)
                        .authCode(token.getToken())
                        .properties(request.getProperties())
                        .property(EDC_CONTRACT_ID, request.getContractId())
                        .build()));
    }

    private static DataAddress destinationAddress() {
        return DataAddress.Builder.newInstance().type(HTTP_PROXY).build();
    }

    private Result<TokenRepresentation> generateToken(String contractId, DataAddress contentAddress) {
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(contentAddress));
        var decorator = new EdcDataPlaneTokenDecorator(Date.from(clock.instant().plusSeconds(tokenValiditySeconds)), contractId, encryptedDataAddress);
        return tokenGenerationService.generate(decorator);
    }
}
