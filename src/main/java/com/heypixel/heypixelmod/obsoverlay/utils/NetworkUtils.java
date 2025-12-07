package com.heypixel.heypixelmod.obsoverlay.utils;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventGlobalPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NetworkUtils {
   public static Set<Packet<?>> passthroughsPackets = new HashSet<>();
   private static final TimeHelper timer = new TimeHelper();

   private static long totalTime = 0L;
   public static final Logger LOGGER = LogManager.getLogger("PacketUtil");

   public static boolean isServerLag() {
      return timer.delay(500.0);
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         if (isServerLag()) {
            totalTime = Math.round(timer.getLastDelay());
         } else {
         }
      }
   }

   public static void sendPacket(Packet<?> packet) {
      if (packet == null || Minecraft.getInstance().getConnection() == null) {
         return;
      }

      EventPacket event = new EventPacket(EventType.PRE, packet);
      Naven.getInstance().getEventManager().call(event);

      if (!event.isCancelled()) {
         Minecraft.getInstance().getConnection().send(packet);
      }
   }

   public static void sendPacketNoEvent(Packet<?> packet) {
      LOGGER.info("Sending: " + packet.getClass().getName());
      if (packet instanceof ServerboundCustomPayloadPacket sb) {
         LOGGER.info("RE custompayload, {}", sb.getIdentifier().toString());
         if (sb.getIdentifier().toString().equals("heypixelmod:s2cevent")) {
            FriendlyByteBuf data = sb.getData();
            data.markReaderIndex();
            int id = data.readVarInt();
            LOGGER.info("after packet ({}", id);
            if (id == 2) {
               LOGGER.info("after packet");
               LOGGER.info(Arrays.toString(MixinProtectionUtils.readByteArray(data, data.readableBytes())));
            }

            data.resetReaderIndex();
         }
      }

      passthroughsPackets.add(packet);
      Minecraft.getInstance().getConnection().send(packet);
   }

   @EventTarget(4)
   public void onGlobalPacket(EventGlobalPacket e) {
      if (e.getPacket() instanceof ClientboundPingPacket
              || e.getPacket() instanceof ClientboundMoveEntityPacket
              || e.getPacket() instanceof ClientboundSetTimePacket
              || e.getPacket() instanceof ClientboundSetPlayerTeamPacket) {
         timer.reset();
      }

      if (!e.isCancelled()) {
         Packet<?> packet = e.getPacket();
         EventPacket event = new EventPacket(e.getType(), packet);
         Naven.getInstance().getEventManager().call(event);
         if (event.isCancelled()) {
            e.setCancelled(true);
         }

         e.setPacket(event.getPacket());
      }
   }
}