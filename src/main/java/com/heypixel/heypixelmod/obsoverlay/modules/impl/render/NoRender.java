package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

@ModuleInfo(
   name = "NoBadRender",
   description = "Disables visual effects rendering (blindness, darkness, nausea)",
   category = Category.RENDER
)
public class NoRender extends Module {
   
   @EventTarget
   public void onPacket(EventPacket event) {
      if (event.getPacket() instanceof ClientboundUpdateMobEffectPacket packet) {

         if (isBadEffect(packet.getEffect())) {
            event.setCancelled(true);
         }
      }
   }
   
   @EventTarget
   public void onTick(EventRunTicks event) {
      if (event.getType() == EventType.POST && mc.player != null) {

         for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (isBadEffect(effect.getEffect())) {
               mc.player.removeEffect(effect.getEffect());
            }
         }
      }
   }
   
   private boolean isBadEffect(MobEffect effect) {

      return effect == MobEffects.BLINDNESS ||
             effect == MobEffects.CONFUSION ||
             effect == MobEffects.DARKNESS;
   }
   
   @Override
   public void onEnable() {
      if (mc.player != null) {

         for (MobEffectInstance effect : mc.player.getActiveEffects()) {
            if (isBadEffect(effect.getEffect())) {
               mc.player.removeEffect(effect.getEffect());
            }
         }
      }
   }
}
