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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * HttpClient for RemoteBootApi.
 *
 * @author Marctrix - Initial contribution
 */
public class RemoteBootHttpClient {

    private final Set<Class<?>> primitives = new HashSet<Class<?>>(Arrays.asList(Boolean.class, Character.class,
            Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class));

    private final Logger logger = LoggerFactory.getLogger(RemoteBootHttpClient.class);

    private String baseUrl;
    private String password;

    public RemoteBootHttpClient(String host, String password) {
        baseUrl = host;
        this.password = Crypto.Hash256(password);
    }

    private Challenge GetChallenge() {
        try {
            return makeQuery("/api/auth/challenge.php", Challenge.class);
        } catch (Exception ex) {
            logger.error("AuthenticationError", ex.getMessage());
        }

        return null;
    }

    public <T, TK> TK makeAuthQuery(String path, T data, Class<?> type) {
        return makeAuthQuery(path, data, "GET", type);
    }

    public <T, TK> TK makeAuthQuery(String path, T data, String verb, Class<?> type) {
        Challenge serverChallenge = GetChallenge();

        if (serverChallenge != null) {

            String clientChallenge = ChallengeGenerator.Generate();
            String commonChallengeInput = serverChallenge.challenge + clientChallenge + password;

            String commonChallenge = Crypto.Hash256(commonChallengeInput);

            try {

                ObjectMapper mapper = new ObjectMapper();
                String jsonData = mapper.writeValueAsString(data);

                String encryptedData = Crypto.AES(jsonData, password, serverChallenge.challenge);

                return makeQuery(path, new Object() {
                    public final String p = encryptedData;
                    public final int l = jsonData.length();
                    public final String r = commonChallenge;
                    public final int rs = serverChallenge.sequence;
                    public final String c = clientChallenge;
                }, verb, type);
            } catch (Exception e) {
                logger.error("Couldn't fetch Query result.", e.getMessage());
            }
        }

        return null;
    }

    public <T, TK> TK makeQuery(String path, Class<?> type) throws IOException {
        return makeQuery(path, null, "GET", type);
    }

    public <T, TK> TK makeQuery(String path, T data, Class<?> type) throws IOException {
        return makeQuery(path, data, "GET", type);
    }

    public <T, TK> TK makeQuery(String path, T data, String verb, Class<?> type) throws IOException {
        String fullUrl = baseUrl + path;

        if (data != null && !primitives.contains(data.getClass())) {
            fullUrl += getParameterStringOf(data);
        }

        URL url = new URL(fullUrl);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        if (con.getResponseCode() == 200) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            ObjectMapper mapper = new ObjectMapper();
            TK retVal = (TK) mapper.readValue(response.toString(), type);

            return retVal;
        }

        return null;
    }

    private <T> String getParameterStringOf(T data) {
        Class<?> type = data.getClass();
        String parameterString = "?";

        Set<Field> fields = Stream.of(type.getFields())// .filter(field -> field.isAccessible())
                .collect(Collectors.toSet());

        for (Field field : fields) {
            try {
                String prefix = parameterString != "?" ? "&" : "";

                parameterString += prefix
                        + URLEncoder.encode(field.getName(), java.nio.charset.StandardCharsets.UTF_8.toString()) + "="
                        + URLEncoder.encode(field.get(data).toString(),
                                java.nio.charset.StandardCharsets.UTF_8.toString());
            } catch (Exception e) {
                logger.error("Error parsing Object. Ignoring Parameter: " + field.getName(), e.getMessage());
            }
        }

        return parameterString;
    }
}
