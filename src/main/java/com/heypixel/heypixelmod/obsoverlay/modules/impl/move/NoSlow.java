package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSlowdown;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

@ModuleInfo(
        name = "NoSlow",
        description = "No slowdown while using items (Grim bypass)",
        category = Category.MOVEMENT
)
public class NoSlow extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    // 核心模式配置
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Old Grim", "Grim Tick", "Grim NoSlow")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();

    // Old Grim 模式配置
    private final ModeValue oldGrimSubMode = ValueBuilder.create(this, "OldGrim Mode")
            .setModes("Post", "Vanilla")
            .setDefaultModeIndex(0)
            .setVisibility(() -> mode.isCurrentMode("Old Grim"))
            .build()
            .getModeValue();

    private final FloatValue forwardMultiplier = ValueBuilder.create(this, "Forward Multiplier")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> mode.isCurrentMode("Old Grim"))
            .build()
            .getFloatValue();

    private final FloatValue strafeMultiplier = ValueBuilder.create(this, "Strafe Multiplier")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> mode.isCurrentMode("Old Grim"))
            .build()
            .getFloatValue();

    private final FloatValue foodMultiplier = ValueBuilder.create(this, "Food Multiplier")
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.2F)
            .setMaxFloatValue(1.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> mode.isCurrentMode("Old Grim"))
            .build()
            .getFloatValue();

    private final BooleanValue groundOnly = ValueBuilder.create(this, "Ground Only")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> mode.isCurrentMode("Old Grim"))
            .build()
            .getBooleanValue();

    // Grim Tick 模式配置
    private final FloatValue grimTickThreshold = ValueBuilder.create(this, "Tick Threshold")
            .setDefaultFloatValue(2.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> mode.isCurrentMode("Grim Tick"))
            .build()
            .getFloatValue();

    // Grim NoSlow 模式配置
    private final ModeValue slowPercent = ValueBuilder.create(this, "Slow Percent")
            .setModes("100%", "90%", "75%", "50%")
            .setDefaultModeIndex(0)
            .setVisibility(() -> mode.isCurrentMode("Grim NoSlow"))
            .build()
            .getModeValue();

    // 调试开关
    private final BooleanValue debug = ValueBuilder.create(this, "Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    // 状态变量
    private int ticksUsing = 0;
    private boolean shouldCancel = false;
    private boolean wasUsing = false;

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
    }

    private void resetState() {
        ticksUsing = 0;
        shouldCancel = false;
        wasUsing = false;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.POST || mc.player == null) return;

        LocalPlayer player = mc.player;
        boolean isUsingItem = player.isUsingItem() && isConsumable(player.getUseItem());

        setSuffix(mode.getCurrentMode());

        if (!isUsingItem) {
            resetState();
            return;
        }

        wasUsing = true;

        switch (mode.getCurrentMode()) {
            case "Old Grim" -> handleOldGrimTick(player);
            case "Grim Tick" -> handleGrimTickTick(player);
            case "Grim NoSlow" -> handleGrimNoSlowTick(player);
        }
    }

    @EventTarget
    public void onSlowdown(EventSlowdown event) {
        if (mc.player == null || !isConsumable(mc.player.getUseItem())) return;

        switch (mode.getCurrentMode()) {
            case "Old Grim" -> handleOldGrimSlowdown(event);
            case "Grim Tick" -> handleGrimTickSlowdown(event);
            case "Grim NoSlow" -> handleGrimNoSlowSlowdown(event);
        }
    }

    // Old Grim 模式逻辑
    private void handleOldGrimTick(LocalPlayer player) {
        if (!mc.options.keyUse.isDown() || oldGrimSubMode.isCurrentMode("Vanilla")) return;

        // 适配正确的数据包构造方式
        if (oldGrimSubMode.isCurrentMode("Post")) {
            // 发送释放物品包
            mc.getConnection().send(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM,
                    BlockPos.ZERO,
                    Direction.DOWN
            ));

            // 使用序列ID发送物品使用包（适配1.19+构造函数）
            PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id));
            debug("OldGrim Post: 发送RELEASE + USE数据包");
        }
    }

    private void handleOldGrimSlowdown(EventSlowdown event) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (groundOnly.getCurrentValue() && !player.onGround()) {
            debug("OldGrim: 未在地面，不取消减速");
            return;
        }

        event.setSlowdown(false);
        fixMovementInput(foodMultiplier.getCurrentValue());
        debug("OldGrim: 已取消减速");
    }

    // Grim Tick 模式逻辑
    private void handleGrimTickTick(LocalPlayer player) {
        ticksUsing++;
        shouldCancel = ticksUsing >= grimTickThreshold.getCurrentValue();
        if (shouldCancel) {
            ticksUsing = 0;
        }
        debug("GrimTick: 计数=" + ticksUsing + "，是否取消=" + shouldCancel);
    }

    private void handleGrimTickSlowdown(EventSlowdown event) {
        if (!shouldCancel) return;

        event.setSlowdown(false);
        fixMovementInput(1.0F);
        debug("GrimTick: 已取消减速");
    }

    // Grim NoSlow 模式逻辑
    private void handleGrimNoSlowTick(LocalPlayer player) {
        ticksUsing = 0;
    }

    private void handleGrimNoSlowSlowdown(EventSlowdown event) {
        float percent = switch (slowPercent.getCurrentMode()) {
            case "90%" -> 0.9F;
            case "75%" -> 0.75F;
            case "50%" -> 0.5F;
            default -> 1.0F;
        };

        event.setSlowdown(false);
        fixMovementInput(percent);
        debug("GrimNoSlow: 减速倍率=" + percent);
    }

    // 工具方法
    private void fixMovementInput(float multiplier) {
        LocalPlayer player = mc.player;
        if (player == null || player.input == null) return;

        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;

        boolean isSlowed = Math.abs(forward) < 0.3F || Math.abs(strafe) < 0.3F;
        if (!isSlowed) return;

        float fMul = forwardMultiplier.getCurrentValue() * multiplier;
        float sMul = strafeMultiplier.getCurrentValue() * multiplier;

        if (Math.abs(forward) > 0.01F) {
            player.input.forwardImpulse = Mth.clamp(forward * fMul / 0.2F, -1F, 1F);
        }
        if (Math.abs(strafe) > 0.01F) {
            player.input.leftImpulse = Mth.clamp(strafe * sMul / 0.2F, -1F, 1F);
        }
    }

    /**
     * 判断物品是否为会导致减速的消耗品/工具（适配全版本）
     */
    private boolean isConsumable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        // 1.18及以下用ItemFood，1.19+用FoodItem，这里统一用物品的可食用性判断
        return item.isEdible()
                || item instanceof PotionItem
                || item instanceof MilkBucketItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item == Items.SHIELD;
    }

    private void debug(String message) {
        if (debug.getCurrentValue()) {
            System.out.println("[NoSlow] " + message);
        }
    }
}