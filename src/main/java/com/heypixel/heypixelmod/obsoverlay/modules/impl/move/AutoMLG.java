package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(name = "AutoMLG", description = "100% Water MLG + Auto Recycle", category = Category.MOVEMENT)
public class AutoMLG extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    private final FloatValue fallDistance = ValueBuilder.create(this, "Fall Distance")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();

    // 状态变量
    private int waterSlot = -1;
    private int originalSlot = -1;
    private BlockPos waterPos = null;
    private boolean placed = false; // 私有变量，通过getter访问
    private int recycleTicks = 0;

    // 新增：提供placed状态的公共访问方法（供RotationManager使用）
    public boolean isPlaced() {
        return placed;
    }

    @EventTarget
    public void onTick(EventRunTicks e) {
        if (e.getType() != EventType.POST || mc.player == null || mc.level == null) return;

        // 1. 检测坠落并放置水
        if (shouldPlaceWater()) {
            if (findWaterBucket() && !placed) {
                switchToWater();
                placeWater();
            }
        }

        // 2. 自动回收水
        handleWaterRecycle();
    }

    /**
     * 判断是否需要放置水
     */
    private boolean shouldPlaceWater() {
        return mc.player.fallDistance > fallDistance.getCurrentValue()
                && !mc.player.onGround()
                && !mc.player.isInWater(); // 增加在水中不放置的判断
    }

    /**
     * 处理水的回收逻辑
     */
    private void handleWaterRecycle() {
        if (!placed || waterPos == null) return;

        recycleTicks++;
        // 放置后0.15~1秒内回收水
        if (recycleTicks > 3 && recycleTicks < 20) {
            if (mc.level.getBlockState(waterPos).getBlock() == Blocks.WATER) {
                lookAt(waterPos);
                PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
                resetState(); // 重置状态
            }
        }
        // 超时未回收则强制重置
        if (recycleTicks > 40) {
            resetState();
        }
    }

    /**
     * 寻找快捷栏中的水桶
     */
    private boolean findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() == Items.WATER_BUCKET) {
                waterSlot = i;
                return true;
            }
        }
        return false;
    }

    /**
     * 切换到水桶槽位
     */
    private void switchToWater() {
        originalSlot = mc.player.getInventory().selected;
        mc.player.getInventory().selected = waterSlot;
    }

    /**
     * 放置水
     */
    private void placeWater() {
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        // 只在方块顶部放置
        if (hit.getDirection() != Direction.UP) return;

        BlockPos pos = hit.getBlockPos().above();
        // 检查目标位置是否可以放置
        if (!mc.level.isEmptyBlock(pos)) return;

        lookAt(pos);
        PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
        waterPos = pos;
        placed = true;
        recycleTicks = 0;
    }

    /**
     * 重置状态（回收后或禁用时）
     */
    private void resetState() {
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = originalSlot;
        }
        placed = false;
        waterPos = null;
        originalSlot = -1;
        waterSlot = -1;
        recycleTicks = 0;
    }

    /**
     * 看向目标方块
     */
    private void lookAt(BlockPos pos) {
        if (mc.player == null) return;

        double dx = pos.getX() + 0.5 - mc.player.getX();
        double dz = pos.getZ() + 0.5 - mc.player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        // 计算旋转角度
        float yaw = (float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90;
        float pitch = (float) -(Math.atan2(0.5, dist) * 180 / Math.PI);
        // 设置玩家视角
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
    }

    @Override
    public void onDisable() {
        resetState(); // 禁用时重置所有状态
    }
}