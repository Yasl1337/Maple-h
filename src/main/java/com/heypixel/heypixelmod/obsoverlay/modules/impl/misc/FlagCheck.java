package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventHandlePacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.NotificationLevel;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;

@ModuleInfo(name = "FlagCheck", description = "Check Flag and alert.", category = Category.MISC)
public class FlagCheck extends Module {

    private int flagCount = 0;
    private float lastYaw;
    private float lastPitch;

    private void log(String message) {
        ChatUtils.addChatMessage("[FlagCheck] " + message);
    }

    BooleanValue notification = ValueBuilder.create(this, "Notification")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    BooleanValue chat = ValueBuilder.create(this, "Chat")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    @Override
    public void onEnable() {
        flagCount = 0;
        lastYaw = mc.player.getYRot();
        lastPitch = mc.player.getXRot();
    }

    @Override
    public void onDisable() {
        flagCount = 0;
    }

    @EventTarget
    public void onPacketReceive(EventHandlePacket event) {
        if (mc.player == null || mc.player.tickCount <= 25) {
            return;
        }
        

        if (event.getPacket() instanceof ClientboundPlayerPositionPacket packet) {
            flagCount++;
            float serverYaw = packet.getYRot();
            float serverPitch = packet.getXRot();
            float deltaYaw = calculateAngleDelta(serverYaw, lastYaw);
            float deltaPitch = calculateAngleDelta(serverPitch, lastPitch);
            if (deltaYaw >= 90 || deltaPitch >= 90) {
                alert("Forced Rotation", String.format("(%.1f° | %.1f°)", deltaYaw, deltaPitch));
            } else {
                alert("Rebound", "");
            }
            lastYaw = mc.player.getYRot();
            lastPitch = mc.player.getXRot();
        }
    }

    private float calculateAngleDelta(float newAngle, float oldAngle) {
        float delta = newAngle - oldAngle;
        if (delta > 180) {
            delta -= 360;
        }
        if (delta < -180) {
            delta += 360;
        }
        return Math.abs(delta);
    }

    private void alert(String reason, String extra) {
        if(chat.getCurrentValue()){
            String message;
            if (extra.isEmpty()) {
                message = String.format("§fServer detected §c%s§f, total of §c%d§f times.", reason, flagCount);
            } else {
                message = String.format("§fServer detected §c%s§f %s, total of §c%d§f times.", reason, extra, flagCount);
            }

            log(message);
        }
        if(notification.getCurrentValue()){
            String message;
            if (extra.isEmpty()) {
                message = String.format("§fServer detected §c%s§f, total of §c%d§f times.", reason, flagCount);
            } else {
                message = String.format("§fServer detected §c%s§f %s, total of §c%d§f times.", reason, extra, flagCount);
            }

            Naven.getInstance().getNotificationManager().addNotification(
                    Notification.create(message, false)
            );
        }

    }
}