package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

@ModuleInfo(
        name = "AntiVoid",
        description = "Grim-Proof Void Protection",
        category = Category.MOVEMENT
)
public class AntiVoid extends Module {

    private final Minecraft mc = Minecraft.getInstance();
    private final Random random = new Random();

    private final FloatValue distance = ValueBuilder.create(this, "Distance")
            .setDefaultFloatValue(5.0f)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0f)
            .setFloatStep(0.1f)
            .build()
            .getFloatValue();

    private final BooleanValue toggleScaffold = ValueBuilder.create(this, "Toggle Scaffold")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Grim", "Vanilla")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    private double safeX, safeY, safeZ;
    private Vec3 lastSafePosition;
    private long lastGroundTime = 0;
    private boolean isProtecting = false;
    private long protectionStartTime = 0;
    private int pullbackAttempts = 0;
    private long lastPullbackTime = 0;
    private int ticksSinceProtection = 0;
    private Vec3 lastMotion = Vec3.ZERO;
    private boolean hasValidSafePos = false;

    public AntiVoid() {}

    @Override
    public void onDisable() {
        isProtecting = false;
        hasValidSafePos = false;
    }

    @EventTarget(0)
    public void onMotion(EventMotion e) {
        if (e.getType() != EventType.PRE || mc.player == null || mc.level == null) return;

        LocalPlayer player = mc.player;

        updateSafePosition();

        if (!isProtecting) {
            checkVoidProtection();
        } else {
            handleProtectionState();
        }

        lastMotion = player.getDeltaMovement();
        ticksSinceProtection++;
    }

    private void updateSafePosition() {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (player.onGround() && hasBlockUnder(5.0)) {
            safeX = player.getX();
            safeY = player.getY();
            safeZ = player.getZ();
            lastSafePosition = new Vec3(safeX, safeY, safeZ);
            lastGroundTime = System.currentTimeMillis();
            pullbackAttempts = 0;
            hasValidSafePos = true;
        }
    }

    private void checkVoidProtection() {
        LocalPlayer player = mc.player;
        if (player == null || player.onGround() || !hasValidSafePos) {
            return;
        }

        if (hasBlockUnder(30.0)) return;

        if (shouldTriggerProtection() && canAttemptPullback()) {
            triggerProtection();
        }
    }

    private boolean shouldTriggerProtection() {
        LocalPlayer player = mc.player;
        if (player == null || lastSafePosition == null) return false;

        double fallDistance = lastSafePosition.y - player.getY();
        if (fallDistance < distance.getCurrentValue()) return false;

        long airTime = System.currentTimeMillis() - lastGroundTime;
        if (airTime < 500) return false;

        if (player.getDeltaMovement().y > -0.5) return false;

        return true;
    }

    private boolean canAttemptPullback() {
        long cooldown = 3000 + pullbackAttempts * 2000;
        if (System.currentTimeMillis() - lastPullbackTime < cooldown) return false;

        return ticksSinceProtection >= 60;
    }

    private void triggerProtection() {
        LocalPlayer player = mc.player;
        if (player == null || mc.getConnection() == null) return;

        isProtecting = true;
        protectionStartTime = System.currentTimeMillis();
        pullbackAttempts++;
        lastPullbackTime = System.currentTimeMillis();
        ticksSinceProtection = 0;

        double targetX = safeX;
        double targetY = safeY + 0.1;
        double targetZ = safeZ;

        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                targetX, targetY, targetZ, true
        ));

        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                targetX, targetY, targetZ, player.onGround()
        ));
    }

    private void handleProtectionState() {
        LocalPlayer player = mc.player;
        if (player == null) {
            isProtecting = false;
            return;
        }

        long protectionTime = System.currentTimeMillis() - protectionStartTime;

        if (protectionTime > 2000 || player.onGround()) {
            isProtecting = false;
            return;
        }
    }

    private boolean hasBlockUnder(double range) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return false;

        return !mc.level.getBlockStates(player.getBoundingBox().move(0, -range, 0))
                .allMatch(state -> state.isAir());
    }
}
