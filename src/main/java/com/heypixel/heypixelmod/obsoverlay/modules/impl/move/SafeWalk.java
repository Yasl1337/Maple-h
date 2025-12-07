package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.HasValue;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(
   name = "SafeWalk",
   description = "Prevents you from falling off blocks",
   category = Category.MOVEMENT
)
public class SafeWalk extends Module {
   private final BooleanValue shift = ValueBuilder.create(this, "Shift").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue slow = ValueBuilder.create(this, "Slow").setDefaultBooleanValue(false).build().getBooleanValue();
   private final FloatValue slowSpeed = ValueBuilder.create(this, "Slow Speed")
           .setDefaultFloatValue(0.2F)
           .setMinFloatValue(0.1F)
           .setMaxFloatValue(1.0F)
           .setFloatStep(0.01F)
           .setVisibility(() -> slow.getCurrentValue())
           .build()
           .getFloatValue();
   
   private final FloatValue sensitivity = ValueBuilder.create(this, "Sensitivity")
           .setDefaultFloatValue(0.3F)
           .setMinFloatValue(0.05F)
           .setMaxFloatValue(0.5F)
           .setFloatStep(0.05F)
           .build()
           .getFloatValue();
   
   private boolean savedShiftState = false;

   public static boolean isOnBlockEdge(Player player, Level level, float sensitivity) {
      return !level
         .getCollisions(player, player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
         .iterator()
         .hasNext();
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE && mc.player != null && mc.level != null) {
         boolean shouldSafeWalk = mc.player.onGround() && isOnBlockEdge(mc.player, mc.level, sensitivity.getCurrentValue());
         
         if (shift.getCurrentValue()) {
            mc.options.keyShift.setDown(shouldSafeWalk || savedShiftState);
         }
         
         if (slow.getCurrentValue() && shouldSafeWalk) {
            mc.player.setSprinting(false);
            mc.player.zza *= slowSpeed.getCurrentValue();
         }
      }
   }

   @Override
   public void onEnable() {
      if (mc.player != null) {
         savedShiftState = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
      }
   }

   @Override
   public void onDisable() {
      if (mc.player != null) {
         boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
         mc.options.keyShift.setDown(isHoldingShift);
      }
   }
}