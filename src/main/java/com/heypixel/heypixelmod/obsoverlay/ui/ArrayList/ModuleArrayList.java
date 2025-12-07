package com.heypixel.heypixelmod.obsoverlay.ui.ArrayList;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleManager;
import com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.SmoothAnimationTimer;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.text.CustomTextRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;

import java.awt.Color;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModuleArrayList {

    public enum Mode {
        Normal,
        Exhibition
    }

    private static List<Module> renderModules;
    private static final ArrayList<Vector4f> blurMatrices = new ArrayList<>();
    public static final int backgroundColor = new Color(0, 0, 0, 60).getRGB(); // 改为深色背景

    private static final Map<Module, Float> widthCache = new HashMap<>();
    private static final Map<Module, String> nameCache = new HashMap<>();
    private static float cachedArrayListSize = -1.0F;
    private static boolean cachedPrettyName = false;
    private static boolean cachedHideRender = false;
    private static float cachedMaxWidth = 0.0F;

    private static String getModuleDisplayName(Module module, boolean pretty) {
        String name = pretty ? module.getPrettyName() : module.getName();
        return name + (module.getSuffix() == null ? "" : " §7" + module.getSuffix());
    }

    public static void onShader(EventShader e) {
        for (Vector4f blurMatrix : blurMatrices) {
            RenderUtils.drawRoundedRect(e.getStack(),
                    blurMatrix.x(), blurMatrix.y(),
                    blurMatrix.z(), blurMatrix.w(),
                    0F, Integer.MIN_VALUE); // 圆角改为0
        }
    }

    private static void drawVerticalAnimatedRainbowBar(PoseStack stack, float x, float y, float width, float height,
                                                       float rainbowSpeed, float rainbowOffset) {
        int segments = Math.max(4, Math.min(12, (int) (height / 2.0F)));
        float segmentHeight = height / (float) segments;
        for (int s = 0; s < segments; s++) {
            float segY0 = y + s * segmentHeight;
            float segY1 = (s == segments - 1) ? (y + height) : (segY0 + segmentHeight);
            float sampleY = (segY0 + segY1) * 0.5F;
            int color = RenderUtils.getRainbowOpaque(
                    (int) (-sampleY * rainbowOffset),
                    1.0F, 1.0F, (21.0F - rainbowSpeed) * 1000.0F
            );
            RenderUtils.fill(stack, x, segY0, x + width, segY1, color);
        }
    }

    public static void onRender(EventRender2D e, Mode mode, boolean capsule, boolean prettyModuleName,
                                boolean hideRenderModules, boolean rainbow, float rainbowSpeed, float rainbowOffset,
                                String arrayListDirection, float xOffset, float yOffset, float arrayListSize,
                                float arrayListSpacing) {
        blurMatrices.clear();
        CustomTextRenderer font = Fonts.opensans;
        PoseStack stack = e.getStack();
        stack.pushPose();
        Minecraft mc = Minecraft.getInstance();
        ModuleManager moduleManager = Naven.getInstance().getModuleManager();

        boolean needRebuild = Module.update
                || renderModules == null
                || cachedArrayListSize != arrayListSize
                || cachedPrettyName != prettyModuleName
                || cachedHideRender != hideRenderModules;

        if (needRebuild) {
            renderModules = new ArrayList<>(moduleManager.getModules());
            if (hideRenderModules) {
                renderModules.removeIf(module -> module.getCategory() == Category.RENDER);
            }

            widthCache.clear();
            nameCache.clear();
            cachedMaxWidth = 0.0F;
            for (Module module : renderModules) {
                String display = getModuleDisplayName(module, prettyModuleName);
                nameCache.put(module, display);
                float width = font.getWidth(display, (double) arrayListSize);
                widthCache.put(module, width);
                cachedMaxWidth = Math.max(cachedMaxWidth, width);
            }

            renderModules.sort((m1, m2) ->
                    Float.compare(widthCache.getOrDefault(m2, 0.0F), widthCache.getOrDefault(m1, 0.0F)));

            cachedArrayListSize = arrayListSize;
            cachedPrettyName = prettyModuleName;
            cachedHideRender = hideRenderModules;
            Module.update = false;
        }

        float maxWidth = cachedMaxWidth;
        if (maxWidth < 50.0F) maxWidth = 100.0F;

        HUDEditor.HUDElement arrayListElement = HUDEditor.getInstance().getHUDElement("arraylist");
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        float arrayListX, arrayListY;
        if (arrayListElement != null) {
            arrayListX = "Right".equals(arrayListDirection)
                    ? (screenWidth - maxWidth) + xOffset
                    : (float) arrayListElement.x + xOffset;
            arrayListY = (float) arrayListElement.y + yOffset;
        } else {
            arrayListX = "Right".equals(arrayListDirection)
                    ? screenWidth - maxWidth + xOffset
                    : 3.0F + xOffset;
            arrayListY = 3.0F + yOffset;
        }

        float totalHeight = 0.0F;
        double fontHeight = font.getHeight(true, (double) arrayListSize);
        final int rainbowPeriodMs = (int) ((21.0F - rainbowSpeed) * 1000.0F);
        final float rainbowOffsetMul = rainbowOffset;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        for (Module module : renderModules) {
            SmoothAnimationTimer animation = module.getAnimation();
            animation.target = module.isEnabled() ? 100.0F : 0.0F;
            animation.update(true);

            if (animation.value > 0.0F) {
                String displayName = nameCache.get(module);
                if (displayName == null) {
                    displayName = getModuleDisplayName(module, prettyModuleName);
                    nameCache.put(module, displayName);
                }

                float stringWidth = widthCache.getOrDefault(module,
                        font.getWidth(displayName, (double) arrayListSize));
                float alpha = animation.value / 100.0F;
                float moduleWidth = stringWidth + 5.0F; // 减少宽度以匹配HUD样式
                float moduleHeight = (float) ((double) (alpha) * fontHeight);

                float offsetX = "Left".equals(arrayListDirection)
                        ? (-stringWidth * (1.0F - alpha))
                        : (maxWidth - stringWidth * alpha);
                float moduleX = arrayListX + offsetX;
                float moduleY = arrayListY + totalHeight + 2.0F; // 调整Y位置

                if (mode == Mode.Normal) {
                    RenderUtils.drawRoundedRect(
                            stack,
                            moduleX, moduleY,
                            moduleWidth, moduleHeight,
                            0F, // 圆角改为0
                            backgroundColor
                    );
                    blurMatrices.add(new Vector4f(moduleX, moduleY, moduleWidth, moduleHeight));
                }

                int textColor = -1;
                if (rainbow) {
                    float baseY = arrayListY + totalHeight + 1.0F;
                    textColor = RenderUtils.getRainbowOpaque(
                            (int) (-baseY * rainbowOffsetMul),
                            1.0F, 1.0F, rainbowPeriodMs
                    );
                }

                font.setAlpha(alpha);
                font.render(
                        stack,
                        displayName,
                        moduleX + 1.5F, // 调整文字X位置
                        arrayListY + totalHeight + 1.0F, // 调整文字Y位置
                        new Color(textColor),
                        true,
                        arrayListSize
                );

                // 在右侧添加竖线 - 匹配HUD样式
                if (mode == Mode.Normal) {
                    float lineWidth = 2.0F;
                    float lineX = moduleX + moduleWidth - lineWidth;
                    float lineY = moduleY + 2.0F;
                    float lineHeight = moduleHeight - 4.0F;

                    if (lineHeight > 0) {
                        RenderUtils.drawRoundedRect(stack, lineX, lineY, lineWidth, lineHeight, 0F, textColor);
                    }
                }

                if (rainbow && capsule && mode == Mode.Normal) {
                    float barWidth = 3.0F;
                    float barPadding = 2.0F;
                    float barX = "Left".equals(arrayListDirection)
                            ? (moduleX - barWidth - barPadding)
                            : (moduleX + moduleWidth + barPadding);

                    drawVerticalAnimatedRainbowBar(
                            stack,
                            barX, moduleY,
                            barWidth, moduleHeight,
                            rainbowSpeed, rainbowOffset
                    );
                }

                totalHeight += (float) (fontHeight * alpha + arrayListSpacing);
                // 添加模块之间的额外空隙
                totalHeight += 2.0F;
            }
        }

        if (arrayListElement != null) {
            arrayListElement.width = maxWidth + 10.0F;
            arrayListElement.height = Math.max(totalHeight + 10.0F, 50.0F);
            if ("Right".equals(arrayListDirection)) {
                arrayListElement.x = arrayListX + maxWidth - xOffset;
            }
        }

        font.setAlpha(1.0F);
        stack.popPose();
    }
}