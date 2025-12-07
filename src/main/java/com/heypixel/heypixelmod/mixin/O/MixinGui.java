package com.heypixel.heypixelmod.mixin.O;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRenderScoreboard;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventSetTitle;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.NoRender;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.Scoreboard;
import com.heypixel.heypixelmod.obsoverlay.ui.BetterHotBar;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Gui.class},
   priority = 100
)
public class MixinGui {
   @Shadow
   @Nullable
   protected Component title;
   @Shadow
   protected int titleTime;
   @Shadow
   protected int titleFadeInTime;
   @Shadow
   protected int titleStayTime;
   @Shadow
   protected int titleFadeOutTime;
   @Shadow
   @Nullable
   protected Component subtitle;

@Inject(
   method = {"displayScoreboardSidebar"},
   at = {@At("HEAD")}
)
public void hookScoreboardHead(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
   pPoseStack.pose().pushPose();
   Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
   if (module.isEnabled()) {
      module.clearScoreboardData();
      // 使用 Down 偏移
      float xOffset = module.getXOffset();
      float yOffset = module.getYOffset();
      pPoseStack.pose().translate(xOffset, yOffset, 0.0F);
   }
}

@Inject(
   method = {"displayScoreboardSidebar"},
   at = {@At("RETURN")}
)
public void hookScoreboardReturn(GuiGraphics pPoseStack, Objective pObjective, CallbackInfo ci) {
   pPoseStack.pose().popPose();
}

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"
      )
   )
   public int hookRenderScore(GuiGraphics instance, Font p_283343_, String p_281896_, int p_283569_, int p_283418_, int p_281560_, boolean p_282130_) {
      Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
      return module.isEnabled() && module.hideScore.getCurrentValue() ? 0 : instance.drawString(p_283343_, p_281896_, p_283569_, p_283418_, p_281560_);
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/PlayerTeam;formatNameForTeam(Lnet/minecraft/world/scores/Team;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"
      )
   )
   public MutableComponent hookScoreboardName(Team pPlayerTeam, Component pPlayerName) {
      MutableComponent mutableComponent = PlayerTeam.formatNameForTeam(pPlayerTeam, pPlayerName);
      EventRenderScoreboard event = new EventRenderScoreboard(mutableComponent);
      Naven.getInstance().getEventManager().call(event);
      return (MutableComponent)event.getComponent();
   }

   @Redirect(
      method = {"displayScoreboardSidebar"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/scores/Objective;getDisplayName()Lnet/minecraft/network/chat/Component;"
      )
   )
   public Component hookScoreboardTitle(Objective instance) {
      Component component = instance.getDisplayName();
      EventRenderScoreboard event = new EventRenderScoreboard(component);
      Naven.getInstance().getEventManager().call(event);
      return event.getComponent();
   }

   @Inject(
      method = {"setTitle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void hookTitle(Component pTitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.TITLE, pTitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.title = event.getTitle();
         this.titleTime = this.titleFadeInTime + this.titleStayTime + this.titleFadeOutTime;
         ci.cancel();
      }
   }

   @Inject(
      method = {"setSubtitle"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void hookSubtitle(Component pSubtitle, CallbackInfo ci) {
      EventSetTitle event = new EventSetTitle(EventType.SUBTITLE, pSubtitle);
      Naven.getInstance().getEventManager().call(event);
      if (!event.isCancelled()) {
         this.subtitle = event.getTitle();
         ci.cancel();
      }
   }

@Inject(
   method = {"renderEffects"},
   at = {@At("HEAD")},
   cancellable = true
)
public void hookRenderEffects(GuiGraphics pPoseStack, CallbackInfo ci) {
   NoRender noRender = (NoRender)Naven.getInstance().getModuleManager().getModule(NoRender.class);
   if (noRender.isEnabled()) {
      ci.cancel();
   }
}

// 拦截 fill 方法以捕获 scoreboard 背景的位置和大小
// 在 1.20.1 中，scoreboard 使用 fill 而不是 fillGradient
@Redirect(
   method = {"displayScoreboardSidebar"},
   at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"
   )
)
public void hookScoreboardFill(GuiGraphics instance, int pX1, int pY1, int pX2, int pY2, int pColor) {
   Scoreboard module = (Scoreboard)Naven.getInstance().getModuleManager().getModule(Scoreboard.class);
   if (module.isEnabled()) {
      // 累积 scoreboard 的边界信息，而不是覆盖
      module.addScoreboardBounds(pX1, pY1, pX2, pY2, pColor);
      
      // 在第一次 fill 后尝试渲染圆角背景（只会渲染一次）
      module.renderBackgroundIfNeeded(instance.pose());
      
      // 如果启用圆角，则跳过原版渲染
      if (module.shouldRenderRoundedBackground()) {
         return;
      }
   }
   // 继续正常渲染直角背景
   instance.fill(pX1, pY1, pX2, pY2, pColor);
}

// 拦截hotbar渲染，使用自定义的BetterHotBar渲染
@Inject(
   method = {"renderHotbar"},
   at = {@At("HEAD")},
   cancellable = true
)
public void hookRenderHotbar(float pPartialTick, GuiGraphics pGuiGraphics, CallbackInfo ci) {
   if (BetterHotBar.shouldRenderCustomHotbar()) {
      // 使用自定义hotbar渲染
      BetterHotBar.renderCustomHotbar(pGuiGraphics, pPartialTick);
      // 取消原版hotbar渲染
      ci.cancel();
   }
}
}
