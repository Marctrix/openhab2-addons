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
package org.openhab.binding.remoteboot.internal.api;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.openhab.binding.remoteboot.internal.api.HttpClient.RemoteBootHttpClient;
import org.openhab.binding.remoteboot.internal.api.WebSocket.LEDUpdateMessageListener;
import org.openhab.binding.remoteboot.internal.api.WebSocket.LifeCycleMessageListener;
import org.openhab.binding.remoteboot.internal.api.WebSocket.RemoteBootLEDSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Programming interface for the RemoteBoot Board.
 *
 * @author Marctrix - Initial contribution
 */
public class RemoteBootApi implements LEDUpdateMessageListener, LifeCycleMessageListener {

    private final Logger logger = LoggerFactory.getLogger(RemoteBootApi.class);

    private String webSocketEndpoint;
    private String password;

    RemoteBootHttpClient httpClient;
    WebSocketClient socketClient;
    RemoteBootLEDSocket socket;

    public Consumer<LEDUpdateEvent> onPowerUpdate;
    public Consumer<LEDUpdateEvent> onHDDUpdate;

    public RemoteBootApi(String host, int port, String password) {
        httpClient = new RemoteBootHttpClient("http://" + host + ":" + port, password);
        webSocketEndpoint = "ws://" + host + "/";
        ConnectWebSocket();
    }

    public void ConnectWebSocket() {
        socket = new RemoteBootLEDSocket();
        socket.addListener((LEDUpdateMessageListener) this);
        socket.addListener((LifeCycleMessageListener) this);

        socketClient = new WebSocketClient();
        try {
            logger.info("Connecting to " + webSocketEndpoint);
            socketClient.start();
            connectSocket();
        } catch (Exception e) {
            logger.error("Failed to start SocketConnection.", e);
        }
    }

    public void PressButton(byte button) throws IOException {
        httpClient.makeAuthQuery("/api/button/press.php", new Object() {
            public final byte button_id = button;
            public final int time = 99;
        }, Void.class);
    }

    @Override
    public void onLEDUpdateMessage(boolean[] event) {
        if (onPowerUpdate != null) {
            onPowerUpdate.accept(new LEDUpdateEvent(StateLED.Power, event[0]));
        }

        if (onHDDUpdate != null) {
            onHDDUpdate.accept(new LEDUpdateEvent(StateLED.HDD, event[1]));
        }
    }

    @Override
    public void onConnectionClose(int statusCode, String reason) {
        logger.info("Reconnecting to " + webSocketEndpoint);
        destroyConnection();
        ConnectWebSocket();
    }

    public void destroyConnection() {
        if (socket != null) {
            socket.removeListener((LEDUpdateMessageListener) this);
            socket.removeListener((LifeCycleMessageListener) this);
            socket = null;
        }

        if (socketClient != null) {
            socketClient.destroy();
            socketClient = null;
        }

        System.gc();
    }

    @Override
    public void onConnectionError(Session session, Throwable throwable) {
        logger.info("Connection Error - Reconnecting to " + webSocketEndpoint);
        destroyConnection();
        ConnectWebSocket();
    }

    void connectSocket() {
        try {
            socketClient.connect(socket, new URI(webSocketEndpoint));
        } catch (Exception e) {
            logger.error("Failed to start SocketConnection.", e);
            destroyConnection();
            ConnectWebSocket();
        }
    }
}
