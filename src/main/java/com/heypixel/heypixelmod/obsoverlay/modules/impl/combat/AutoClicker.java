package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.mixin.O.accessors.MinecraftAccessor;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.HitResult.Type;

@ModuleInfo(
        name = "AutoClicker",
        description = "Automatically clicks for you",
        category = Category.COMBAT
)
public class AutoClicker extends Module {
   private final FloatValue cps = ValueBuilder.create(this, "CPS")
           .setDefaultFloatValue(10.0F)
           .setFloatStep(1.0F)
           .setMinFloatValue(5.0F)
           .setMaxFloatValue(20.0F)
           .build()
           .getFloatValue();
   private final BooleanValue leftClick = ValueBuilder.create(this, "Left").setDefaultBooleanValue(true).build().getBooleanValue();
   private final BooleanValue itemCheck = ValueBuilder.create(this, "Weapon Only").setDefaultBooleanValue(false)
           .setVisibility(this.leftClick::getCurrentValue)
           .build()
           .getBooleanValue();
   private final BooleanValue rightClick = ValueBuilder.create(this, "Right").setDefaultBooleanValue(false).build().getBooleanValue();
   private final BooleanValue blocksOnly = ValueBuilder.create(this, "Blocks Only")
           .setDefaultBooleanValue(false)
           .setVisibility(this.rightClick::getCurrentValue)
           .build()
           .getBooleanValue();
   private final BooleanValue expOnly = ValueBuilder.create(this, "EXP Only")
           .setDefaultBooleanValue(false)
           .setVisibility(this.rightClick::getCurrentValue)
           .build()
           .getBooleanValue();
   private float counter = 0.0F;

   private boolean isHoldingBlock() {
      return mc.player.getMainHandItem().getItem() instanceof BlockItem;
   }

   @EventTarget
   public void onMotion(EventMotion e) {
      if (e.getType() == EventType.PRE) {
         
         if (mc.player == null || mc.hitResult == null) {
            return;
         }

         MinecraftAccessor accessor = (MinecraftAccessor)mc;
         Item heldItem = mc.player.getMainHandItem().getItem();

         boolean isWeapon = heldItem instanceof SwordItem || heldItem instanceof AxeItem;
         boolean isExperienceBottle = heldItem instanceof ExperienceBottleItem;

         boolean shouldLeftClick = this.leftClick.getCurrentValue()
                 && mc.options.keyAttack.isDown()
                 && mc.hitResult.getType() != Type.BLOCK
                 && (!this.itemCheck.getCurrentValue() || isWeapon);

         boolean shouldRightClick = false;
         if (this.rightClick.getCurrentValue() && mc.options.keyUse.isDown()) {
            boolean blocksOrExpCondition = (this.blocksOnly.getCurrentValue() && this.isHoldingBlock())
                    || (this.expOnly.getCurrentValue() && isExperienceBottle)
                    || (!this.blocksOnly.getCurrentValue() && !this.expOnly.getCurrentValue());

            if (blocksOrExpCondition) {
               shouldRightClick = true;
            }
         }


         if (shouldLeftClick || shouldRightClick) {
            this.counter += this.cps.getCurrentValue() / 20.0F;

            if (this.counter >= 1.0F) {
               if (shouldLeftClick) {
                  accessor.setMissTime(0);
                  KeyMapping.click(mc.options.keyAttack.getKey());
               }
               if (shouldRightClick) {
                  KeyMapping.click(mc.options.keyUse.getKey());
               }
               this.counter--;
            }
         } else {
            this.counter = 0.0F;
         }
      }
   }
}
