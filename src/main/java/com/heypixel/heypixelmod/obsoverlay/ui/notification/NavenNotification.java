package com.heypixel.heypixelmod.obsoverlay.ui.notification;

import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;


public class NavenNotification extends Notification {
    
    public NavenNotification(NotificationLevel level, String message, long age) {
        super(level, message, age);
    }
    
    public NavenNotification(NotificationLevel level, String title, String description, long age) {
        super(level, title, description, age);
    }
    
    public NavenNotification(String message, boolean enabled) {
        super(message, enabled);
    }
    
    @Override
    public void renderShader(PoseStack stack, float x, float y) {
        RenderUtils.drawRoundedRect(stack, x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F, this.getLevel().getColor());
    }
    
    @Override
    public void render(PoseStack stack, float x, float y) {
        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(stack, x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, 5.0F, this.getLevel().getColor());
        StencilUtils.erase(true);
        RenderUtils.fillBound(stack, x + 2.0F, y + 4.0F, this.getWidth(), 20.0F, this.getLevel().getColor());
        Fonts.harmony.render(stack, this.getMessage(), (double)(x + 6.0F), (double)(y + 9.0F), Color.WHITE, true, 0.35);
        StencilUtils.dispose();
    }
    
    @Override
    public float getWidth() {
        float stringWidth = Fonts.harmony.getWidth(this.getMessage(), 0.35);
        return stringWidth + 12.0F;
    }
    
    @Override
    public float getHeight() {
        return 24.0F;
    }
}
