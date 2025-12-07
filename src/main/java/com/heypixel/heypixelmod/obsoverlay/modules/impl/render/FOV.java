package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;

@ModuleInfo(
        name = "FOV",
        description = "Change field of view.",
        category = Category.RENDER
)
public class FOV extends Module {
    // 配置项：使用正确的包路径和构建方式
    private final FloatValue fov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(120.0F)
            .setMaxFloatValue(180.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(1.0F)
            .build()
            .getFloatValue();

    // 事件处理：修改视场角
    @EventTarget
    public void onFoV(EventUpdateFoV e) {
        // 修正：直接使用配置值（原代码的0.01F缩放会导致FOV过小）
        e.setFov(fov.getCurrentValue());
    }
}