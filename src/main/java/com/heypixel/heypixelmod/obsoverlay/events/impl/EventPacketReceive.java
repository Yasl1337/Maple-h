// EventPacketReceive.java
package com.heypixel.heypixelmod.obsoverlay.events.impl;

import net.minecraft.network.protocol.Packet;

public final class EventPacketReceive extends com.heypixel.heypixelmod.obsoverlay.events.impl.Event {
    private final Packet<?> packet;
    public EventPacketReceive(Packet<?> packet) { this.packet = packet; }
    public Packet<?> getPacket() { return packet; }
}