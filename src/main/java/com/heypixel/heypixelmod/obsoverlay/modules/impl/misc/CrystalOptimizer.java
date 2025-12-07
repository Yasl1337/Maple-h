package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

import java.lang.reflect.Field;

@ModuleInfo(
        name = "CrystalOptimizer",
        description = "Does not wait for server-side confirmation when breaking crystals",
        category = Category.MISC
)
public class CrystalOptimizer extends Module {

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() != EventType.PRE) {
            return;
        }
        

        Packet<?> packet = e.getPacket();
        if (packet instanceof ServerboundInteractPacket interactPacket) {
            try {
                Field actionField = ServerboundInteractPacket.class.getDeclaredField("action");
                actionField.setAccessible(true);
                Object action = actionField.get(interactPacket);

                Field entityIdField = ServerboundInteractPacket.class.getDeclaredField("entityId");
                entityIdField.setAccessible(true);
                int entityId = entityIdField.getInt(interactPacket);

                Object actionType = action.getClass().getMethod("getType").invoke(action);
                if (actionType.toString().equals("ATTACK")) {
                    Entity entity = mc.level.getEntity(entityId);

                    if (entity instanceof EndCrystal && mc.player.getEffect(MobEffects.WEAKNESS) == null) {
                        entity.kill();
                        entity.setRemoved(Entity.RemovalReason.KILLED);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}