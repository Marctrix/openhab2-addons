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
package org.openhab.binding.remoteboot.internal.discovery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the local Network for RemoteBoot Devices. Currently only works for class C networks.
 *
 * @author Marctrix - Initial contribution
 */
public class NetworkScan implements Runnable {

    private String subnet;

    private final Logger logger = LoggerFactory.getLogger(NetworkScan.class);

    public Consumer<InetAddress> onRemoteBootDiscovered;

    public NetworkScan() {
        try {
            subnet = getCurrentSubnet();
        } catch (UnknownHostException e) {
            logger.error("Can't determin current Subnet", e);
        }
    }

    public NetworkScan(Consumer<InetAddress> onSuccess) {
        this();
        onRemoteBootDiscovered = onSuccess;
    }

    @Override
    public void run() {
        logger.info("Scanning for new RemoteBoot devices.");

        GenerateIPList().stream().forEach(address -> {
            try {
                if (address.isReachable(200) && isRemoteBoot(address) && onRemoteBootDiscovered != null) {
                    onRemoteBootDiscovered.accept(address);
                }
            } catch (IOException e) {
                logger.warn("Error while trying to reach host " + address.getHostAddress(), e);
            }
        });

        logger.info("Scan for new RemoteBoot devices finished.");
    }

    private List<InetAddress> GenerateIPList() {
        List<InetAddress> hosts = new ArrayList<InetAddress>();

        int timeout = 100;
        for (int i = 1; i < 255; i++) {
            String host = subnet + "." + i;
            try {
                hosts.add(InetAddress.getByName(host));
            } catch (UnknownHostException e) {
                logger.error("Unknown host " + host, e);
            }
        }

        return hosts;
    }

    private boolean isRemoteBoot(InetAddress host) {
        try {
            URL url = new URL("http://" + host.getHostAddress());

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            return response.toString().contains("<title>Remoteboot Web Interface - Overview</title>");

        } catch (Exception e) {
            return false;
        }
    }

    private String getCurrentSubnet() throws UnknownHostException {

        String[] subnetParts = DetectOutboundIp().split("\\.");
        String subnet = String.join(".", Stream.of(subnetParts).limit(3).toArray(String[]::new));

        return subnet;
    }

    private String DetectOutboundIp() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
}