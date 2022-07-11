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
 *       Fraunhofer Institute for Software and Systems Engineering - Improvements
 *
 */

package org.eclipse.dataspaceconnector.core.security.fs;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.security.ConfigurablePrivateKeyResolver;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an RSA or EC private key from a JKS keystore.
 */
public class FsPrivateKeyResolver extends ConfigurablePrivateKeyResolver {
    private final Map<String, String> privateKeyCache = new HashMap<>();

    /**
     * Caches the private keys for performance.
     *
     * @param password the keystore password. Individual key passwords are not supported.
     * @param keyStore the keystore
     */
    public FsPrivateKeyResolver(String password, KeyStore keyStore) {
        var encodedPassword = password.toCharArray();
        try {
            var iter = keyStore.aliases();
            while (iter.hasMoreElements()) {
                var alias = iter.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                var key = keyStore.getKey(alias, encodedPassword);
                if ((key instanceof RSAPrivateKey || key instanceof ECPrivateKey)) {
                    privateKeyCache.put(alias, toPem((PrivateKey) key));
                }
            }

        } catch (GeneralSecurityException e) {
            throw new EdcException(e);
        }
    }

    @Override
    protected @Nullable String getEncodedKey(String id) {
        return privateKeyCache.get(id);
    }

    private static String toPem(PrivateKey key) {
        StringWriter sw = new StringWriter();
        try (var pw = new JcaPEMWriter(sw)) {
            pw.writeObject(key);
        } catch (IOException e) {
            throw new EdcException("Failed to write key to PEM", e);
        }
        return sw.toString();
    }
}
