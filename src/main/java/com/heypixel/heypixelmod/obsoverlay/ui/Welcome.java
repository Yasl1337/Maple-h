package com.heypixel.heypixelmod.obsoverlay.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

public class Welcome extends Screen {
    // 修正：使用 ResourceLocation.parse() 替代过时的构造函数
    private static final ResourceLocation BACKGROUND_TEXTURE = ResourceLocation.parse("heypixel:textures/images/background.png");

    private int fadeInStage = 0;
    private int fadeAlpha = 0;
    private static final int FADE_IN_DURATION = 30;
    private static final int FADE_OUT_DURATION = 30;
    private static final int MAX_ALPHA = 255;
    private boolean textureLoaded = false;

    public Welcome() {
        super(Component.literal("Welcome"));
    }

    @Override
    protected void init() {
        super.init();
        textureLoaded = checkTextureLoaded();
    }

    private boolean checkTextureLoaded() {
        try {
            Minecraft.getInstance().getResourceManager().getResourceOrThrow(BACKGROUND_TEXTURE);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to load background texture: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void tick() {
        switch (fadeInStage) {
            case 0:
                fadeAlpha += (MAX_ALPHA / FADE_IN_DURATION);
                if (fadeAlpha >= MAX_ALPHA) {
                    fadeAlpha = MAX_ALPHA;
                    fadeInStage = 1; // 停留阶段
                }
                break;
            case 1:
                // 停留阶段（可通过点击/按键跳过）
                break;
            case 2:
                fadeAlpha -= (MAX_ALPHA / FADE_OUT_DURATION);
                if (fadeAlpha <= 0) {
                    fadeAlpha = 0;
                    fadeInStage = 3;
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new TitleScreen());
                    }
                }
                break;
            case 3:
                break;
        }
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderText(guiGraphics);
    }

    public void renderBackground(@Nonnull GuiGraphics guiGraphics) {
        Window window = Minecraft.getInstance().getWindow();
        int width = window.getGuiScaledWidth();
        int height = window.getGuiScaledHeight();

        // 绘制黑色背景
        guiGraphics.fill(0, 0, width, height, 0xFF000000);

        // 绘制背景纹理（带淡入淡出效果）
        if (textureLoaded) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, fadeAlpha / 255.0F);
            guiGraphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, width, height, width, height);
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        // 绘制半透明覆盖层
        int overlayAlpha = (int)((fadeAlpha / 255.0f) * 150);
        int overlayColor = (overlayAlpha << 24); // 黑色半透明
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.fill(0, 0, width, height, overlayColor);
        RenderSystem.disableBlend();
    }

    private void renderText(GuiGraphics guiGraphics) {
        int textAlpha = fadeAlpha;
        int shadowAlpha = textAlpha / 2;
        int textColor = 0xFFFFFF | (textAlpha << 24); // 白色文本
        int shadowColor = 0x000000 | (shadowAlpha << 24); // 黑色阴影

        String title = "Welcome to Maple";
        int titleY = this.height / 2;

        // 绘制文字阴影
        guiGraphics.drawCenteredString(this.font, title, this.width / 2 + 1, titleY + 1, shadowColor);
        // 绘制文字本体
        guiGraphics.drawCenteredString(this.font, title, this.width / 2, titleY, textColor);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fadeInStage == 1) {
            fadeInStage = 2; // 按键跳过停留阶段，开始淡出
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fadeInStage == 1) {
            fadeInStage = 2; // 点击跳过停留阶段，开始淡出
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}