package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventUpdateFoV;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;

@ModuleInfo(
        name = "NoFOV",
        description = "Changes FOV value",
        category = Category.RENDER
)
public class NoFOV extends Module {
    private final ModeValue mode = ValueBuilder.create(this, "Mode")
            .setDefaultModeIndex(0)
            .setModes("Constant", "Custom")
            .build()
            .getModeValue();

    private final FloatValue constantFov = ValueBuilder.create(this, "FOV")
            .setVisibility(() -> mode.isCurrentMode("Constant"))
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private final FloatValue baseFov = ValueBuilder.create(this, "BaseFOV")
            .setVisibility(() -> mode.isCurrentMode("Custom"))
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private final FloatValue limitMin = ValueBuilder.create(this, "Limit Min")
            .setVisibility(() -> mode.isCurrentMode("Custom"))
            .setDefaultFloatValue(0.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private final FloatValue limitMax = ValueBuilder.create(this, "Limit Max")
            .setVisibility(() -> mode.isCurrentMode("Custom"))
            .setDefaultFloatValue(1.5F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    private final FloatValue multiplier = ValueBuilder.create(this, "Multiplier")
            .setVisibility(() -> mode.isCurrentMode("Custom"))
            .setDefaultFloatValue(1.0F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.5F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();

    @EventTarget
    public void onUpdateFoV(EventUpdateFoV event) {
        event.setFov(calculateFov(event.getFov()));
    }

    private float calculateFov(float originalFov) {
        if (mode.isCurrentMode("Constant")) {
            return constantFov.getCurrentValue();
        } else if (mode.isCurrentMode("Custom")) {
            float newFov = (originalFov - 1) * multiplier.getCurrentValue() + baseFov.getCurrentValue();
            return Math.max(limitMin.getCurrentValue(), Math.min(newFov, limitMax.getCurrentValue()));
        }
        return originalFov;
    }
}