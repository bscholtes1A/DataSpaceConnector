/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.transfer.dataplane.provision;

import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.function.Predicate;

import static org.eclipse.edc.connector.transfer.dataplane.spi.TransferDataPlaneConstants.HTTP_PROXY;

/**
 * Validates {@link DataAddress}, returns true if the Address is of type HTTP_PROXY.
 */
public class ConsumerPullDataPlaneProxyDataAddressValidator implements Predicate<DataAddress> {

    private final Predicate<DataAddress> isHttpProxyType = dataAddress -> HTTP_PROXY.equals(dataAddress.getType());

    @Override
    public boolean test(DataAddress dataAddress) {
        return isHttpProxyType.test(dataAddress);
    }

}
