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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link RemoteBootBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marctrix - Initial contribution
 */

@NonNullByDefault
public class RemoteBootBindingConstants {

    public static final String BINDING_ID = "remoteboot";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_REMOTEBOOT = new ThingTypeUID(BINDING_ID, "remoteboot");

    public static final ThingTypeUID THING_TYPE_POWER = new ThingTypeUID(BINDING_ID, "power");
    public static final ThingTypeUID THING_TYPE_RESET = new ThingTypeUID(BINDING_ID, "reset");
    public static final ThingTypeUID THING_TYPE_HDDLED = new ThingTypeUID(BINDING_ID, "hddled");
    public static final ThingTypeUID THING_TYPE_POWERLED = new ThingTypeUID(BINDING_ID, "powerled");

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_RESET = "reset";
    public static final String CHANNEL_HDDLED = "hddled";
    public static final String CHANNEL_POWERLED = "powerled";

}
