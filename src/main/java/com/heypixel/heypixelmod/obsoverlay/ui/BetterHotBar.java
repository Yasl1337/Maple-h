package com.heypixel.heypixelmod.obsoverlay.ui;

import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventShader;
import com.heypixel.heypixelmod.obsoverlay.utils.AnimationUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class BetterHotBar {
    
    private static final Minecraft mc = Minecraft.getInstance();
    
    // 动画相关
    private static float currentSlotAnimation = 0.0f; 
    private static int lastSelectedSlot = 0; 
    
    // hotbar位置缓存（用于blur渲染）
    private static float lastHotbarX = 0;
    private static float lastHotbarY = 0;
    private static float lastHotbarWidth = 0;
    private static float lastHotbarHeight = 0;
    
    // 样式常量 - 参考原版hotbar尺寸
    private static final float HOTBAR_CORNER_RADIUS = 6.0f; 
    private static final int SLOT_SIZE = 20; 
    private static final int SLOT_SPACING = 0; 
    private static final int OFFHAND_SPACING = 8; 
    private static final int HOTBAR_PADDING_X = 4; 
    private static final int HOTBAR_PADDING_Y = 2; 
    private static final int SELECTED_BORDER_WIDTH = 1; 
    private static final int SELECTED_BORDER_COLOR = 0xFFFFFFFF;
    /**
     * @param guiGraphics 
     * @param partialTick 
     */
    public static void renderCustomHotbar(GuiGraphics guiGraphics, float partialTick) {
        LocalPlayer player = mc.player;
        if (player == null) return;
        
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        boolean hasOffhandItem = !player.getOffhandItem().isEmpty();
        int totalSlots = 9;
        int mainHotbarContentWidth = totalSlots * SLOT_SIZE + (totalSlots - 1) * SLOT_SPACING;
        
        int hotbarWidth;
        if (hasOffhandItem) {
            hotbarWidth = SLOT_SIZE + OFFHAND_SPACING + mainHotbarContentWidth + HOTBAR_PADDING_X * 2;
        } else {
            hotbarWidth = mainHotbarContentWidth + HOTBAR_PADDING_X * 2;
        }
        
        int hotbarHeight = SLOT_SIZE + HOTBAR_PADDING_Y * 2;
        int hotbarX = (screenWidth - hotbarWidth) / 2;
        int hotbarY = screenHeight - hotbarHeight;
        lastHotbarX = hotbarX;
        lastHotbarY = hotbarY;
        lastHotbarWidth = hotbarWidth;
        lastHotbarHeight = hotbarHeight;
    
        updateSlotAnimation(player.getInventory().selected);
 
        int offhandSlotX = hotbarX + HOTBAR_PADDING_X;
        int mainHotbarStartX = hasOffhandItem ? (offhandSlotX + SLOT_SIZE + OFFHAND_SPACING) : (hotbarX + HOTBAR_PADDING_X);
        
        renderSelectedSlotBorder(poseStack, mainHotbarStartX, hotbarY);
        
        if (hasOffhandItem) {
            renderOffhandItem(guiGraphics, player, offhandSlotX, hotbarY);
        }
        renderItems(guiGraphics, player, mainHotbarStartX, hotbarY);
        
        poseStack.popPose();
    }
    
    private static void updateSlotAnimation(int selectedSlot) {
        float targetPosition = selectedSlot;
        
        if (selectedSlot != lastSelectedSlot) {
            lastSelectedSlot = selectedSlot;
        }
        
        currentSlotAnimation = AnimationUtils.getAnimationState(
                currentSlotAnimation, 
                targetPosition, 
                20.0f 
        );
    }

    public static void onShader(EventShader e) {
        if (e.getType() == EventType.BLUR && shouldRenderCustomHotbar()) {
            
            RenderUtils.drawRoundedRect(e.getStack(), lastHotbarX, lastHotbarY, 
                    lastHotbarWidth, lastHotbarHeight, HOTBAR_CORNER_RADIUS, Integer.MIN_VALUE);
        }
    }
    
    private static void renderSelectedSlotBorder(PoseStack poseStack, int hotbarX, int hotbarY) {
        float slotX = hotbarX + currentSlotAnimation * (SLOT_SIZE + SLOT_SPACING);
        float slotY = hotbarY + HOTBAR_PADDING_Y;
        float borderX = slotX - SELECTED_BORDER_WIDTH / 2.0f;
        float borderY = slotY - SELECTED_BORDER_WIDTH / 2.0f;
        float borderWidth = SLOT_SIZE + SELECTED_BORDER_WIDTH;
        float borderHeight = SLOT_SIZE + SELECTED_BORDER_WIDTH;
        float cornerRadius = 4.0f;
        
        
        RenderUtils.drawRoundedRectCustom(poseStack, borderX, borderY, borderWidth, SELECTED_BORDER_WIDTH,
                cornerRadius, cornerRadius, 0, 0, SELECTED_BORDER_COLOR);
        

        RenderUtils.drawRoundedRectCustom(poseStack, borderX, borderY + borderHeight - SELECTED_BORDER_WIDTH, 
                borderWidth, SELECTED_BORDER_WIDTH,
                0, 0, cornerRadius, cornerRadius, SELECTED_BORDER_COLOR);
        
        
        RenderUtils.drawRoundedRect(poseStack, borderX, borderY + SELECTED_BORDER_WIDTH, 
                SELECTED_BORDER_WIDTH, borderHeight - SELECTED_BORDER_WIDTH * 2, 0, SELECTED_BORDER_COLOR);
        
        
        RenderUtils.drawRoundedRect(poseStack, borderX + borderWidth - SELECTED_BORDER_WIDTH, borderY + SELECTED_BORDER_WIDTH, 
                SELECTED_BORDER_WIDTH, borderHeight - SELECTED_BORDER_WIDTH * 2, 0, SELECTED_BORDER_COLOR);
    }
    private static void renderItems(GuiGraphics guiGraphics, Player player, int hotbarX, int hotbarY) {
        for (int i = 0; i < 9; i++) {
            int slotX = hotbarX + i * (SLOT_SIZE + SLOT_SPACING);
            int slotY = hotbarY + HOTBAR_PADDING_Y;
            
            renderSlot(guiGraphics, slotX, slotY, i, player);
        }
    }
    private static void renderSlot(GuiGraphics guiGraphics, int x, int y, int slot, Player player) {
        ItemStack itemStack = player.getInventory().items.get(slot);
        
        if (!itemStack.isEmpty()) {            
            guiGraphics.renderItem(itemStack, x + 2, y + 2);
            guiGraphics.renderItemDecorations(mc.font, itemStack, x + 2, y + 2);
        }
    }

    private static void renderOffhandItem(GuiGraphics guiGraphics, Player player, int x, int y) {
        ItemStack itemStack = player.getOffhandItem();
        int slotY = y + HOTBAR_PADDING_Y;
        if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, x + 2, slotY + 2);
            guiGraphics.renderItemDecorations(mc.font, itemStack, x + 2, slotY + 2);
        }
    }
    public static boolean shouldRenderCustomHotbar() {
        return mc.player != null && !mc.options.hideGui;
    }
}
