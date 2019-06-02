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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CryptoWrapper
 *
 * @author Marctrix - Initial contribution
 */
public final class Crypto {

    private static final int bound = 32;

    private final static Logger logger = LoggerFactory.getLogger(Crypto.class);

    public static String Hash256(String input) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            return Hex.encodeHexString(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            logger.error("Unkown Algorithm.", e);
        }

        return null;
    }

    public static String AES(String input, String key, String initializationVector) {
        byte[] inputBytes;
        try {

            inputBytes = Pad(input).getBytes("ISO-8859-1");

            SecretKeySpec aesKey = new SecretKeySpec(Hex.decodeHex(key.toCharArray()), 0, 32, "AES");
            IvParameterSpec aesIv = new IvParameterSpec(Hex.decodeHex(initializationVector.toCharArray()), 0, 16);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv);
            byte[] encrypted = cipher.doFinal(inputBytes);

            return Hex.encodeHexString(encrypted);

        } catch (Exception e) {
            logger.error("Error while encrypting workload.", e);
        }

        return null;
    }

    public static String Pad(String input) {
        byte bs = 16;
        int pad_count = bs - input.length() % bs;

        String output = input;

        for (int n = 0; n < pad_count; n++) {
            output += (char) 0;
        }

        return output;
    }

    public static int[] CheckInt(byte[] byteArray) {
        IntBuffer intBuf = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);

        return array;
    }
}