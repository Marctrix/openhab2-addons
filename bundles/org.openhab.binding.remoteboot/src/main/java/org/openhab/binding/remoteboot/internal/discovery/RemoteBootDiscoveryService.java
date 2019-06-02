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

import static org.openhab.binding.remoteboot.internal.RemoteBootBindingConstants.*;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service scans the lokal network for RemoteBoot devices.
 *
 * @author Marc Wiechmann - Initial contribution
 */

@NonNullByDefault
public class RemoteBootDiscoveryService extends AbstractDiscoveryService {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Stream.of(THING_TYPE_REMOTEBOOT)
            .collect(Collectors.toSet());

    private final Logger logger = LoggerFactory.getLogger(RemoteBootDiscoveryService.class);

    ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
    private NetworkScan networkScan = new NetworkScan(address -> onRemoteBootFound(address));

    public RemoteBootDiscoveryService() {
        super(SUPPORTED_THING_TYPES, 60, true);
    }

    private void onRemoteBootFound(InetAddress address) {
        publishDiscovery(address);
    }

    @SuppressWarnings("unchecked")
    public void activate() {
        timer.scheduleWithFixedDelay(networkScan, 0, 5, TimeUnit.MINUTES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void deactivate() {
        timer.shutdown();
    }

    @Override
    protected void startScan() {
        timer.submit(networkScan);
    }

    private void publishDiscovery(InetAddress host) {
        DiscoveryResult result = DiscoveryResultBuilder
                .create(new ThingUID(BINDING_ID + ":" + THING_TYPE_REMOTEBOOT.getId() + ":Remoteboot_"
                        + host.getHostAddress().replaceAll("\\.", "_")))
                .withProperty("host", host.getHostAddress()).withThingType(THING_TYPE_REMOTEBOOT)
                .withLabel("RemoteBoot on " + host.getHostName()).build();

        thingDiscovered(result);
    }
}
