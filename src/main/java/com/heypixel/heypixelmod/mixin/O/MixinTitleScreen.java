package com.heypixel.heypixelmod.mixin.O;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 对标题界面的Mixin注入类，用于在标题界面初始化时执行自定义逻辑
 */
@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Unique
    private static final ResourceLocation WINDOW_ICON = ResourceLocation.parse("navenxd:icon/maple_icon.png");
    @Unique
    private boolean windowIconSet = false;

    @Unique
    private float titleGlowPhase = 0.0f;
    @Unique
    private void setWindowIcon() {
        if (windowIconSet) return;

        try {
            Minecraft minecraft = Minecraft.getInstance();
            long window = minecraft.getWindow().getWindow();

            InputStream iconStream = Minecraft.getInstance().getResourceManager().getResource(WINDOW_ICON).get().open();
            BufferedImage image = ImageIO.read(iconStream);
            iconStream.close();

            int[] pixels = new int[image.getWidth() * image.getHeight()];
            image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

            ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = pixels[y * image.getWidth() + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF));
                    buffer.put((byte) ((pixel >> 8) & 0xFF));
                    buffer.put((byte) (pixel & 0xFF));
                    buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }

            buffer.flip();

            GLFWImage.Buffer iconBuffer = GLFWImage.malloc(1);
            GLFWImage icon = GLFWImage.malloc();
            icon.set(image.getWidth(), image.getHeight(), buffer);
            iconBuffer.put(0, icon);

            GLFW.glfwSetWindowIcon(window, iconBuffer);

            icon.free();
            iconBuffer.free();
            windowIconSet = true;
        } catch (Exception e) {
            System.err.println("Failed to set window icon: " + e.getMessage());
        }
    }
    /**
     * 在标题界面初始化完成后注入逻辑
     * @param ci 回调信息对象
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // 使用Minecraft的主线程执行器，确保UI操作在主线程进行
        Minecraft.getInstance().execute(() -> {
            // 检查当前屏幕是否为标题界面，避免重复切换导致异常
            if (Minecraft.getInstance().screen instanceof TitleScreen) {
                // 切换到自定义的MainUI界面
                Minecraft.getInstance().setScreen(new TitleScreen());
            }
        });
    }
}