package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;

/**
 * Eagle2: 在方块边缘自动潜行（辅助桥接/防跌落）
 */
@ModuleInfo(
        name = "Eagle",
        description = "Legit trick to build faster. Auto-sneak near edges.",
        category = Category.MOVEMENT
)
public class Eagle extends Module {
    private final Minecraft mc = Minecraft.getInstance(); // 补充Minecraft实例

    private final BooleanValue backwards = ValueBuilder.create(this, "OnlyBackwards")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private final BooleanValue onlyWithBlocks = ValueBuilder.create(this, "OnlyWithBlocks")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private float forward = 0;


    public static boolean isOnBlockEdge(float sensitivity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;

        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onMoveInput(EventMoveInput event) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        if (!player.onGround()) {
            return;
        }

        if (event.getForward() != 0) this.forward = event.getForward();


        if (backwards.getCurrentValue() && forward > 0) {
            return;
        }

        if (onlyWithBlocks.getCurrentValue()) {
            if (player.getMainHandItem().isEmpty() ||
                    !(player.getMainHandItem().getItem() instanceof BlockItem)) {
                return;
            }
        }


        boolean closeToEdge = isOnBlockEdge(0.3F);


        if (!player.getAbilities().flying && closeToEdge) {
            event.setSneak(true);
        }
    }

    @Override
    public void onEnable() {
        forward = 0; // 重置移动状态
    }

    @Override
    public void onDisable() {
        forward = 0; // 重置移动状态
    }
}