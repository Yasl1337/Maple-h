package com.heypixel.heypixelmod.obsoverlay.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class TextureUtils {

    /**
     * 绘制纹理
     * @param poseStack 姿势堆栈
     * @param texture 纹理资源位置
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     */
    public static void drawTexture(PoseStack poseStack, ResourceLocation texture, float x, float y, float width, float height) {
        drawTexture(poseStack, texture, x, y, width, height, 0, 0, 1, 1, 1, 1, 1, 1);
    }

    /**
     * 绘制纹理（带颜色和透明度）
     * @param poseStack 姿势堆栈
     * @param texture 纹理资源位置
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param red 红色分量 (0-1)
     * @param green 绿色分量 (0-1)
     * @param blue 蓝色分量 (0-1)
     * @param alpha 透明度 (0-1)
     */
    public static void drawTexture(PoseStack poseStack, ResourceLocation texture, float x, float y, float width, float height,
                                   float red, float green, float blue, float alpha) {
        drawTexture(poseStack, texture, x, y, width, height, 0, 0, 1, 1, red, green, blue, alpha);
    }

    /**
     * 绘制纹理（完整参数）
     * @param poseStack 姿势堆栈
     * @param texture 纹理资源位置
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param u 纹理U坐标
     * @param v 纹理V坐标
     * @param uWidth 纹理U宽度
     * @param vHeight 纹理V高度
     * @param red 红色分量 (0-1)
     * @param green 绿色分量 (0-1)
     * @param blue 蓝色分量 (0-1)
     * @param alpha 透明度 (0-1)
     */
    public static void drawTexture(PoseStack poseStack, ResourceLocation texture, float x, float y, float width, float height,
                                   float u, float v, float uWidth, float vHeight,
                                   float red, float green, float blue, float alpha) {
        if (texture == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        Matrix4f matrix = poseStack.last().pose();
        buffer.vertex(matrix, x, y + height, 0).uv(u, v + vHeight).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).uv(u + uWidth, v + vHeight).endVertex();
        buffer.vertex(matrix, x + width, y, 0).uv(u + uWidth, v).endVertex();
        buffer.vertex(matrix, x, y, 0).uv(u, v).endVertex();

        tessellator.end();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * 绘制圆形纹理（用于圆形图标）
     * @param poseStack 姿势堆栈
     * @param texture 纹理资源位置
     * @param x X坐标
     * @param y Y坐标
     * @param radius 半径
     * @param red 红色分量 (0-1)
     * @param green 绿色分量 (0-1)
     * @param blue 蓝色分量 (0-1)
     * @param alpha 透明度 (0-1)
     */
    public static void drawCircularTexture(PoseStack poseStack, ResourceLocation texture, float x, float y, float radius,
                                           float red, float green, float blue, float alpha) {
        if (texture == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);

        Matrix4f matrix = poseStack.last().pose();
        float centerX = x + radius;
        float centerY = y + radius;

        // 中心点
        buffer.vertex(matrix, centerX, centerY, 0).uv(0.5f, 0.5f).endVertex();

        // 圆形边缘点
        int segments = 36;
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (2.0f * Math.PI * i / segments);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            float u = 0.5f + 0.5f * (float) Math.cos(angle);
            float v = 0.5f + 0.5f * (float) Math.sin(angle);
            buffer.vertex(matrix, centerX + dx, centerY + dy, 0).uv(u, v).endVertex();
        }

        tessellator.end();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * 绘制带圆角的纹理
     * @param poseStack 姿势堆栈
     * @param texture 纹理资源位置
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param radius 圆角半径
     * @param red 红色分量 (0-1)
     * @param green 绿色分量 (0-1)
     * @param blue 蓝色分量 (0-1)
     * @param alpha 透明度 (0-1)
     */
    public static void drawRoundedTexture(PoseStack poseStack, ResourceLocation texture, float x, float y, float width, float height,
                                          float radius, float red, float green, float blue, float alpha) {
        if (texture == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        Matrix4f matrix = poseStack.last().pose();

        // 计算圆角矩形的顶点和UV坐标
        addRoundedRect(buffer, matrix, x, y, width, height, radius);

        tessellator.end();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * 添加圆角矩形的顶点数据
     */
    private static void addRoundedRect(BufferBuilder buffer, Matrix4f matrix, float x, float y, float width, float height, float radius) {
        float u1 = 0;
        float v1 = 0;
        float u2 = 1;
        float v2 = 1;

        // 限制圆角半径不超过宽度或高度的一半
        radius = Math.min(radius, Math.min(width, height) / 2);

        // 中心矩形部分
        addQuad(buffer, matrix,
                x + radius, y + radius,
                width - 2 * radius, height - 2 * radius,
                u1 + radius / width, v1 + radius / height,
                u2 - radius / width, v2 - radius / height);

        // 四个边矩形
        // 上边
        addQuad(buffer, matrix,
                x + radius, y,
                width - 2 * radius, radius,
                u1 + radius / width, v1,
                u2 - radius / width, v1 + radius / height);
        // 下边
        addQuad(buffer, matrix,
                x + radius, y + height - radius,
                width - 2 * radius, radius,
                u1 + radius / width, v2 - radius / height,
                u2 - radius / width, v2);
        // 左边
        addQuad(buffer, matrix,
                x, y + radius,
                radius, height - 2 * radius,
                u1, v1 + radius / height,
                u1 + radius / width, v2 - radius / height);
        // 右边
        addQuad(buffer, matrix,
                x + width - radius, y + radius,
                radius, height - 2 * radius,
                u2 - radius / width, v1 + radius / height,
                u2, v2 - radius / height);

        // 四个圆角
        addCorner(buffer, matrix, x + radius, y + radius, radius, 180, 270, u1, v1, u1 + radius / width, v1 + radius / height); // 左上
        addCorner(buffer, matrix, x + width - radius, y + radius, radius, 270, 360, u2 - radius / width, v1, u2, v1 + radius / height); // 右上
        addCorner(buffer, matrix, x + width - radius, y + height - radius, radius, 0, 90, u2 - radius / width, v2 - radius / height, u2, v2); // 右下
        addCorner(buffer, matrix, x + radius, y + height - radius, radius, 90, 180, u1, v2 - radius / height, u1 + radius / width, v2); // 左下
    }

    /**
     * 添加四边形顶点数据
     */
    private static void addQuad(BufferBuilder buffer, Matrix4f matrix,
                                float x, float y, float width, float height,
                                float u1, float v1, float u2, float v2) {
        buffer.vertex(matrix, x, y + height, 0).uv(u1, v2).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).uv(u2, v2).endVertex();
        buffer.vertex(matrix, x + width, y, 0).uv(u2, v1).endVertex();

        buffer.vertex(matrix, x + width, y, 0).uv(u2, v1).endVertex();
        buffer.vertex(matrix, x, y, 0).uv(u1, v1).endVertex();
        buffer.vertex(matrix, x, y + height, 0).uv(u1, v2).endVertex();
    }

    /**
     * 添加圆角顶点数据
     */
    private static void addCorner(BufferBuilder buffer, Matrix4f matrix,
                                  float centerX, float centerY, float radius,
                                  float startAngle, float endAngle,
                                  float u1, float v1, float u2, float v2) {
        int segments = 10;
        float angleStep = (endAngle - startAngle) / segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = (float) Math.toRadians(startAngle + i * angleStep);
            float angle2 = (float) Math.toRadians(startAngle + (i + 1) * angleStep);

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            float uCenter = (u1 + u2) / 2;
            float vCenter = (v1 + v2) / 2;
            float ux1 = uCenter + (x1 - centerX) / (2 * radius) * (u2 - u1);
            float vy1 = vCenter + (y1 - centerY) / (2 * radius) * (v2 - v1);
            float ux2 = uCenter + (x2 - centerX) / (2 * radius) * (u2 - u1);
            float vy2 = vCenter + (y2 - centerY) / (2 * radius) * (v2 - v1);

            buffer.vertex(matrix, centerX, centerY, 0).uv(uCenter, vCenter).endVertex();
            buffer.vertex(matrix, x1, y1, 0).uv(ux1, vy1).endVertex();
            buffer.vertex(matrix, x2, y2, 0).uv(ux2, vy2).endVertex();
        }
    }

    /**
     * 检查纹理是否存在
     * @param texture 纹理资源位置
     * @return 是否存在
     */
    public static boolean textureExists(ResourceLocation texture) {
        if (texture == null) return false;
        try {
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}