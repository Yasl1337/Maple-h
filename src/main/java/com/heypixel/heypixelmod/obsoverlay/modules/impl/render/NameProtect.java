package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderTabOverlay;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.StringValue;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;

@ModuleInfo(
        name = "NameProtect",
        description = "Protect your name",
        category = Category.RENDER
)
public class NameProtect extends Module {
   public static NameProtect instance;
   public NameProtect() {
      instance = this;
   }
   public static String getName(String string) {
      if (!instance.isEnabled() || mc.player == null) {
         return string;
      } else {
         return string.contains(mc.player.getName().getString()) ? StringUtils.replace(string, mc.player.getName().getString(), "§d曾浩宇§7") : string;
      }
   }
   @EventTarget
   public void onRenderTab(EventRenderTabOverlay e) {
      e.setComponent(Component.literal(getName(e.getComponent().getString())));
   }
}