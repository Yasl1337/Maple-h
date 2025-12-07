package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.ChatUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;

@ModuleInfo(
        name = "DelayTrack",
        description = "Stuck Network,but adversaries",
        category = Category.COMBAT
)
public class DelayTrack extends Module {
    public BooleanValue log = ValueBuilder.create(this, "Logging").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue killAuraEnableCheck = ValueBuilder.create(this, "KillAura Enable Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue OnGroundStop = ValueBuilder.create(this, "On Ground Stop")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public FloatValue maxpacket = ValueBuilder.create(this, "Max Packet number")
            .setDefaultFloatValue(75.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(500.0F)
            .build()
            .getFloatValue();
    FloatValue range = ValueBuilder.create(this, "Range")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(8.0F)
            .build()
            .getFloatValue();
    FloatValue delay = ValueBuilder.create(this, "Delay(Tick)")
            .setDefaultFloatValue(17.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(100.0F)
            .build()
            .getFloatValue();
    public BooleanValue btrender = ValueBuilder.create(this, "Render")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
//    public ModeValue btrendermode = ValueBuilder.create(this, "Render Mode")
//            .setVisibility(this.btrender::getCurrentValue)
//            .setDefaultModeIndex(0)
//            .setModes(new String[]{"Normal", "LingDong"})
//            .build()
//            .getModeValue();
    public FloatValue textSize = ValueBuilder.create(this, "Text Size")
            .setVisibility(this.btrender::getCurrentValue)
            .setDefaultFloatValue(0.35F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
//    public FloatValue progressWidthCfg = ValueBuilder.create(this, "Progress Width")
//            .setVisibility(this.btrender::getCurrentValue)
//            .setDefaultFloatValue(200.0F)
//            .setFloatStep(5.0F)
//            .setMinFloatValue(50.0F)
//            .setMaxFloatValue(600.0F)
//            .build()
//            .getFloatValue();
//    public FloatValue progressHeightCfg = ValueBuilder.create(this, "Progress Height")
//            .setVisibility(this.btrender::getCurrentValue)
//            .setDefaultFloatValue(10.0F)
//            .setFloatStep(1.0F)
//            .setMinFloatValue(4.0F)
//            .setMaxFloatValue(40.0F)
//            .build()
//            .getFloatValue();
    public BooleanValue targetNearbyCheck = ValueBuilder.create(this, "Target Nearby Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public ModeValue interceptMode = ValueBuilder.create(this, "Intercept Mode")
            .setDefaultModeIndex(1)
            .setModes(new String[]{"Original", "AllExceptSelf", "OnlyInterceptTarget"})
            .build()
            .getModeValue();
    public BooleanValue debugFilter = ValueBuilder.create(this, "Dev Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    public BooleanValue releaseWhenTP = ValueBuilder.create(this, "Release When TP/BPS Abnormal")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public FloatValue bpsThreshold = ValueBuilder.create(this, "BPS Threshold")
            .setDefaultFloatValue(12.0F)
            .setFloatStep(0.5F)
            .setMinFloatValue(5.0F)
            .setMaxFloatValue(50.0F)
            .build()
            .getFloatValue();
    public BooleanValue botCheck = ValueBuilder.create(this, "Bot Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue teamCheck = ValueBuilder.create(this, "Team Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    public BooleanValue onlyCombat = ValueBuilder.create(this, "Only Combat")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public boolean btwork = false;
    private final LinkedBlockingDeque<Packet<?>> airKBQueue = new LinkedBlockingDeque<>();
    private final List<Integer> knockbackPositions = new ArrayList<>();
    private boolean isInterceptingAirKB = false;
    private int interceptedPacketCount = 0;
    private int delayTicks = 0;
    private boolean shouldCheckGround = false;
    public String trackingText = "";
    private float progressBarAnimation = 0f;
    private float textAnimation = 0f;
    private static final int mainColor =(HUD.headerColor);
    private long lastAnimationUpdate = 0;
    private double lastPosX = Double.NaN;
    private double lastPosZ = Double.NaN;
    private Integer currentTargetId = null;
    private int applyAlpha(int color, float alpha) {
        int a = (int)(((color >> 24) & 0xFF) * alpha);
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void onEnable() {
        this.reset();
        this.progressBarAnimation = 0f;
        this.textAnimation = 0f;
        this.lastAnimationUpdate = 0;

        if (mc != null && mc.player != null) {
            this.lastPosX = mc.player.getX();
            this.lastPosZ = mc.player.getZ();
        } else {
            this.lastPosX = Double.NaN;
            this.lastPosZ = Double.NaN;
        }
    }

    public void onDisable() {
        this.reset();
        this.progressBarAnimation = 0f;
        this.textAnimation = 0f;
        this.lastAnimationUpdate = 0;
    }

    public int getPacketCount() {
        return this.airKBQueue.size();
    }

    public void reset() {
        this.releaseAirKBQueue();
        this.isInterceptingAirKB = false;
        this.interceptedPacketCount = 0;
        this.delayTicks = 0;
        this.shouldCheckGround = false;
        this.btwork = false;
        this.knockbackPositions.clear();
        this.lastPosX = Double.NaN;
        this.lastPosZ = Double.NaN;
        this.currentTargetId = null;
    }

    private boolean isAuraEnabledOrBypassed() {
        if (!this.killAuraEnableCheck.getCurrentValue()) {
            return true;
        } else {
            try {
                return Naven.getInstance().getModuleManager().getModule(Aura.class).isEnabled();
            } catch (Exception var2) {
                return false;
            }
        }
    }

    private boolean shouldWork() {
        if (onlyCombat.getCurrentValue()) {
            try {
                Aura auraModule = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
                if (auraModule == null || !auraModule.isEnabled() || Aura.target == null) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    private void updateCurrentTarget() {
        if (mc != null && mc.level != null && mc.player != null) {
            Entity dist = Aura.target;
            if (dist instanceof Player) {
                Player p = (Player)dist;
                if (p.isAlive()) {
                    if (botCheck.getCurrentValue() && isBot(p)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a bot, skipping: id=" + p.getId());
                        }
                        this.currentTargetId = null;
                        return;
                    }
                    if (teamCheck.getCurrentValue() && isTeammate(p)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a teammate, skipping: id=" + p.getId());
                        }
                        this.currentTargetId = null;
                        return;
                    }

                    this.currentTargetId = p.getId();
                    if (this.debugFilter.getCurrentValue()) {
                        double distance = mc.player.distanceTo(p);
                        this.log("Target Update (Aura.target): id=" + this.currentTargetId + ", dist=" + String.format("%.2f", distance));
                    }
                    return;
                }
            }

            dist = Aura.aimingTarget;
            if (dist instanceof Player) {
                Player p2 = (Player)dist;
                if (p2.isAlive()) {
                    if (botCheck.getCurrentValue() && isBot(p2)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a bot, skipping: id=" + p2.getId());
                        }
                        this.currentTargetId = null;
                        return;
                    }
                    if (teamCheck.getCurrentValue() && isTeammate(p2)) {
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("Target is a teammate, skipping: id=" + p2.getId());
                        }
                        this.currentTargetId = null;
                        return;
                    }

                    this.currentTargetId = p2.getId();
                    if (this.debugFilter.getCurrentValue()) {
                        double distance = mc.player.distanceTo(p2);
                        this.log("Target Update (Aura.aimingTarget): id=" + this.currentTargetId + ", dist=" + String.format("%.2f", distance));
                    }
                    return;
                }
            }

            this.currentTargetId = null;
            if (this.debugFilter.getCurrentValue()) {
                this.log("Target Update: none");
            }
        } else {
            this.currentTargetId = null;
        }
    }
    private boolean isBot(Entity entity) {
        try {
            AntiBots antiBotsModule = (AntiBots) Naven.getInstance().getModuleManager().getModule(AntiBots.class);
            if (antiBotsModule != null && antiBotsModule.isEnabled()) {
                return AntiBots.isBot(entity) || AntiBots.isBedWarsBot(entity);
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isTeammate(Entity entity) {
        try {
            Teams teamsModule = (Teams) Naven.getInstance().getModuleManager().getModule(Teams.class);
            if (teamsModule != null && teamsModule.isEnabled()) {
                return Teams.isSameTeam(entity);
            }
        } catch (Exception e) {
        }
        return false;
    }

    private boolean isTargetRelatedPacket(Packet<?> packet) {
        if (this.currentTargetId == null) {
            return false;
        } else {
            try {
                if (packet instanceof ClientboundSetEntityMotionPacket) {
                    ClientboundSetEntityMotionPacket motion = (ClientboundSetEntityMotionPacket)packet;
                    return motion.getId() == this.currentTargetId;
                }

                Method m = packet.getClass().getMethod("getId");
                if (m.getReturnType() == Integer.TYPE) {
                    int id = (Integer)m.invoke(packet);
                    boolean match = id == this.currentTargetId;
                    if (this.debugFilter.getCurrentValue()) {
                        this.log("[OnlyInterceptTarget] getId Checker: packet=" + packet.getClass().getSimpleName() + ", id=" + id + ", match=" + match);
                    }
                    return match;
                }
            } catch (NoSuchMethodException var5) {
            } catch (Exception var6) {
                if (this.debugFilter.getCurrentValue()) {
                    this.log("[OnlyInterceptTarget] Reflection acquisition failed: " + var6.getClass().getSimpleName());
                }
            }
            return false;
        }
    }

    private void releaseAirKBQueue() {
        int packetCount = this.airKBQueue.size();

        while(!this.airKBQueue.isEmpty()) {
            try {
                Packet<ClientPacketListener> packet = (Packet<ClientPacketListener>) this.airKBQueue.poll();
                if (packet != null && mc.getConnection() != null) {
                    packet.handle(mc.getConnection());
                }
            } catch (Exception var31) {
                var31.printStackTrace();
            }
        }

        if (packetCount > 0) {
            this.log("Release " + packetCount + " Packets");
        }

        this.interceptedPacketCount = 0;
        this.knockbackPositions.clear();
    }

    private boolean hasNearbyPlayers(float range) {
        if (mc.level != null && mc.player != null) {
            for(Player player : mc.level.players()) {
                if (player != mc.player && player.isAlive() && mc.player.distanceTo(player) <= range) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    private void log(String message) {
        if (this.log.getCurrentValue()) {
            ChatUtils.addChatMessage("Tick" + message);
        }
    }

    private boolean isSelfRelatedPacket(Packet<?> packet) {
        if (packet instanceof ClientboundPlayerPositionPacket) {
            return true;
        } else if (packet instanceof ClientboundSetEntityMotionPacket) {
            return false;
        } else {
            try {
                Method m = packet.getClass().getMethod("getId");
                if (m.getReturnType() == Integer.TYPE && mc.player != null) {
                    int id = (Integer)m.invoke(packet);
                    boolean self = id == mc.player.getId();
                    if (this.debugFilter.getCurrentValue()) {
                        this.log("getId Checker: packet=" + packet.getClass().getSimpleName() + ", id=" + id + ", self=" + self);
                    }
                    if (self) {
                        return true;
                    }
                }
            } catch (NoSuchMethodException var5) {
            } catch (Exception var6) {
                if (this.debugFilter.getCurrentValue()) {
                    this.log("Reflection acquisition failed: " + var6.getClass().getSimpleName());
                }
            }
            return false;
        }
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (mc.player != null) {
            if (!shouldWork()) {
                if (this.isInterceptingAirKB || this.shouldCheckGround) {
                    this.releaseAirKBQueue();
                    this.resetAfterRelease();
                }
                this.btwork = false;
                return;
            }

            if (!this.isAuraEnabledOrBypassed()) {
                if (this.isInterceptingAirKB || this.shouldCheckGround) {
                    this.releaseAirKBQueue();
                    this.resetAfterRelease();
                }
                this.btwork = false;
            } else {
                this.btwork = this.isInterceptingAirKB || this.shouldCheckGround;
                this.updateCurrentTarget();
                if (this.isInterceptingAirKB && this.releaseWhenTP.getCurrentValue()) {
                    double curX = mc.player.getX();
                    double curZ = mc.player.getZ();
                    if (!Double.isNaN(this.lastPosX) && !Double.isNaN(this.lastPosZ)) {
                        double dx = curX - this.lastPosX;
                        double dz = curZ - this.lastPosZ;
                        double horizDist = Math.sqrt(dx * dx + dz * dz);
                        double bps = horizDist * 20.0D;
                        if (bps > (double)this.bpsThreshold.getCurrentValue()) {
                            this.log("BPS Error! (" + String.format("%.2f", bps) + ")，Release All Packets");
                            this.isInterceptingAirKB = false;
                            this.shouldCheckGround = false;
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    }
                    this.lastPosX = curX;
                    this.lastPosZ = curZ;
                } else if (mc.player != null) {
                    this.lastPosX = mc.player.getX();
                    this.lastPosZ = mc.player.getZ();
                }

                if (this.delayTicks > 0) {
                    --this.delayTicks;
                } else {
                    boolean shouldStartByNearby = !this.targetNearbyCheck.getCurrentValue() || this.hasNearbyPlayers(this.range.getCurrentValue());
                    if (!this.isInterceptingAirKB && shouldStartByNearby) {
                        this.isInterceptingAirKB = true;
                        this.shouldCheckGround = false;
                        this.interceptedPacketCount = 0;
                        this.airKBQueue.clear();
                        this.knockbackPositions.clear();
                        if (this.targetNearbyCheck.getCurrentValue()) {
                            this.log("Checker Player(<= " + this.range.getCurrentValue() + ")，Start intercepting");
                        } else {
                            this.log("Start intercepting");
                        }
                    }

                    if (this.isInterceptingAirKB && (float)this.interceptedPacketCount >= this.maxpacket.getCurrentValue()) {
                        if (this.OnGroundStop.getCurrentValue()) {
                            this.shouldCheckGround = true;
                            this.log("Wait Player OnGround");
                        } else {
                            this.log("Release All Packets");
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    }
                }
            }
        }
    }

    @EventTarget
    public void onRender2D(EventRender2D event) {
        if (this.isEnabled()) {
            this.render(event.getGuiGraphics());
        }
    }

    private void resetAfterRelease() {
        this.isInterceptingAirKB = false;
        this.shouldCheckGround = false;
        this.delayTicks = (int)this.delay.getCurrentValue();
        this.log("DelayTrack Delay: " + this.delayTicks + " ticks");
    }

    @EventTarget
    public void onPacket(EventPacket event) {
        if (mc.player != null && mc.getConnection() != null) {
            if (event.getType() == EventType.RECEIVE) {
                // 检查Only Combat选项
                if (!shouldWork()) {
                    if (this.isInterceptingAirKB || this.shouldCheckGround) {
                        this.releaseAirKBQueue();
                        this.resetAfterRelease();
                    }
                    return;
                }

                if (!this.isAuraEnabledOrBypassed()) {
                    if (this.isInterceptingAirKB || this.shouldCheckGround) {
                        this.releaseAirKBQueue();
                        this.resetAfterRelease();
                    }
                } else {
                    Packet<?> packet = event.getPacket();
                    if (packet instanceof ClientboundPlayerPositionPacket) {
                        if (this.releaseWhenTP.getCurrentValue()) {
                            this.isInterceptingAirKB = false;
                            this.shouldCheckGround = false;
                            this.log("Checked TP, Release All Packets");
                            this.releaseAirKBQueue();
                            this.resetAfterRelease();
                        }
                    } else if (packet instanceof ClientboundSetEntityMotionPacket) {
                        ClientboundSetEntityMotionPacket motionPacket = (ClientboundSetEntityMotionPacket)packet;
                        if (motionPacket.getId() == mc.player.getId() && this.isInterceptingAirKB) {
                            event.setCancelled(true);
                            this.airKBQueue.add(packet);
                            ++this.interceptedPacketCount;
                            this.knockbackPositions.add(this.airKBQueue.size() - 1);
                            this.log("Intercepting KnockBack Packets #" + this.interceptedPacketCount);
                        }
                    } else if (this.isInterceptingAirKB) {
                        boolean skipSelf = this.interceptMode.isCurrentMode("AllExceptSelf") && this.isSelfRelatedPacket(packet);
                        if (skipSelf) {
                            if (this.debugFilter.getCurrentValue()) {
                                this.log("[AllExceptSelf] Skip Packet Of Myself: " + packet.getClass().getSimpleName());
                            }
                            return;
                        }

                        if (this.interceptMode.isCurrentMode("OnlyInterceptTarget") && !this.isTargetRelatedPacket(packet)) {
                            if (this.debugFilter.getCurrentValue()) {
                                this.log("[OnlyInterceptTarget] Skip Non-Target Packet: " + packet.getClass().getSimpleName());
                            }
                            return;
                        }

                        event.setCancelled(true);
                        this.airKBQueue.add(packet);
                        ++this.interceptedPacketCount;
                        if (this.debugFilter.getCurrentValue()) {
                            this.log("[" + this.interceptMode.getCurrentMode() + "] Intercepting Normal Packets #" + this.interceptedPacketCount + ": " + packet.getClass().getSimpleName());
                        }
                    }
                }
            }
        }
    }

    public void render(GuiGraphics guiGraphics) {
        long currentTime = System.currentTimeMillis();
        if (lastAnimationUpdate == 0) {
            lastAnimationUpdate = currentTime;
        }
        long elapsed = currentTime - lastAnimationUpdate;
        lastAnimationUpdate = currentTime;
        float animationSpeed = Math.min(1.0f, elapsed / 16.0f);
        if (this.isInterceptingAirKB || this.shouldCheckGround) {
            progressBarAnimation = Math.min(1.0f, progressBarAnimation + 0.1f * animationSpeed);
            textAnimation = Math.min(1.0f, textAnimation + 0.15f * animationSpeed);
            this.trackingText = "Tracking...";
        } else {
            progressBarAnimation = Math.max(0.0f, progressBarAnimation - 0.2f * animationSpeed);
            textAnimation = Math.max(0.0f, textAnimation - 0.25f * animationSpeed);
        }

        if (progressBarAnimation > 0 || textAnimation > 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;

            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();
            float barWidth = 100.0F;
            float barHeight = 5.0F;
            float x = ((float)screenWidth - barWidth) / 2.0F;
            float y = (float)screenHeight / 2.0F + 100.0F;

            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();

            float animatedProgress = progressBarAnimation;
            float maxPacketValue = Math.max(1.0F, this.maxpacket.getCurrentValue());
            float progress = Math.min(1.0F, (float)this.interceptedPacketCount / maxPacketValue);
            float progressWidthPx = barWidth * progress;
            RenderUtils.drawRoundedRect(poseStack, x, y, barWidth, barHeight, 2.0F, Integer.MIN_VALUE);
            if (progressWidthPx > 0.0F) {
                float animatedProgressWidth = progressWidthPx * easeOutElastic(animatedProgress);
                RenderUtils.drawRoundedRect(poseStack, x, y, animatedProgressWidth, barHeight, 2.0F, mainColor);
            }
            if (this.OnGroundStop.getCurrentValue() && (float)this.interceptedPacketCount > this.maxpacket.getCurrentValue()) {
                float overflowProgress = ((float)this.interceptedPacketCount - this.maxpacket.getCurrentValue()) / maxPacketValue;
                float overflowWidth = Math.min(barWidth * overflowProgress, barWidth);
                float animatedOverflowWidth = overflowWidth * easeOutElastic(animatedProgress);
                int overflowColor = (int)(255 * animatedProgress) << 24 | (Color.RED.getRGB() & 0x00FFFFFF);
                RenderUtils.drawRoundedRect(poseStack, x + barWidth - animatedOverflowWidth, y, animatedOverflowWidth, barHeight, 2.0F, overflowColor);
            }
            if (textAnimation > 0) {
                float textScale = Math.max(0.05F, this.textSize.getCurrentValue());
                float animatedTextScale = textScale * (0.8f + 0.2f * easeOutElastic(textAnimation));
                int textAlpha = (int)(255 * textAnimation);
                float textWidth = Fonts.harmony.getWidth(this.trackingText, (double)animatedTextScale);
                float textX = ((float)screenWidth - textWidth) / 2.0F;
                float textY = y - 25.0F;
                Color textColor = new Color(255, 255, 255, textAlpha);
                Fonts.harmony.render(poseStack, this.trackingText, (double)textX, (double)textY, textColor, false, (double)animatedTextScale);
            }

            poseStack.popPose();
        }
    }

    private float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    private float easeInOutSine(float x) {
        return (float) (-(Math.cos(Math.PI * x) - 1) / 2);
    }

    private float easeOutElastic(float x) {
        float c4 = (float) ((2 * Math.PI) / 3);
        return x == 0 ? 0 : x == 1 ? 1 : (float) (Math.pow(2, -10 * x) * Math.sin((x * 10 - 0.75) * c4) + 1);
    }
    }