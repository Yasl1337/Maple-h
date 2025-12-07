package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.google.common.collect.Lists;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.StringUtil;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@ModuleInfo(
        name = "EffectDisplay",
        description = "Displays potion effects on the HUD",
        category = Category.RENDER
)
public class EffectDisplay extends Module {
   private List<Runnable> list;
   private final Map<MobEffect, EffectDisplay.MobEffectInfo> infos = new ConcurrentHashMap<>();
   private final Color headerColor = new Color(150, 45, 45, 255);
   private final Color bodyColor = new Color(0, 0, 0, 50);
   private final List<Vector4f> blurMatrices = new ArrayList<>();
   private static final Pattern CJK_CHAR_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
   
   private float totalMinX = Float.MAX_VALUE;
   private float totalMinY = Float.MAX_VALUE;
   private float totalMaxX = Float.MIN_VALUE;
   private float totalMaxY = Float.MIN_VALUE;
   
   private final ModeValue displayMode = ValueBuilder.create(this, "Display Mode")
           .setModes("Normal", "Capsule")
           .setDefaultModeIndex(0)
           .build()
           .getModeValue();
   
   private final BooleanValue renderBackground = ValueBuilder.create(this, "RenderBackGround")
           .setDefaultBooleanValue(true)
           .setVisibility(() -> this.displayMode.getCurrentMode().equals("Capsule"))
           .build()
           .getBooleanValue();

   private String getDisplayName(MobEffect effect, MobEffectInfo info) {
      String displayName = I18n.get(effect.getDescriptionId());
      if (info.amplifier > 0) {
         displayName += " " + I18n.get("enchantment.level." + (info.amplifier + 1));
      }
      return displayName;
   }

   @EventTarget(4)
   public void renderIcons(EventRender2D e) {
      this.list.forEach(Runnable::run);
   }

   @EventTarget
   public void onShader(EventShader e) {
      for (int i = 0; i < this.blurMatrices.size(); i++) {
         Vector4f matrix = this.blurMatrices.get(i);

         float cornerRadius = (i == 0 && displayMode.getCurrentMode().equals("Capsule")) ? 8.0F : 5.0F;
         RenderUtils.drawRoundedRect(e.getStack(), matrix.x(), matrix.y(), matrix.z(), matrix.w(), cornerRadius, 1073741824);
      }
   }

   private float getCorrectedWidth(CustomTextRenderer renderer, String text, double scale) {
      float baseWidth = renderer.getWidth(text, scale);
      if (CJK_CHAR_PATTERN.matcher(text).find()) {
         return baseWidth * 1.6F;
      }
      return baseWidth;
   }

   @EventTarget
   public void onRender(EventRender2D e) {
      for (MobEffectInstance effect : mc.player.getActiveEffects()) {
         EffectDisplay.MobEffectInfo info;
         if (this.infos.containsKey(effect.getEffect())) {
            info = this.infos.get(effect.getEffect());
         } else {
            info = new EffectDisplay.MobEffectInfo();
            this.infos.put(effect.getEffect(), info);
         }

         info.maxDuration = Math.max(info.maxDuration, effect.getDuration());
         info.duration = effect.getDuration();
         info.amplifier = effect.getAmplifier();
         info.shouldDisappear = false;
      }

      int startY = mc.getWindow().getGuiScaledHeight() / 2 - this.infos.size() * 16;
      this.list = Lists.newArrayListWithExpectedSize(this.infos.size());
      this.blurMatrices.clear();
      

      if (displayMode.getCurrentMode().equals("Capsule") && !this.infos.isEmpty()) {
         this.totalMinX = Float.MAX_VALUE;
         this.totalMinY = Float.MAX_VALUE;
         this.totalMaxX = Float.MIN_VALUE;
         this.totalMaxY = Float.MIN_VALUE;
      }

      for (Entry<MobEffect, EffectDisplay.MobEffectInfo> entry : this.infos.entrySet()) {
         e.getStack().pushPose();
         EffectDisplay.MobEffectInfo effectInfo = entry.getValue();
         String text = this.getDisplayName(entry.getKey(), effectInfo);
         if (effectInfo.yTimer.value == -1.0F) {
            effectInfo.yTimer.value = (float)startY;
         }

         CustomTextRenderer harmony = Fonts.harmony;

         float textWidth = getCorrectedWidth(harmony, text, 0.3);
         effectInfo.width = 25.0F + textWidth + 20.0F;

         float x = effectInfo.xTimer.value;
         float y = effectInfo.yTimer.value;
         effectInfo.shouldDisappear = !mc.player.hasEffect(entry.getKey());
         if (effectInfo.shouldDisappear) {
            effectInfo.xTimer.target = -effectInfo.width - 20.0F;
            if (x <= -effectInfo.width - 20.0F) {
               this.infos.remove(entry.getKey());
            }
         } else {
            effectInfo.durationTimer.target = (float)effectInfo.duration / (float)effectInfo.maxDuration * effectInfo.width;
            if (effectInfo.durationTimer.value <= 0.0F) {
               effectInfo.durationTimer.value = effectInfo.durationTimer.target;
            }

            effectInfo.xTimer.target = 10.0F;
            effectInfo.yTimer.target = (float)startY;
            effectInfo.yTimer.update(true);
         }

         effectInfo.durationTimer.update(true);
         effectInfo.xTimer.update(true);
         
         if (displayMode.getCurrentMode().equals("Capsule")) {
            renderCapsuleMode(e, entry, effectInfo, x, y, harmony, text);
         } else {
            renderNormalMode(e, entry, effectInfo, x, y, harmony, text);
         }
         
         startY += 34;
         e.getStack().popPose();
      }
      

      if (displayMode.getCurrentMode().equals("Capsule") && !this.infos.isEmpty() && 
          this.totalMinX != Float.MAX_VALUE && this.totalMaxX != Float.MIN_VALUE && 
          this.renderBackground.getCurrentValue()) {
         float padding = 8.0F;
         float backgroundX = this.totalMinX - padding;
         float backgroundY = this.totalMinY - padding;
         float backgroundWidth = (this.totalMaxX - this.totalMinX) + (padding * 2);
         float backgroundHeight = (this.totalMaxY - this.totalMinY) + (padding * 2);
         

         this.blurMatrices.add(0, new Vector4f(backgroundX, backgroundY, backgroundWidth, backgroundHeight));
      }
   }

   private void renderNormalMode(EventRender2D e, Entry<MobEffect, EffectDisplay.MobEffectInfo> entry, EffectDisplay.MobEffectInfo effectInfo, float x, float y, CustomTextRenderer harmony, String text) {
      StencilUtils.write(false);
      this.blurMatrices.add(new Vector4f(x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F));
      RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
      StencilUtils.erase(true);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.width, 30.0F, this.bodyColor.getRGB());
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.durationTimer.value, 30.0F, this.bodyColor.getRGB());
      RenderUtils.drawRoundedRect(e.getStack(), x + effectInfo.width - 10.0F, y + 7.0F, 5.0F, 18.0F, 2.0F, this.headerColor.getRGB());
      harmony.render(e.getStack(), text, (double)(x + 27.0F), (double)(y + 7.0F), this.headerColor, true, 0.3);
      String duration = StringUtil.formatTickDuration(effectInfo.duration);
      harmony.render(e.getStack(), duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);
      MobEffectTextureManager mobeffecttexturemanager = mc.getMobEffectTextures();
      TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(entry.getKey());
      this.list.add(() -> {
         RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         e.getGuiGraphics().blit((int)(x + 6.0F), (int)(y + 8.0F), 1, 18, 18, textureatlassprite);
      });
      StencilUtils.dispose();
   }

   private void renderCapsuleMode(EventRender2D e, Entry<MobEffect, EffectDisplay.MobEffectInfo> entry, EffectDisplay.MobEffectInfo effectInfo, float x, float y, CustomTextRenderer harmony, String text) {

      this.totalMinX = Math.min(this.totalMinX, x);
      this.totalMinY = Math.min(this.totalMinY, y);
      this.totalMaxX = Math.max(this.totalMaxX, x + effectInfo.width);
      this.totalMaxY = Math.max(this.totalMaxY, y + 30.0F);
      
      StencilUtils.write(false);
      RenderUtils.drawRoundedRect(e.getStack(), x + 2.0F, y + 2.0F, effectInfo.width - 2.0F, 28.0F, 5.0F, -1);
      StencilUtils.erase(true);
      int effectColor = getEffectThemeColor(entry.getKey(), 95);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.width, 30.0F, effectColor);
      RenderUtils.fillBound(e.getStack(), x, y, effectInfo.durationTimer.value, 30.0F, effectColor);
      harmony.render(e.getStack(), text, (double)(x + 27.0F), (double)(y + 7.0F), Color.WHITE, true, 0.3);
      String duration = StringUtil.formatTickDuration(effectInfo.duration);
      harmony.render(e.getStack(), duration, (double)(x + 27.0F), (double)(y + 17.0F), Color.WHITE, true, 0.25);
      MobEffectTextureManager mobeffecttexturemanager = mc.getMobEffectTextures();
      TextureAtlasSprite textureatlassprite = mobeffecttexturemanager.get(entry.getKey());
      this.list.add(() -> {
         RenderSystem.setShaderTexture(0, textureatlassprite.atlasLocation());
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         e.getGuiGraphics().blit((int)(x + 6.0F), (int)(y + 8.0F), 1, 18, 18, textureatlassprite);
      });


      StencilUtils.dispose();
   }

   private int getEffectThemeColor(MobEffect effect, int alpha) {
      String descriptionId = effect.getDescriptionId();
      if (descriptionId.equals("effect.minecraft.strength")) return createColorWithAlpha(0x932423, alpha);
      if (descriptionId.equals("effect.minecraft.weakness")) return createColorWithAlpha(0x484D48, alpha);
      if (descriptionId.equals("effect.minecraft.speed")) return createColorWithAlpha(0x7CAFC6, alpha);
      if (descriptionId.equals("effect.minecraft.slowness")) return createColorWithAlpha(0x5A6C81, alpha);
      if (descriptionId.equals("effect.minecraft.jump_boost")) return createColorWithAlpha(0x22FF4C, alpha);
      if (descriptionId.equals("effect.minecraft.regeneration")) return createColorWithAlpha(0xCD5CAB, alpha);
      if (descriptionId.equals("effect.minecraft.poison")) return createColorWithAlpha(0x4E9331, alpha);
      if (descriptionId.equals("effect.minecraft.fire_resistance")) return createColorWithAlpha(0xE49A3A, alpha);
      if (descriptionId.equals("effect.minecraft.water_breathing")) return createColorWithAlpha(0x2E5299, alpha);
      if (descriptionId.equals("effect.minecraft.invisibility")) return createColorWithAlpha(0x7F8392, alpha);
      if (descriptionId.equals("effect.minecraft.night_vision")) return createColorWithAlpha(0x1F1FA1, alpha);
      if (descriptionId.equals("effect.minecraft.haste")) return createColorWithAlpha(0xD9C043, alpha);
      if (descriptionId.equals("effect.minecraft.mining_fatigue")) return createColorWithAlpha(0x4A4217, alpha);
      if (descriptionId.equals("effect.minecraft.resistance")) return createColorWithAlpha(0x99453A, alpha);
      if (descriptionId.equals("effect.minecraft.absorption")) return createColorWithAlpha(0x2552A5, alpha);
      if (descriptionId.equals("effect.minecraft.health_boost")) return createColorWithAlpha(0xF87D23, alpha);
      if (descriptionId.equals("effect.minecraft.saturation")) return createColorWithAlpha(0xF8AD48, alpha);
      if (descriptionId.equals("effect.minecraft.glowing")) return createColorWithAlpha(0x94A61B, alpha);
      if (descriptionId.equals("effect.minecraft.levitation")) return createColorWithAlpha(0xCE32ED, alpha);
      if (descriptionId.equals("effect.minecraft.luck")) return createColorWithAlpha(0x339900, alpha);
      if (descriptionId.equals("effect.minecraft.unluck")) return createColorWithAlpha(0xBC0000, alpha);
      if (descriptionId.equals("effect.minecraft.slow_falling")) return createColorWithAlpha(0xF7F8CE, alpha);
      if (descriptionId.equals("effect.minecraft.conduit_power")) return createColorWithAlpha(0x1BCAD8, alpha);
      if (descriptionId.equals("effect.minecraft.dolphins_grace")) return createColorWithAlpha(0x86B2CA, alpha);
      if (descriptionId.equals("effect.minecraft.bad_omen")) return createColorWithAlpha(0x0B6138, alpha);
      if (descriptionId.equals("effect.minecraft.hero_of_the_village")) return createColorWithAlpha(0xCDD724, alpha);
      if (descriptionId.equals("effect.minecraft.darkness")) return createColorWithAlpha(0x1E1E23, alpha);
      

      return createColorWithAlpha(0x000000, alpha);
   }
   

   private int createColorWithAlpha(int rgb, int alpha) {
      int r = (rgb >> 16) & 0xFF;
      int g = (rgb >> 8) & 0xFF;
      int b = rgb & 0xFF;
      return (alpha << 24) | (r << 16) | (g << 8) | b;
   }

   public static class MobEffectInfo {
      public SmoothAnimationTimer xTimer = new SmoothAnimationTimer(-60.0F, 0.2F);
      public SmoothAnimationTimer yTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public SmoothAnimationTimer durationTimer = new SmoothAnimationTimer(-1.0F, 0.2F);
      public int maxDuration = -1;
      public int duration = 0;
      public int amplifier = 0;
      public boolean shouldDisappear = false;
      public float width;
   }
}