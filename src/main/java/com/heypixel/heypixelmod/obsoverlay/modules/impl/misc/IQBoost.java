package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.Minecraft;

@ModuleInfo(
        name = "IQBoost",
        description = "Improve your IQ.",
        category = Category.MISC
)
public class IQBoost extends Module {
    private final Minecraft mc = Minecraft.getInstance(); // 添加上下文实例

    public FloatValue iq = ValueBuilder.create(this, "IQ")
            .setDefaultFloatValue(114514.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(114514.0F)
            .build()
            .getFloatValue();

    @EventTarget
    public void onTick(EventRunTicks event) {
        // 检查玩家和世界是否存在，且事件类型为PRE
        if (mc.player == null || mc.level == null || event.getType() != EventType.PRE) return;
        // 在模块后缀显示当前IQ值（保留一位小数）
        this.setSuffix(String.format("%.1f", iq.getCurrentValue()));
    }
}