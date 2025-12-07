package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdate;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.awt.Color;
import java.text.DecimalFormat;

@ModuleInfo(
        name = "AutoPlay",
        description = "Automatically joins the next game after a delay.",
        category = Category.MISC
)
public class AutoPlay extends Module {
    private final FloatValue delay = ValueBuilder.create(this, "Delay (Seconds)")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private long scheduledTime = 0;
    private long waitStartTime = 0;
    private long totalWaitTime = 0;


    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player == null || !this.isEnabled()) return;

        if (event.getPacket() instanceof ClientboundSystemChatPacket) {
            String message = ((ClientboundSystemChatPacket) event.getPacket()).content().getString();

            if (message.contains("游戏结束，请对")) {
                long delayMillis = (long) (delay.getCurrentValue() * 1000L);
                this.scheduledTime = System.currentTimeMillis() + delayMillis;
                this.waitStartTime = System.currentTimeMillis();
                this.totalWaitTime = delayMillis;
            }

            if (message.contains("正在为您匹配可用的游戏服务器")) {
                if (this.scheduledTime > 0) {
                    resetTimer();
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || !this.isEnabled()) return;

        if (this.scheduledTime > 0 && System.currentTimeMillis() >= this.scheduledTime) {
            mc.player.connection.sendCommand("again");
            ChatUtils.addChatMessage("§b[AutoPlay] §fEntering the next game.");
            resetTimer();
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (isWaiting()) {
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            DecimalFormat df = new DecimalFormat("0.0");
            float remainingSeconds = getRemainingSeconds();
            float totalSeconds = delay.getCurrentValue();
            String autoPlayText = "AutoPlay | " + df.format(remainingSeconds) + "s / " + df.format(totalSeconds) + "s";
            float textX = (float)screenWidth / 2.0F - (float)mc.font.width(autoPlayText) / 2.0F;
            float textY = (float)screenHeight / 2.0F;
            Fonts.opensans.render(event.getStack(), autoPlayText, (int)textX, (int)textY, Color.YELLOW, true, 0.4);
        }
    }

    private void resetTimer() {
        this.scheduledTime = 0;
        this.waitStartTime = 0;
        this.totalWaitTime = 0;
    }

    @Override
    public void onEnable() {
        resetTimer();
    }

    @Override
    public void onDisable() {
        resetTimer();
    }

    public boolean isWaiting() {
        return this.scheduledTime > 0;
    }

    public float getProgress() {
        if (!isWaiting() || totalWaitTime == 0) {
            return 0.0f;
        }
        long elapsedTime = System.currentTimeMillis() - this.waitStartTime;
        return Math.min(1.0f, (float) elapsedTime / (float) totalWaitTime);
    }

    public float getRemainingSeconds() {
        if (!isWaiting()) {
            return 0.0f;
        }
        long remainingMillis = this.scheduledTime - System.currentTimeMillis();
        return Math.max(0.0f, remainingMillis / 1000.0f);
    }
}