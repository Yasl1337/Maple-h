package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.combat.Aura;
import com.heypixel.heypixelmod.obsoverlay.utils.ProjectionUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.heypixel.heypixelmod.obsoverlay.utils.Vector2f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleInfo(
        name = "AttackEffect",
        description = "Visual effects when attacking entities",
        category = Category.RENDER
)
public class AttackEffect extends Module {
    
    private final Random random = new Random();
    private final List<AttackParticle> particles = new ArrayList<>();
    
    ModeValue mode = ValueBuilder.create(this, "Mode")
            .setModes("Circle", "Triangle", "Both")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();
    
    BooleanValue rainbow = ValueBuilder.create(this, "Rainbow")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    
    FloatValue circleSize = ValueBuilder.create(this, "Circle Size")
            .setDefaultFloatValue(0.5f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(2.0f)
            .build()
            .getFloatValue();
    
    FloatValue triangleSize = ValueBuilder.create(this, "Triangle Size")
            .setDefaultFloatValue(0.3f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(1.5f)
            .build()
            .getFloatValue();
    
    FloatValue duration = ValueBuilder.create(this, "Duration")
            .setDefaultFloatValue(1.0f)
            .setFloatStep(0.1f)
            .setMinFloatValue(0.1f)
            .setMaxFloatValue(5.0f)
            .build()
            .getFloatValue();
    
    FloatValue particleCount = ValueBuilder.create(this, "Particle Count")
            .setDefaultFloatValue(5.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(20.0f)
            .build()
            .getFloatValue();
    
    @EventTarget
    public void onRender3D(EventRender event) {
        if (Aura.target != null && Aura.target instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) Aura.target;
            Vec3 targetPos = target.getPosition(event.getRenderPartialTicks());
            

            float targetHeight = target.getBbHeight();
            

            Vec3 effectPos = new Vec3(targetPos.x, targetPos.y, targetPos.z);
            

            String currentMode = mode.getCurrentMode();
            
            if (currentMode.equals("Circle") || currentMode.equals("Both")) {
                renderCircleEffect(effectPos, targetHeight);
            }
            
            if (currentMode.equals("Triangle") || currentMode.equals("Both")) {
                renderTriangleEffect(effectPos, targetHeight);
            }
        }
        

        updateParticles();
        renderParticles(event.getPMatrixStack());
    }
    
    private void renderCircleEffect(Vec3 pos, float targetHeight) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        

        RenderUtils.applyRegionalRenderOffset(poseStack);
        
        Matrix4f matrix = poseStack.last().pose();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        
        int color = getEffectColor();
        

        float circleRadius = circleSize.getCurrentValue();
        int segments = 36;
        
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        

        Vector2f screenPos = ProjectionUtils.project(pos.x, pos.y, pos.z, 1.0f);
        if (screenPos != null && screenPos.x != Float.MAX_VALUE) {
            RenderUtils.drawCircle(new PoseStack(), screenPos.x, screenPos.y, circleRadius * 10, color, segments);
        }
        
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }
    
    private void renderTriangleEffect(Vec3 pos, float targetHeight) {
        PoseStack poseStack = new PoseStack();
        poseStack.pushPose();
        
        RenderUtils.applyRegionalRenderOffset(poseStack);
        
        Matrix4f matrix = poseStack.last().pose();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        
        int color = getEffectColor();
        
        float size = triangleSize.getCurrentValue();
        

        Vec3 topPos = new Vec3(pos.x, pos.y + targetHeight * 0.8, pos.z);
        

        Vector2f screenPos = ProjectionUtils.project(topPos.x, topPos.y, topPos.z, 1.0f);
        if (screenPos != null && screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE) {

            if (screenPos.x >= 0 && screenPos.x <= Minecraft.getInstance().getWindow().getGuiScaledWidth() && 
                screenPos.y >= 0 && screenPos.y <= Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
                
                float x = screenPos.x;
                float y = screenPos.y;
                float triangleSizePx = size * 15;
                

                float x1 = x;
                float y1 = y - triangleSizePx;
                float x2 = x - triangleSizePx;
                float y2 = y + triangleSizePx;
                float x3 = x + triangleSizePx;
                float y3 = y + triangleSizePx;
                
                RenderUtils.drawTriangle(x1, y1, x2, y2, x3, y3, color);
            }
        }
        
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.disableBlend();
        
        poseStack.popPose();
    }
    
    private int getEffectColor() {
        if (rainbow.getCurrentValue()) {
            long time = System.currentTimeMillis();
            float hue = (time % 2000) / 2000.0f;
            return Color.HSBtoRGB(hue, 0.8f, 1.0f);
        } else {
            return 0xFFFF0000;
        }
    }
    
    private void updateParticles() {
        particles.removeIf(particle -> System.currentTimeMillis() - particle.createTime > duration.getCurrentValue() * 1000);
    }
    
    private void renderParticles(PoseStack poseStack) {
        for (AttackParticle particle : particles) {
            particle.render(poseStack);
        }
    }
    
    @EventTarget
    public void onAttack(com.heypixel.heypixelmod.obsoverlay.events.impl.EventRayTrace event) {
        if (event.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) event.getEntity();
            Vec3 targetPos = target.position();
            

            int count = (int) particleCount.getCurrentValue();
            for (int i = 0; i < count; i++) {
                double offsetX = (random.nextDouble() - 0.5) * target.getBbWidth();
                double offsetY = random.nextDouble() * target.getBbHeight();
                double offsetZ = (random.nextDouble() - 0.5) * target.getBbWidth();
                
                Vec3 particlePos = new Vec3(
                        targetPos.x + offsetX,
                        targetPos.y + offsetY,
                        targetPos.z + offsetZ
                );
                
                particles.add(new AttackParticle(particlePos, getEffectColor()));
            }
        }
    }
    
    @Override
    public void onDisable() {
        particles.clear();
        super.onDisable();
    }
    
    private static class AttackParticle {
        private final Vec3 position;
        private final int color;
        private final long createTime;
        
        public AttackParticle(Vec3 position, int color) {
            this.position = position;
            this.color = color;
            this.createTime = System.currentTimeMillis();
        }
        
        public void render(PoseStack poseStack) {
            float alpha = 1.0f - (float)(System.currentTimeMillis() - createTime) / 1000.0f;
            if (alpha <= 0) return;
            
            poseStack.pushPose();
            RenderUtils.applyRegionalRenderOffset(poseStack);
            
            Matrix4f matrix = poseStack.last().pose();
            
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.depthMask(false);
            

            int particleColor = (int)(alpha * 255) << 24 | (color & 0xFFFFFF);
            
            Vector2f screenPos = ProjectionUtils.project(position.x, position.y, position.z, 1.0f);
            if (screenPos != null && 
                screenPos.x != Float.MAX_VALUE && 
                screenPos.y != Float.MAX_VALUE && 
                screenPos.x >= 0 && 
                screenPos.x <= Minecraft.getInstance().getWindow().getGuiScaledWidth() && 
                screenPos.y >= 0 && 
                screenPos.y <= Minecraft.getInstance().getWindow().getGuiScaledHeight()) {
                
                float size = 2.0f;
                float x = screenPos.x - size / 2;
                float y = screenPos.y - size / 2;
                

                RenderUtils.drawCircle(new PoseStack(), x + size / 2, y + size / 2, size, particleColor, 8);
            }
            
            RenderSystem.depthMask(true);
            RenderSystem.setShaderTexture(0, 0);
            RenderSystem.disableBlend();
            
            poseStack.popPose();
        }
    }
}