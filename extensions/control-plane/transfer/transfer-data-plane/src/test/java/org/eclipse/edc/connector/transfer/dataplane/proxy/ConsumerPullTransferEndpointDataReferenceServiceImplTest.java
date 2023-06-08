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
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.proxy;

class ConsumerPullTransferEndpointDataReferenceServiceImplTest {


//    private static final TypeManager TYPE_MANAGER = new TypeManager();
//
//    private final Instant now = Instant.now();
//    private final ThreadLocalRandom random = ThreadLocalRandom.current();
//    private long tokenValiditySeconds;
//    private ConsumerPullTransferEndpointDataReferenceServiceImpl proxyManager;
//    private TokenGenerationService tokenGeneratorMock;
//    private DataEncrypter encrypterMock;
//
//    @BeforeEach
//    public void setUp() {
//        tokenGeneratorMock = mock(TokenGenerationService.class);
//        tokenValiditySeconds = random.nextLong(100L);
//        encrypterMock = mock(DataEncrypter.class);
//        Clock clock = Clock.fixed(now, UTC);
//        proxyManager = new ConsumerPullTransferEndpointDataReferenceServiceImpl(tokenGeneratorMock, TYPE_MANAGER, tokenValiditySeconds, encrypterMock, clock);
//    }
//
//    /**
//     * OK test: check proxy creation.
//     */
//    @Test
//    void createProxy_success() {
//        var id = UUID.randomUUID().toString();
//        var address = DataAddress.Builder.newInstance().type("test-type").build();
//        var addressStr = TYPE_MANAGER.writeValueAsString(address);
//        var encryptedDataAddress = UUID.randomUUID().toString();
//        var contractId = UUID.randomUUID().toString();
//        var proxyEndpoint = "test.proxy.endpoint";
//        var generatedToken = TokenRepresentation.Builder.newInstance().token(UUID.randomUUID().toString()).build();
//
//        var decoratorCaptor = ArgumentCaptor.forClass(ConsumerPullTransferTokenDecorator.class);
//
//        when(encrypterMock.encrypt(addressStr)).thenReturn(encryptedDataAddress);
//        when(tokenGeneratorMock.generate(any())).thenReturn(Result.success(generatedToken));
//
//        var proxyCreationRequest = ConsumerPullTransferEndpointDataReferenceCreationRequest.Builder.newInstance()
//                .id(id)
//                .contentAddress(address)
//                .contractId(contractId)
//                .proxyEndpoint(proxyEndpoint)
//                .property("key1", UUID.randomUUID().toString())
//                .property("key2", UUID.randomUUID().toString())
//                .build();
//
//        var result = proxyManager.createProxyReference(proxyCreationRequest);
//
//        verify(tokenGeneratorMock).generate(decoratorCaptor.capture());
//
//        var decorator = decoratorCaptor.getValue();
//
//        assertThat(decorator.claims())
//                .containsEntry(CONTRACT_ID, contractId)
//                .containsEntry(DATA_ADDRESS, encryptedDataAddress)
//                .containsEntry(EXPIRATION_TIME, Date.from(now.plusSeconds(tokenValiditySeconds)));
//
//        assertThat(result.succeeded()).isTrue();
//        var edr = result.getContent();
//        assertThat(edr.getId()).isEqualTo(id);
//        assertThat(edr.getEndpoint()).isEqualTo(proxyEndpoint);
//        assertThat(edr.getAuthKey()).isEqualTo(HttpHeaders.AUTHORIZATION);
//        assertThat(edr.getAuthCode()).isEqualTo(generatedToken.getToken());
//        var expectedProperties = new HashMap<>(proxyCreationRequest.getProperties());
//        expectedProperties.put(EDC_CONTRACT_ID, contractId);
//        assertThat(edr.getProperties()).containsExactlyInAnyOrderEntriesOf(expectedProperties);
//    }
//
//    /**
//     * Check that a failed result is returned if input {@link EndpointDataReference} does not contain a contract id.
//     */
//    @Test
//    void tokenGenerationFails_shouldReturnFailedResult() {
//        var errorMsg = "test-errormsg";
//        var request = ConsumerPullTransferEndpointDataReferenceCreationRequest.Builder.newInstance()
//                .contentAddress(DataAddress.Builder.newInstance().type("test-type").build())
//                .contractId(UUID.randomUUID().toString())
//                .proxyEndpoint("test.proxy.endpoint")
//                .build();
//
//        when(tokenGeneratorMock.generate(any(ConsumerPullTransferTokenDecorator.class))).thenReturn(Result.failure(errorMsg));
//
//        var result = proxyManager.createProxyReference(request);
//
//        assertThat(result.failed()).isTrue();
//        assertThat(result.getFailureMessages()).containsExactly(errorMsg);
//    }
}