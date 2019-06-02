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
package org.openhab.binding.remoteboot.internal.api.WebSocket;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.event.EventListenerList;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Socket for the LED Updates.
 *
 * @author Marctrix - Initial contribution
 */
@WebSocket(maxTextMessageSize = 64 * 1024, maxIdleTime = 5000)
public class RemoteBootLEDSocket {
    private final CountDownLatch closeLatch;
    private final Logger logger = LoggerFactory.getLogger(RemoteBootLEDSocket.class);

    EventListenerList socketListeners = new EventListenerList();
    EventListenerList lifeCylcleEventListeners = new EventListenerList();

    public void addListener(LEDUpdateMessageListener listener) {
        socketListeners.add(LEDUpdateMessageListener.class, listener);
    }

    public void removeListener(LEDUpdateMessageListener listener) {
        socketListeners.remove(LEDUpdateMessageListener.class, listener);
    }

    public void addListener(LifeCycleMessageListener listener) {
        lifeCylcleEventListeners.add(LifeCycleMessageListener.class, listener);
    }

    public void removeListener(LifeCycleMessageListener listener) {
        lifeCylcleEventListeners.remove(LifeCycleMessageListener.class, listener);
    }

    @SuppressWarnings("unused")
    private Session session;

    public RemoteBootLEDSocket() {
        this.closeLatch = new CountDownLatch(1);
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        logger.info(String.format("Connection closed: %d - %s%n", statusCode, reason));
        this.session = null;
        this.closeLatch.countDown(); // trigger latch

        Object[] listeners = lifeCylcleEventListeners.getListenerList();

        for (Object listener : listeners) {
            if (listener instanceof LifeCycleMessageListener) {
                ((LifeCycleMessageListener) listener).onConnectionClose(statusCode, reason);
            }
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable throwable) {
        logger.error("Websocket connection failed.", throwable);

        Object[] listeners = lifeCylcleEventListeners.getListenerList();

        for (Object listener : listeners) {
            if (listener instanceof LifeCycleMessageListener) {
                ((LifeCycleMessageListener) listener).onConnectionError(session, throwable);
            }
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        logger.info(String.format("Got connect: %s%n", session));
    }

    @OnWebSocketMessage
    @SuppressWarnings("unchecked")
    public void onMessage(String msg) {
        logger.info("Received message");
        try {
            ObjectMapper mapper = new ObjectMapper();
            boolean[] event = mapper.readValue(msg.replace("l:", ""), boolean[].class);

            Object[] listeners = socketListeners.getListenerList();

            for (Object listener : listeners) {
                if (listener instanceof LEDUpdateMessageListener) {
                    ((LEDUpdateMessageListener) listener).onLEDUpdateMessage(event);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e.getStackTrace());
        }
    }
}