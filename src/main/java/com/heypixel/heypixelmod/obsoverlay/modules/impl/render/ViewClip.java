package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;

@ModuleInfo(
        name = "ViewClip",
        description = "Allows you to see through blocks",
        category = Category.RENDER
)
public class ViewClip extends Module {
    // 配置项：使用 private final 修饰符，与其他模块保持一致
    private final FloatValue scale = ValueBuilder.create(this, "Scale")
            .setMinFloatValue(0.5F)
            .setMaxFloatValue(2.0F)
            .setDefaultFloatValue(1.0F)
            .setFloatStep(0.01F)
            .build()
            .getFloatValue();

    private final BooleanValue animation = ValueBuilder.create(this, "Animation")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private final FloatValue animationSpeed = ValueBuilder.create(this, "Animation Speed")
            .setMinFloatValue(0.01F)
            .setMaxFloatValue(0.5F)
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.01F)
            .setVisibility(() -> this.animation.getCurrentValue())
            .build()
            .getFloatValue();

    // 动画计时器和相机状态
    private final SmoothAnimationTimer personViewAnimation = new SmoothAnimationTimer(100.0F);
    private CameraType lastPersonView;
    private final Minecraft mc = Minecraft.getInstance(); // 统一 Minecraft 实例引用

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.lastPersonView != mc.options.getCameraType()) {
            this.lastPersonView = mc.options.getCameraType();
            // 第一人称或第三人称背部视角时重置动画
            if (this.lastPersonView == CameraType.FIRST_PERSON || this.lastPersonView == CameraType.THIRD_PERSON_BACK) {
                this.personViewAnimation.value = 0.0F;
            }
        }

        // 更新动画速度和状态
        this.personViewAnimation.speed = this.animationSpeed.getCurrentValue();
        this.personViewAnimation.update(true);
    }
}