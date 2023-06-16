package org.eclipse.edc.connector.transfer.dataplane.util;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.connector.transfer.dataplane.spi.security.DataEncrypter;
import org.eclipse.edc.jwt.spi.TokenGenerationService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.time.Clock;
import java.util.Date;
import java.util.Optional;

public class ConsumerPullDataPlaneProxyAddressResolver {

    private static final String PUBLIC_API_URL_PROPERTY = "publicApiUrl";

    private final DataEncrypter dataEncrypter;
    private final TypeManager typeManager;
    private final Clock clock;
    private final long tokenValiditySeconds;
    private final TokenGenerationService tokenGenerationService;

    public ConsumerPullDataPlaneProxyAddressResolver(DataEncrypter dataEncrypter, TypeManager typeManager, Clock clock, long tokenValiditySeconds, TokenGenerationService tokenGenerationService) {
        this.dataEncrypter = dataEncrypter;
        this.typeManager = typeManager;
        this.clock = clock;
        this.tokenValiditySeconds = tokenValiditySeconds;
        this.tokenGenerationService = tokenGenerationService;
    }

    public Result<DataAddress> toDataAddress(DataAddress address, String contractId, DataPlaneInstance instance) {
        return resolveProxyUrl(instance)
                .compose(proxyUrl -> generateAccessToken(address, contractId)
                        .map(token -> DataAddress.Builder.newInstance()
                                .type(EndpointDataReference.EDR_SIMPLE_TYPE)
                                .property(EndpointDataReference.ENDPOINT, proxyUrl)
                                .property(EndpointDataReference.AUTH_KEY, "Authorization")
                                .property(EndpointDataReference.AUTH_CODE, token)
                                .build()));
    }

    private Result<String> resolveProxyUrl(DataPlaneInstance instance) {
        return Optional.ofNullable(instance.getProperties().get(PUBLIC_API_URL_PROPERTY))
                .map(url -> Result.success((String) url))
                .orElse(Result.failure(String.format("Missing property `%s` in DataPlaneInstance", PUBLIC_API_URL_PROPERTY)));
    }

    private Result<String> generateAccessToken(DataAddress source, String contractId) {
        var encryptedDataAddress = dataEncrypter.encrypt(typeManager.writeValueAsString(source));
        var decorator = new ConsumerPullDataPlaneProxyTokenDecorator(Date.from(clock.instant().plusSeconds(tokenValiditySeconds)), contractId, encryptedDataAddress);
        return tokenGenerationService.generate(decorator)
                .map(TokenRepresentation::getToken);
    }
}
