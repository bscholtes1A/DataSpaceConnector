/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.oauth2.core.impl;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.JwtDecoratorRegistry;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.ValidationRule;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.ValidationRuleResult;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenResult;
import org.eclipse.dataspaceconnector.spi.iam.VerificationResult;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Implements the OAuth2 client credentials flow and bearer token validation.
 */
public class Oauth2ServiceImpl implements IdentityService {

    private static final String GRANT_TYPE = "client_credentials";
    private static final String ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final Oauth2Configuration configuration;

    private final OkHttpClient httpClient;
    private final TypeManager typeManager;
    private final List<ValidationRule> validationRules;
    private final JWSSigner tokenSigner;
    private final JwtDecoratorRegistry jwtDecoratorRegistry;

    /**
     * Creates a new instance of the OAuth2 Service
     *
     * @param configuration             The configuration
     * @param signerProvider            A {@link Supplier} which is used to get a {@link JWSSigner} instance.
     * @param client                    Http client
     * @param jwtDecoratorRegistry      Registry containing the decorator for build the JWT
     * @param typeManager               Type manager
     * @param additionalValidationRules An optional list of {@link ValidationRule} that are evaluated <em>after</em> the
     *                                  standard OAuth2 validation
     */
    public Oauth2ServiceImpl(Oauth2Configuration configuration, Supplier<JWSSigner> signerProvider, OkHttpClient client, JwtDecoratorRegistry jwtDecoratorRegistry, TypeManager typeManager, ValidationRule... additionalValidationRules) {
        this.configuration = configuration;
        this.typeManager = typeManager;
        this.httpClient = client;
        this.jwtDecoratorRegistry = jwtDecoratorRegistry;

        List<ValidationRule> rules = new ArrayList<>();
        rules.add(new Oauth2ValidationRule()); //OAuth2 validation must ALWAYS be done
        rules.addAll(List.of(additionalValidationRules));
        validationRules = Collections.unmodifiableList(rules);

        tokenSigner = signerProvider.get();
        if (tokenSigner == null) {
            throw new EdcException("Could not resolve private key");
        }
    }

    @Override
    public TokenResult obtainClientCredentials(String scope) {
        String assertion = buildJwt();

        RequestBody requestBody = new FormBody.Builder()
                .add("client_assertion_type", ASSERTION_TYPE)
                .add("grant_type", GRANT_TYPE)
                .add("client_assertion", assertion)
                .add("scope", scope)
                .build();

        Request request = new Request.Builder().url(configuration.getTokenUrl()).addHeader("Content-Type", CONTENT_TYPE).post(requestBody).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                try (var body = response.body()) {
                    String message = body == null ? "<empty body>" : body.string();
                    return TokenResult.Builder.newInstance().error(message).build();
                }
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return TokenResult.Builder.newInstance().error("<empty token body>").build();
            }

            String responsePayload = responseBody.string();
            LinkedHashMap<String, Object> deserialized = typeManager.readValue(responsePayload, LinkedHashMap.class);
            String token = (String) deserialized.get("access_token");
            long expiresIn = ((Integer) deserialized.get("expires_in")).longValue();
            return TokenResult.Builder.newInstance().token(token).expiresIn(expiresIn).build();
        } catch (IOException e) {
            throw new EdcException(e);
        }
    }

    @Override
    public VerificationResult verifyJwtToken(String token, String audience) {
        try {
            var signedJwt = SignedJWT.parse(token);

            String publicKeyId = signedJwt.getHeader().getKeyID();
            var verifier = createVerifier(signedJwt.getHeader(), publicKeyId);
            if (verifier == null) {
                return new VerificationResult("Failed to create verifier");
            }

            if (!signedJwt.verify(verifier)) {
                return new VerificationResult("Token verification not successful");
            }
            var claimsSet = signedJwt.getJWTClaimsSet();

            // now we get the results of all the single rules, lets collate them into one
            var res = validationRules.stream()
                    .map(r -> r.checkRule(claimsSet, audience))
                    .reduce(ValidationRuleResult::merge)
                    .orElseThrow();

            // return instantly if there are errors present
            if (!res.isSuccess()) {
                return new VerificationResult(res.getErrorMessages());
            }

            // build claim tokens
            var tokenBuilder = ClaimToken.Builder.newInstance();
            claimsSet.getClaims().forEach((k, v) -> {
                var claimValue = Objects.toString(v);
                if (claimValue == null) {
                    // only support strings
                    return;
                }
                tokenBuilder.claim(k, claimValue);
            });
            return new VerificationResult(tokenBuilder.build());

        } catch (JOSEException e) {
            return new VerificationResult(e.getMessage());
        } catch (ParseException e) {
            return new VerificationResult("Token could not be decoded");
        }
    }


    @Nullable
    private JWSVerifier createVerifier(JWSHeader header, String publicKeyId) {
        RSAPublicKey publicKey = configuration.getIdentityProviderKeyResolver().resolveKey(publicKeyId);
        try {
            return new DefaultJWSVerifierFactory().createJWSVerifier(header, publicKey);
        } catch (JOSEException e) {
            return null;
        }
    }

    private String buildJwt() {
        try {
            JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.RS256);
            var claimsSet = new JWTClaimsSet.Builder();
            jwtDecoratorRegistry.getAll().forEach(d -> d.decorate(headerBuilder, claimsSet));
            var jwt = new SignedJWT(headerBuilder.build(), claimsSet.build());
            jwt.sign(tokenSigner);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new EdcException(e);
        }
    }
}
