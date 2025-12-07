package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventTyped;

public class EventJoinWorld extends EventTyped {
    public EventJoinWorld(byte eventType) {
        super(eventType);
    }
}