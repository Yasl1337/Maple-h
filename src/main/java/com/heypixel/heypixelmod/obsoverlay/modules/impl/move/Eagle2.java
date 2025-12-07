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
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.item.ItemStack;
/*
     author Yasl
 */

/// 为什么写这个？Eagle不检测副手方块也是笑了,ai二改
@ModuleInfo(
        name = "Eagle2",
        description = "Save Eagle2",
        category = Category.MOVEMENT
)
public class Eagle2 extends Module {
    private final FloatValue edgeSensitivity = ValueBuilder.create(this, "Edge Sensitivity")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    private final BooleanValue autoSwitchBlocks = ValueBuilder.create(this, "Auto Switch Blocks")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private int originalSlot = -1;
    private boolean isSneaking = false;

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE && mc.player != null) {

            boolean shouldSneak = mc.player.onGround() && isOnBlockEdge(edgeSensitivity.getCurrentValue());
            if (shouldSneak != isSneaking) {
                mc.options.keyShift.setDown(shouldSneak);
                isSneaking = shouldSneak;
            }


            if (autoSwitchBlocks.getCurrentValue()) {
                handleBlockSwitching();
            }
        }
    }

    private void handleBlockSwitching() {
        ItemStack offhandStack = null;
        if (mc.player != null) {
            offhandStack = mc.player.getOffhandItem();
        }
        if (Scaffold.isValidStack(offhandStack)) {
            resetSlot();
            return;
        }

        // 搜搜看看
        int blockSlot = findBlockSlot();
        if (blockSlot != -1 && mc.player.getInventory().selected != blockSlot) {
            if (originalSlot == -1) {
                originalSlot = mc.player.getInventory().selected;
            }
            mc.player.getInventory().selected = blockSlot;
        } else if (blockSlot == -1) {
            resetSlot();
        }
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = null;
            if (mc.player != null) {
                stack = mc.player.getInventory().getItem(i);
            }
            if (Scaffold.isValidStack(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void resetSlot() {
        if (mc.player != null && originalSlot != -1 && originalSlot != mc.player.getInventory().selected) {
            mc.player.getInventory().selected = originalSlot;
            originalSlot = -1;
        }
    }

    @Override
    public void onDisable() {

        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyShift.setDown(isHoldingShift);
        isSneaking = false;


        resetSlot();
    }
}