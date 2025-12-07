package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.ByteBuffer;

public class RenderUtils {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final AABB DEFAULT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

    public static int reAlpha(int color, float alpha) {
        int col = MathUtils.clamp((int)(alpha * 255.0F), 0, 255) << 24;
        col |= MathUtils.clamp(color >> 16 & 0xFF, 0, 255) << 16;
        col |= MathUtils.clamp(color >> 8 & 0xFF, 0, 255) << 8;
        return col | MathUtils.clamp(color & 0xFF, 0, 255);
    }

    public static void drawTracer(PoseStack poseStack, float x, float y, float size, float widthDiv, float heightDiv, int color) {
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glDisable(2929);
        GL11.glDepthMask(false);
        GL11.glEnable(2848);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = poseStack.last().pose();
        float a = (float)(color >> 24 & 0xFF) / 255.0F;
        float r = (float)(color >> 16 & 0xFF) / 255.0F;
        float g = (float)(color >> 8 & 0xFF) / 255.0F;
        float b = (float)(color & 0xFF) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x - size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y + size / heightDiv, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x + size / widthDiv, y + size, 0.0F).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(3042);
        GL11.glEnable(2929);
        GL11.glDepthMask(true);
        GL11.glDisable(2848);
    }

    public static void drawHealthRing(PoseStack poseStack, float centerX, float centerY,
                                      float radius, float thickness, float progress) {
        if (progress <= 0) return;

        Matrix4f matrix = poseStack.last().pose();
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 1.0f;

        float sweepAngle = progress * 360.0f;

        int segments = (int) (Math.min(360, Math.max(36, sweepAngle)));
        float angleStep = sweepAngle / segments;

        float startAngle = -90.0f;

        buffer.begin(Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            float angle = (float) Math.toRadians(startAngle + i * angleStep);

            float outerX = centerX + (float)Math.cos(angle) * radius;
            float outerY = centerY + (float)Math.sin(angle) * radius;
            buffer.vertex(matrix, outerX, outerY, 0)
                    .color(r, g, b, a)
                    .endVertex();

            float innerX = centerX + (float)Math.cos(angle) * (radius - thickness);
            float innerY = centerY + (float)Math.sin(angle) * (radius - thickness);
            buffer.vertex(matrix, innerX, innerY, 0)
                    .color(r, g, b, a)
                    .endVertex();
        }

        tessellator.end();
        RenderSystem.disableBlend();
    }

    public static int getRainbowOpaque(int index, float saturation, float brightness, float speed) {
        float hue = (float)((System.currentTimeMillis() + (long)index) % (long)((int)speed)) / speed;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static BlockPos getCameraBlockPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getBlockPosition();
    }

    public static Vec3 getCameraPos() {
        Camera camera = mc.getBlockEntityRenderDispatcher().camera;
        return camera.getPosition();
    }

    public static RegionPos getCameraRegion() {
        return RegionPos.of(getCameraBlockPos());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack) {
        applyRegionalRenderOffset(matrixStack, getCameraRegion());
    }

    public static void applyRegionalRenderOffset(PoseStack matrixStack, RegionPos region) {
        Vec3 offset = region.toVec3().subtract(getCameraPos());
        matrixStack.translate(offset.x, offset.y, offset.z);
    }

    public static void fill(PoseStack pPoseStack, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        innerFill(pPoseStack.last().pose(), pMinX, pMinY, pMaxX, pMaxY, pColor);
    }

    private static void innerFill(Matrix4f pMatrix, float pMinX, float pMinY, float pMaxX, float pMaxY, int pColor) {
        if (pMinX < pMaxX) {
            float i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            float j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        float f3 = (float)(pColor >> 24 & 0xFF) / 255.0F;
        float f = (float)(pColor >> 16 & 0xFF) / 255.0F;
        float f1 = (float)(pColor >> 8 & 0xFF) / 255.0F;
        float f2 = (float)(pColor & 0xFF) / 255.0F;
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(pMatrix, pMinX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMaxY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMaxX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.vertex(pMatrix, pMinX, pMinY, 0.0F).color(f, f1, f2, f3).endVertex();
        Tesselator.getInstance().end();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawRectBound(PoseStack poseStack, float x, float y, float width, float height, int color) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
        float red = (float)(color >> 16 & 0xFF) / 255.0F;
        float green = (float)(color >> 8 & 0xFF) / 255.0F;
        float blue = (float)(color & 0xFF) / 255.0F;
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x + width, y, 0.0F).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
        tesselator.end();
    }

    private static void color(BufferBuilder buffer, Matrix4f matrix, float x, float y, int color) {
        float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
        float red = (float)(color >> 16 & 0xFF) / 255.0F;
        float green = (float)(color >> 8 & 0xFF) / 255.0F;
        float blue = (float)(color & 0xFF) / 255.0F;
        buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    public static void drawRoundedRect(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int color) {
        if (color == 16777215) {
            color = ARGB32.color(255, 255, 255, 255);
        }

        if (edgeRadius < 0.0F) {
            edgeRadius = 0.0F;
        }

        if (edgeRadius > width / 2.0F) {
            edgeRadius = width / 2.0F;
        }

        if (edgeRadius > height / 2.0F) {
            edgeRadius = height / 2.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        drawRectBound(poseStack, x + edgeRadius, y + edgeRadius, width - edgeRadius * 2.0F, height - edgeRadius * 2.0F, color);
        drawRectBound(poseStack, x + edgeRadius, y, width - edgeRadius * 2.0F, edgeRadius, color);
        drawRectBound(poseStack, x + edgeRadius, y + height - edgeRadius, width - edgeRadius * 2.0F, edgeRadius, color);
        drawRectBound(poseStack, x, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
        drawRectBound(poseStack, x + width - edgeRadius, y + edgeRadius, edgeRadius, height - edgeRadius * 2.0F, color);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        float centerX = x + edgeRadius;
        float centerY = y + edgeRadius;
        int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
        color(buffer, matrix, centerX, centerY, color);

        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 180) / (double)(vertices * 4);
            color(
                    buffer,
                    matrix,
                    (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                    (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                    color
            );
        }

        tesselator.end();
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + width - edgeRadius;
        centerY = y + edgeRadius;
        color(buffer, matrix, centerX, centerY, color);

        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 90) / (double)(vertices * 4);
            color(
                    buffer,
                    matrix,
                    (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                    (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                    color
            );
        }

        tesselator.end();
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + edgeRadius;
        centerY = y + height - edgeRadius;
        color(buffer, matrix, centerX, centerY, color);

        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 270) / (double)(vertices * 4);
            color(
                    buffer,
                    matrix,
                    (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                    (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                    color
            );
        }

        tesselator.end();
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        centerX = x + width - edgeRadius;
        centerY = y + height - edgeRadius;
        color(buffer, matrix, centerX, centerY, color);

        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)i / (double)(vertices * 4);
            color(
                    buffer,
                    matrix,
                    (float)((double)centerX + Math.sin(angleRadians) * (double)edgeRadius),
                    (float)((double)centerY + Math.cos(angleRadians) * (double)edgeRadius),
                    color
            );
        }

        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawRoundedRectGradient(PoseStack poseStack, float x, float y, float width, float height, float edgeRadius, int colorFrom, int colorTo) {
        if (edgeRadius < 0.0F) {
            edgeRadius = 0.0F;
        }

        if (edgeRadius > width / 2.0F) {
            edgeRadius = width / 2.0F;
        }

        if (edgeRadius > height / 2.0F) {
            edgeRadius = height / 2.0F;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        
        float alphaFrom = (float)(colorFrom >> 24 & 0xFF) / 255.0F;
        float redFrom = (float)(colorFrom >> 16 & 0xFF) / 255.0F;
        float greenFrom = (float)(colorFrom >> 8 & 0xFF) / 255.0F;
        float blueFrom = (float)(colorFrom & 0xFF) / 255.0F;
        
        float alphaTo = (float)(colorTo >> 24 & 0xFF) / 255.0F;
        float redTo = (float)(colorTo >> 16 & 0xFF) / 255.0F;
        float greenTo = (float)(colorTo >> 8 & 0xFF) / 255.0F;
        float blueTo = (float)(colorTo & 0xFF) / 255.0F;
        
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        tesselator.end();
        

        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        tesselator.end();
        
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + edgeRadius, y + height, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + height, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        tesselator.end();
        
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        tesselator.end();
        
        buffer.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + width - edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        buffer.vertex(matrix, x + width, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        tesselator.end();
        
        int vertices = (int)Math.min(Math.max(edgeRadius, 10.0F), 90.0F);
        
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 180) / (double)(vertices * 4);
            buffer.vertex(matrix, 
                (float)((double)(x + edgeRadius) + Math.sin(angleRadians) * (double)edgeRadius),
                (float)((double)(y + edgeRadius) + Math.cos(angleRadians) * (double)edgeRadius), 
                0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        }
        tesselator.end();
        
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + width - edgeRadius, y + edgeRadius, 0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 90) / (double)(vertices * 4);
            buffer.vertex(matrix, 
                (float)((double)(x + width - edgeRadius) + Math.sin(angleRadians) * (double)edgeRadius),
                (float)((double)(y + edgeRadius) + Math.cos(angleRadians) * (double)edgeRadius), 
                0.0F).color(redFrom, greenFrom, blueFrom, alphaFrom).endVertex();
        }
        tesselator.end();
        
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)(i + 270) / (double)(vertices * 4);
            buffer.vertex(matrix, 
                (float)((double)(x + edgeRadius) + Math.sin(angleRadians) * (double)edgeRadius),
                (float)((double)(y + height - edgeRadius) + Math.cos(angleRadians) * (double)edgeRadius), 
                0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        }
        tesselator.end();
    
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + width - edgeRadius, y + height - edgeRadius, 0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        for (int i = 0; i <= vertices; i++) {
            double angleRadians = (Math.PI * 2) * (double)i / (double)(vertices * 4);
            buffer.vertex(matrix, 
                (float)((double)(x + width - edgeRadius) + Math.sin(angleRadians) * (double)edgeRadius),
                (float)((double)(y + height - edgeRadius) + Math.cos(angleRadians) * (double)edgeRadius), 
                0.0F).color(redTo, greenTo, blueTo, alphaTo).endVertex();
        }
        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void drawSolidBox(PoseStack matrixStack) {
        drawSolidBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawSolidBox(AABB bb, PoseStack matrixStack) {
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();
        Matrix4f matrix = matrixStack.last().pose();
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawOutlinedBox(PoseStack matrixStack) {
        drawOutlinedBox(DEFAULT_BOX, matrixStack);
    }

    public static void drawOutlinedBox(AABB bb, PoseStack matrixStack) {
        Matrix4f matrix = matrixStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.minY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.minZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.maxX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.maxZ).endVertex();
        bufferBuilder.vertex(matrix, (float)bb.minX, (float)bb.maxY, (float)bb.minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawSolidBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        drawSolidBox(bb, bufferBuilder);
        BufferUploader.reset();
        vertexBuffer.bind();
        RenderedBuffer buffer = bufferBuilder.end();
        vertexBuffer.upload(buffer);
        VertexBuffer.unbind();
    }

    public static void drawSolidBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static void drawOutlinedBox(AABB bb, VertexBuffer vertexBuffer) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION);
        drawOutlinedBox(bb, bufferBuilder);
        vertexBuffer.upload(bufferBuilder.end());
    }

    public static void drawOutlinedBox(AABB bb, BufferBuilder bufferBuilder) {
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.minY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.minZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.maxZ).endVertex();
        bufferBuilder.vertex(bb.minX, bb.maxY, bb.minZ).endVertex();
    }

    public static boolean isHovering(int mouseX, int mouseY, float xLeft, float yUp, float xRight, float yBottom) {
        return (float)mouseX > xLeft && (float)mouseX < xRight && (float)mouseY > yUp && (float)mouseY < yBottom;
    }

    public static boolean isHoveringBound(int mouseX, int mouseY, float xLeft, float yUp, float width, float height) {
        return (float)mouseX > xLeft && (float)mouseX < xLeft + width && (float)mouseY > yUp && (float)mouseY < yUp + height;
    }

    public static void fillBound(PoseStack stack, float left, float top, float width, float height, int color) {
        float right = left + width;
        float bottom = top + height;
        fill(stack, left, top, right, bottom, color);
    }

    public static void drawStencilRoundedRect(GuiGraphics graphics, float x, float y, float width, float height, float cornerRadius, int blurStrength, int color) {
        RenderSystem.assertOnRenderThread();
        int textureId = -1;

        try {
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(graphics.pose(), x, y, width, height, cornerRadius, 0xFFFFFFFF);
            StencilUtils.erase(true);

            Minecraft mc = Minecraft.getInstance();
            int windowWidth = mc.getWindow().getWidth();
            int windowHeight = mc.getWindow().getHeight();
            int scaledWidth = mc.getWindow().getGuiScaledWidth();
            int scaledHeight = mc.getWindow().getGuiScaledHeight();

            int pixelX = (int) (x * windowWidth / scaledWidth);
            int pixelY = (int) (y * windowHeight / scaledHeight);
            int pixelWidth = (int) (width * windowWidth / scaledWidth);
            int pixelHeight = (int) (height * windowHeight / scaledHeight);

            pixelX = Math.max(0, pixelX);
            pixelY = Math.max(0, pixelY);
            pixelWidth = Math.min(windowWidth - pixelX, pixelWidth);
            pixelHeight = Math.min(windowHeight - pixelY, pixelHeight);

            textureId = TextureUtil.generateTextureId();
            RenderSystem.bindTexture(textureId);

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, pixelWidth, pixelHeight, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            ByteBuffer buffer = ByteBuffer.allocateDirect(pixelWidth * pixelHeight * 4);
            GL11.glReadPixels(pixelX, windowHeight - pixelY - pixelHeight, pixelWidth, pixelHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, pixelWidth, pixelHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, textureId);

            Matrix4f matrix = graphics.pose().last().pose();
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.vertex(matrix, x, y + height, 0).uv(0, 1).endVertex();
            builder.vertex(matrix, x + width, y + height, 0).uv(1, 1).endVertex();
            builder.vertex(matrix, x + width, y, 0).uv(1, 0).endVertex();
            builder.vertex(matrix, x, y, 0).uv(0, 0).endVertex();
            Tesselator.getInstance().end();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderUtils.fillBound(graphics.pose(), x, y, width, height, color);

            StencilUtils.dispose();

        } finally {
            if (textureId != -1) {
                RenderSystem.deleteTexture(textureId);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void drawSolidBox(BufferBuilder bufferBuilder, Matrix4f matrix, AABB box) {
        float minX = (float)(box.minX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float minY = (float)(box.minY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float minZ = (float)(box.minZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        float maxX = (float)(box.maxX - mc.getEntityRenderDispatcher().camera.getPosition().x());
        float maxY = (float)(box.maxY - mc.getEntityRenderDispatcher().camera.getPosition().y());
        float maxZ = (float)(box.maxZ - mc.getEntityRenderDispatcher().camera.getPosition().z());
        bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, minZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, maxX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, minZ).endVertex();
        bufferBuilder.vertex(matrix, minX, minY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, maxZ).endVertex();
        bufferBuilder.vertex(matrix, minX, maxY, minZ).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void drawCircle(PoseStack poseStack, float centerX, float centerY, float radius, int color) {
        drawCircle(poseStack, centerX, centerY, radius, color, 20);
    }
    
    public static void drawCircle(PoseStack poseStack, float centerX, float centerY, float radius, int color, int segments) {
        if (radius <= 0) return;
        
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        
        float a = (float)(color >> 24 & 0xFF) / 255.0F;
        float r = (float)(color >> 16 & 0xFF) / 255.0F;
        float g = (float)(color >> 8 & 0xFF) / 255.0F;
        float b = (float)(color & 0xFF) / 255.0F;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, centerX, centerY, 0).color(r, g, b, a).endVertex();
        
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = centerX + (float)(Math.cos(angle) * radius);
            float y = centerY + (float)(Math.sin(angle) * radius);
            buffer.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        }
        
        tessellator.end();
        RenderSystem.disableBlend();
    }
    
    public static void drawTriangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = new Matrix4f();
        
        float a = (float)(color >> 24 & 0xFF) / 255.0F;
        float r = (float)(color >> 16 & 0xFF) / 255.0F;
        float g = (float)(color >> 8 & 0xFF) / 255.0F;
        float b = (float)(color & 0xFF) / 255.0F;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        
        buffer.begin(Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, 0).color(r, g, b, a).endVertex();
        
        tessellator.end();
        RenderSystem.disableBlend();
    }
    
    /**
     * 绘制带有特定圆角的矩形
     * @param poseStack 渲染堆栈
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param topLeftRadius 左上角圆角半径
     * @param topRightRadius 右上角圆角半径
     * @param bottomLeftRadius 左下角圆角半径
     * @param bottomRightRadius 右下角圆角半径
     * @param color 颜色
     */
    public static void drawRoundedRectCustom(PoseStack poseStack, float x, float y, float width, float height,
                                           float topLeftRadius, float topRightRadius, float bottomLeftRadius, float bottomRightRadius, int color) {
        if (color == 16777215) {
            color = ARGB32.color(255, 255, 255, 255);
        }

        // 规范化半径，避免超过宽高一半
        topLeftRadius = Math.max(0.0F, Math.min(topLeftRadius, Math.min(width, height) / 2.0F));
        topRightRadius = Math.max(0.0F, Math.min(topRightRadius, Math.min(width, height) / 2.0F));
        bottomLeftRadius = Math.max(0.0F, Math.min(bottomLeftRadius, Math.min(width, height) / 2.0F));
        bottomRightRadius = Math.max(0.0F, Math.min(bottomRightRadius, Math.min(width, height) / 2.0F));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        // 计算各边需要保留的最大半径，用于填充中间与边带区域
        float leftInset = Math.max(topLeftRadius, bottomLeftRadius);
        float rightInset = Math.max(topRightRadius, bottomRightRadius);
        float topInset = Math.max(topLeftRadius, topRightRadius);
        float bottomInset = Math.max(bottomLeftRadius, bottomRightRadius);

        // 中心矩形
        float centerX = x + leftInset;
        float centerY = y + topInset;
        float centerW = Math.max(0.0F, width - leftInset - rightInset);
        float centerH = Math.max(0.0F, height - topInset - bottomInset);
        if (centerW > 0.0F && centerH > 0.0F) {
            drawRectBound(poseStack, centerX, centerY, centerW, centerH, color);
        }

        // 顶部带（不包含圆角区域）
        if (topInset > 0.0F) {
            float topBandX = x + topLeftRadius;
            float topBandW = Math.max(0.0F, width - topLeftRadius - topRightRadius);
            if (topBandW > 0.0F) {
                drawRectBound(poseStack, topBandX, y, topBandW, topInset, color);
            }
        }

        // 底部带（不包含圆角区域）
        if (bottomInset > 0.0F) {
            float bottomBandX = x + bottomLeftRadius;
            float bottomBandW = Math.max(0.0F, width - bottomLeftRadius - bottomRightRadius);
            if (bottomBandW > 0.0F) {
                drawRectBound(poseStack, bottomBandX, y + height - bottomInset, bottomBandW, bottomInset, color);
            }
        }

        // 左侧带（不包含圆角区域）
        if (leftInset > 0.0F) {
            float leftBandY = y + topLeftRadius;
            float leftBandH = Math.max(0.0F, height - topLeftRadius - bottomLeftRadius);
            if (leftBandH > 0.0F) {
                drawRectBound(poseStack, x, leftBandY, leftInset, leftBandH, color);
            }
        }

        // 右侧带（不包含圆角区域）
        if (rightInset > 0.0F) {
            float rightBandY = y + topRightRadius;
            float rightBandH = Math.max(0.0F, height - topRightRadius - bottomRightRadius);
            if (rightBandH > 0.0F) {
                drawRectBound(poseStack, x + width - rightInset, rightBandY, rightInset, rightBandH, color);
            }
        }

        // 四个圆角
        if (topLeftRadius > 0) {
            drawCornerArc(poseStack, x + topLeftRadius, y + topLeftRadius, topLeftRadius, 180, 270, color);
        }
        if (topRightRadius > 0) {
            drawCornerArc(poseStack, x + width - topRightRadius, y + topRightRadius, topRightRadius, 270, 360, color);
        }
        if (bottomLeftRadius > 0) {
            drawCornerArc(poseStack, x + bottomLeftRadius, y + height - bottomLeftRadius, bottomLeftRadius, 90, 180, color);
        }
        if (bottomRightRadius > 0) {
            drawCornerArc(poseStack, x + width - bottomRightRadius, y + height - bottomRightRadius, bottomRightRadius, 0, 90, color);
        }
    }
    
    private static void drawCornerArc(PoseStack poseStack, float centerX, float centerY, float radius, int startAngle, int endAngle, int color) {
        if (radius <= 0) return;
        
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();
        
        float alpha = (float)(color >> 24 & 0xFF) / 255.0F;
        float red = (float)(color >> 16 & 0xFF) / 255.0F;
        float green = (float)(color >> 8 & 0xFF) / 255.0F;
        float blue = (float)(color & 0xFF) / 255.0F;
        
        buffer.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, centerX, centerY, 0.0F).color(red, green, blue, alpha).endVertex();
        
        for (int i = startAngle; i <= endAngle; i++) {
            double angle = Math.toRadians(i);
            float x = centerX + (float)(Math.cos(angle) * radius);
            float y = centerY + (float)(Math.sin(angle) * radius);
            buffer.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
        }
        
        tesselator.end();
    }
    /**
     * 优化的圆角矩形绘制方法
     */
    public static void drawRoundedRectOptimized(PoseStack poseStack, float x, float y, float width, float height,
                                                float radius, int color) {
        drawRoundedRectCustom(poseStack, x, y, width, height, radius, radius, radius, radius, color);
    }

    /**
     * 绘制带阴影的圆角矩形
     */
    public static void drawRoundedRectWithShadow(PoseStack poseStack, float x, float y, float width, float height,
                                                 float radius, int color, int shadowColor, float shadowOffset) {
        // 绘制阴影
        drawRoundedRectCustom(poseStack, x + shadowOffset, y + shadowOffset, width, height,
                radius, radius, radius, radius, shadowColor);
        // 绘制主体
        drawRoundedRectCustom(poseStack, x, y, width, height,
                radius, radius, radius, radius, color);
    }

    /**
     * 绘制渐变圆角矩形
     */
    public static void drawRoundedRectGradient(PoseStack poseStack, float x, float y, float width, float height,
                                               float radius, int startColor, int endColor, boolean vertical) {
        if (radius < 0.0F) radius = 0.0F;
        if (radius > width / 2.0F) radius = width / 2.0F;
        if (radius > height / 2.0F) radius = height / 2.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // 提取颜色分量
        float sa = (float)(startColor >> 24 & 0xFF) / 255.0F;
        float sr = (float)(startColor >> 16 & 0xFF) / 255.0F;
        float sg = (float)(startColor >> 8 & 0xFF) / 255.0F;
        float sb = (float)(startColor & 0xFF) / 255.0F;

        float ea = (float)(endColor >> 24 & 0xFF) / 255.0F;
        float er = (float)(endColor >> 16 & 0xFF) / 255.0F;
        float eg = (float)(endColor >> 8 & 0xFF) / 255.0F;
        float eb = (float)(endColor & 0xFF) / 255.0F;

        if (vertical) {
            // 垂直渐变
            addGradientQuad(buffer, matrix, x + radius, y + radius, width - 2 * radius, height - 2 * radius,
                    sr, sg, sb, sa, er, eg, eb, ea, true);
        } else {
            // 水平渐变
            addGradientQuad(buffer, matrix, x + radius, y + radius, width - 2 * radius, height - 2 * radius,
                    sr, sg, sb, sa, er, eg, eb, ea, false);
        }

        tessellator.end();

        // 绘制圆角部分（简化处理，实际可能需要更复杂的渐变处理）
        drawRoundedRectCustom(poseStack, x, y, width, height, radius, radius, radius, radius, startColor);

        RenderSystem.disableBlend();
    }

    private static void addGradientQuad(BufferBuilder buffer, Matrix4f matrix,
                                        float x, float y, float width, float height,
                                        float sr, float sg, float sb, float sa,
                                        float er, float eg, float eb, float ea,
                                        boolean vertical) {
        if (vertical) {
            buffer.vertex(matrix, x, y + height, 0).color(sr, sg, sb, sa).endVertex();
            buffer.vertex(matrix, x + width, y + height, 0).color(sr, sg, sb, sa).endVertex();
            buffer.vertex(matrix, x + width, y, 0).color(er, eg, eb, ea).endVertex();
            buffer.vertex(matrix, x, y, 0).color(er, eg, eb, ea).endVertex();
        } else {
            buffer.vertex(matrix, x, y + height, 0).color(sr, sg, sb, sa).endVertex();
            buffer.vertex(matrix, x + width, y + height, 0).color(er, eg, eb, ea).endVertex();
            buffer.vertex(matrix, x + width, y, 0).color(er, eg, eb, ea).endVertex();
            buffer.vertex(matrix, x, y, 0).color(sr, sg, sb, sa).endVertex();
        }
    }


    public static void drawRoundedRectOutline(PoseStack poseStack, float x, float y, float size, float size1, float v, float v1, int rgb) {
    }

    public static void drawRoundedRect(PoseStack stack, Color color) {
    }
}