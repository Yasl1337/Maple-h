package com.heypixel.heypixelmod.obsoverlay.events.impl;

import com.heypixel.heypixelmod.obsoverlay.events.api.events.Event;
import com.heypixel.heypixelmod.obsoverlay.events.api.events.callables.EventTyped;

public class EventLeaveWorld extends EventTyped {
    public EventLeaveWorld(byte eventType) {
        super(eventType);
    }
}