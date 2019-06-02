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
package org.openhab.binding.remoteboot.internal;

import static org.openhab.binding.remoteboot.internal.RemoteBootBindingConstants.*;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.remoteboot.internal.api.RemoteBootApi;
import org.openhab.binding.remoteboot.internal.api.HttpClient.Buttons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RemoteBootHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marctrix - Initial contribution
 */
@NonNullByDefault
public class RemoteBootHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(RemoteBootHandler.class);

    @Nullable
    private RemoteBootConfiguration config;

    @Nullable
    private RemoteBootApi api;

    public RemoteBootHandler(Thing thing) {
        super(thing);

        if (isInitialized()) {
            StartUp();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            return; // Handled reactive!
        }

        String id = channelUID.getId();

        switch (id) {
            case CHANNEL_POWER:
                Press(Buttons.Power);
                break;
            case CHANNEL_RESET:
                Press(Buttons.Reset);
                break;
            default:
                logger.warn("Channel {} not supported on the RemoteBoot.", id);
        }
    }

    private void Press(byte button) {
        try {
            if (this.thing.getStatus() == ThingStatus.ONLINE) {
                api.PressButton(button);
            }
        } catch (IOException ex) {
            logger.error("Could not switch Button", ex);
        }
    }

    @Override
    public void initialize() {
        logger.info("Initializing Remote Boot.");
        StartUp();
    }

    public void StartUp() {
        logger.info("Calling Startup");

        config = getConfigAs(RemoteBootConfiguration.class);

        api = new RemoteBootApi(config.host, config.port, config.password);

        api.onPowerUpdate = (event) -> {
            this.updateState(CHANNEL_POWER, event.IsPowered ? OnOffType.ON : OnOffType.OFF);
            this.updateState(CHANNEL_RESET, event.IsPowered ? OnOffType.ON : OnOffType.OFF);
            this.updateState(CHANNEL_POWERLED, event.IsPowered ? OpenClosedType.OPEN : OpenClosedType.CLOSED);
        };

        api.onHDDUpdate = (event) -> this.updateState(CHANNEL_HDDLED,
                event.IsPowered ? OpenClosedType.OPEN : OpenClosedType.CLOSED);

        updateStatus(ThingStatus.ONLINE);
        logger.info("Startup finished");
    }

    @Override
    public void dispose() {
        api.onPowerUpdate = null;
        api.onHDDUpdate = null;
        super.dispose();
    }

}
