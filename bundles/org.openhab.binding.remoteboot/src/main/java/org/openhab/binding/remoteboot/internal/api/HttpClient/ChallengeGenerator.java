/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.remoteboot.internal.api.HttpClient;

import java.security.SecureRandom;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client Challenge Generator
 *
 * @author Marctrix - Initial contribution
 */
public final class ChallengeGenerator {
    private static final int bound = 32;

    private final static Logger logger = LoggerFactory.getLogger(ChallengeGenerator.class);

    public static String Generate() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[bound];
        random.nextBytes(bytes);

        return Hex.encodeHexString(bytes);
    }
}