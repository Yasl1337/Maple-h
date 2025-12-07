package com.heypixel.heypixelmod.obsoverlay.ui.targethud;

import com.heypixel.heypixelmod.obsoverlay.modules.impl.render.HUD;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.StencilUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.HealthBarAnimator;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.HealthParticle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector4f;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TargetHUD {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final Map<UUID, HealthBarAnimator> healthAnimators = new HashMap<>();
    private static final Map<UUID, List<HealthParticle>> playerParticles = new HashMap<>();
    private static final Map<UUID, Float> lastHealth = new HashMap<>();
    private static final Random random = new Random();

    public static Vector4f render(GuiGraphics graphics, LivingEntity living, String style, float x, float y) {
        if ("Naven".equals(style)) {
            return renderNavenStyle(graphics, living, x, y);
        } else if ("New".equals(style)) {
            return renderNewStyle(graphics, living, x, y);
        } else if ("MoonLight".equals(style)) {
            return renderMoonLightV2Style(graphics, living, x, y);
        } else if ("Rise".equals(style)) {
            return renderRise(graphics, living, x, y);
        } else if ("Exhibition".equals(style)) {
            return renderExhibitionStyle(graphics, living, x, y);
        } else if ("Capsule".equals(style)) {
            return renderCapsuleStyle(graphics, living, x, y);
        }
        return null;
    }

    private static Vector4f renderCapsuleStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        String targetName = living.getName().getString();
        String hpText = "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "");

        float titleW = Fonts.harmony.getWidth(targetName, 0.35F);
        float hpW = Fonts.harmony.getWidth(hpText, 0.30F);
        float contentW = Math.max(titleW, hpW);

        float paddingH = 10.0F; 
        float paddingV = 10.0F;
        float hudWidth = Math.max(180.0F, contentW + paddingH * 2.0F);
        float hudHeight = 50.0F; 
        float nameX = x + paddingH;
        float nameY = y + paddingV;
        Fonts.harmony.render(graphics.pose(), targetName, (double) nameX, (double) nameY, Color.WHITE, true, 0.35F);

        float hpX = x + paddingH;
        float hpY = nameY + (float) Fonts.harmony.getHeight(true, 0.35F) + 4.0F;
        Fonts.harmony.render(graphics.pose(), hpText, (double) hpX, (double) hpY, new Color(220, 220, 220), true, 0.30F);
        float barWidthBase = hudWidth - paddingH * 2.0F;
        float barWidth = Math.max(60.0F, barWidthBase - 20.0F); 
        float barHeight = 5.0F;
        float barX = x + paddingH;
        float barY = y + hudHeight - paddingV - barHeight;

        float cornerRadius = 4.0F;
        RenderUtils.drawRoundedRect(graphics.pose(), barX, barY, barWidth, barHeight, cornerRadius, 0x80FFFFFF);
        float ratio = Math.min(1.0F, Math.max(0.0F, living.getHealth() / Math.max(1.0F, living.getMaxHealth())));
        float fillW = barWidth * ratio;
        if (fillW > 0) {

            int fillColor;
            if (ratio <= (1.0F / 3.0F)) {
                fillColor = 0xFFFF9999; 
            } else if (ratio <= 0.5F) {
                fillColor = 0xFFFFD28C; 
            } else {
                fillColor = 0xFFB6F2B6; 
            }

            float foregroundRadius = Math.min(cornerRadius, fillW / 2.0F);
            RenderUtils.drawRoundedRect(
                    graphics.pose(),
                    barX,
                    barY,
                    fillW,
                    barHeight,
                    foregroundRadius,
                    fillColor
            );
        }

        return new Vector4f(x, y, hudWidth, hudHeight);
    }

    private static Vector4f renderNavenStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float width = Math.max(Fonts.harmony.getWidth(targetName, 0.4F) + 10.0F, 60.0F);
        Vector4f blurMatrix = new Vector4f(x, y, width, 30.0F);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, width, 30.0F, 5.0F, HUD.headerColor);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, width, 30.0F, HUD.bodyColor);
        RenderUtils.fillBound(graphics.pose(), x, y, width * (living.getHealth() / living.getMaxHealth()), 3.0F, HUD.headerColor);
        StencilUtils.dispose();

        Fonts.harmony.render(graphics.pose(), targetName, (double)(x + 5.0F), (double)(y + 6.0F), Color.WHITE, true, 0.35F);
        Fonts.harmony.render(graphics.pose(), "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : ""), (double)(x + 5.0F), (double)(y + 17.0F), Color.WHITE, true, 0.35F);

        return blurMatrix;
    }

    private static Vector4f renderNewStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float hudWidth = 140.0F;
        float hudHeight = 50.0F;
        Vector4f blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, hudWidth, hudHeight, 8.0F, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, hudWidth, hudHeight, 0x80000000);
        StencilUtils.dispose();

        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float nameX = x + 10.0F;
        float nameY = y + 8.0F;
        Fonts.harmony.render(graphics.pose(), "Name: " + targetName, (double)nameX, (double)nameY, Color.WHITE, true, 0.30F);

        String healthText = "HP: " + Math.round(living.getHealth()) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "");
        float healthTextX = x + 10.0F;
        float healthTextY = y + 20.0F;
        Fonts.harmony.render(graphics.pose(), healthText, (double)healthTextX, (double)healthTextY, Color.WHITE, true, 0.30F);

        float healthBarWidth = 120.0F;
        float healthBarHeight = 6.0F;
        float healthBarX = x + 10.0F;
        float healthBarY = y + 36.0F;

        if (healthBarX + healthBarWidth > x + hudWidth) {
            healthBarWidth = hudWidth - 20.0F;
        }

        RenderUtils.drawRoundedRect(graphics.pose(), healthBarX, healthBarY, healthBarWidth, healthBarHeight, 4.0F, 0x80FFFFFF);

        float healthRatio = living.getHealth() / living.getMaxHealth();
        if (healthRatio > 1.0F) healthRatio = 1.0F;
        float currentHealthWidth = healthBarWidth * healthRatio;

        if (currentHealthWidth > 0) {
            RenderUtils.fillBound(graphics.pose(), healthBarX, healthBarY, currentHealthWidth, healthBarHeight, 0xFFFFFFFF);
        }

        return blurMatrix;
    }

    private static Vector4f renderMoonLightV2Style(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float mlHudWidth = 150.0F;
        float mlHudHeight = 35.0F;
        Vector4f blurMatrix = new Vector4f(x, y, mlHudWidth, mlHudHeight);

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 4.0F, 0x80000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, mlHudWidth, mlHudHeight, 0x80000000);
        StencilUtils.dispose();

        String mlTargetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float mlNameX = x + 8.0F;
        float mlNameY = y + 8.0F;
        Fonts.harmony.render(graphics.pose(), mlTargetName, (double) mlNameX, (double) mlNameY, Color.WHITE, true, 0.30F);

        String mlHealthText = Math.round(living.getHealth()) + "/" + Math.round(living.getMaxHealth());
        float mlHealthTextX = x + 8.0F;
        float mlHealthTextY = y + 20.0F;
        Fonts.harmony.render(graphics.pose(), mlHealthText, (double) mlHealthTextX, (double) mlHealthTextY, Color.WHITE, true, 0.30F);

        float mlCircleX = x + mlHudWidth - 20.0F;
        float mlCircleY = y + mlHudHeight / 2.0F;
        float mlCircleRadius = 10.0F;
        float mlHealthPercent = Math.min(1.0f, Math.max(0.0f, living.getHealth() / living.getMaxHealth()));

        RenderUtils.drawHealthRing(
                graphics.pose(),
                mlCircleX,
                mlCircleY,
                mlCircleRadius,
                2.5F,
                mlHealthPercent
        );

        return blurMatrix;
    }

    private static Vector4f renderRise(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float hudWidth = 160.0F;
        float hudHeight = 45.0F;
        float avatarSize = 32.0F;
        float padding = 4.0F;

        Vector4f blurMatrix = new Vector4f(x, y, hudWidth, hudHeight);


        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(graphics.pose(), x, y, hudWidth, hudHeight, 6.0F, 0x70000000);
        StencilUtils.erase(true);
        RenderUtils.fillBound(graphics.pose(), x, y, hudWidth, hudHeight, 0x70000000);
        StencilUtils.dispose();


        float currentTotalHealth = living.getHealth() + living.getAbsorptionAmount();
        float previousHealth = lastHealth.getOrDefault(living.getUUID(), currentTotalHealth);

        if (currentTotalHealth < previousHealth) {
            int particleCount = random.nextInt(6) + 8;
            float avatarX = x + padding;
            float avatarY = y + (hudHeight - avatarSize) / 2;

            List<HealthParticle> particles = playerParticles.computeIfAbsent(living.getUUID(), k -> new CopyOnWriteArrayList<>());
            for (int i = 0; i < particleCount; i++) {
                particles.add(new HealthParticle(avatarX + avatarSize / 2, avatarY + avatarSize / 2));
            }
        }

        lastHealth.put(living.getUUID(), currentTotalHealth);

        List<HealthParticle> particles = playerParticles.get(living.getUUID());
        if (particles != null) {
            for (HealthParticle particle : particles) {
                particle.update();
                particle.render(graphics);
            }
            particles.removeIf(HealthParticle::isDead);
        }


        float avatarX = x + padding;
        float avatarY = y + (hudHeight - avatarSize) / 2;
        RenderUtils.drawRoundedRect(graphics.pose(), avatarX, avatarY, avatarSize, avatarSize, 4.0F, Color.WHITE.getRGB());

        ResourceLocation skinLocation = null;
        if (living instanceof Player player) {
            PlayerInfo playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            if (playerInfo != null) {
                skinLocation = playerInfo.getSkinLocation();
            }
        }

        if (skinLocation != null) {
            if (living instanceof Player player) {
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 8, 8, 8, 8, 64, 64);
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 40, 8, 8, 8, 64, 64);
            } else {
                graphics.blit(skinLocation, (int) avatarX, (int) avatarY, (int) avatarSize, (int) avatarSize, 0, 0, 16, 16, 16, 16);
            }
        } else {

            String noneText = "NONE";
            float noneTextWidth = Fonts.harmony.getWidth(noneText, 0.30F);
            float noneTextHeight = (float) Fonts.harmony.getHeight(true, 0.30F);
            float noneTextX = avatarX + (avatarSize - noneTextWidth) / 2.0F;
            float noneTextY = avatarY + (avatarSize - noneTextHeight) / 2.0F;
            Fonts.harmony.render(graphics.pose(), noneText, (double) noneTextX, (double) noneTextY, Color.WHITE, true, 0.30F);
        }


        String targetName = living.getName().getString() + (living.isBaby() ? " (Baby)" : "");
        float textX = x + avatarSize + padding * 2;
        float textY = y + padding + 2;
        Fonts.harmony.render(graphics.pose(), "Name: " + targetName, (double) textX, (double) textY, Color.WHITE, true, 0.30F);

        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();
        float absorption = living.getAbsorptionAmount();

        HealthBarAnimator animator = healthAnimators.computeIfAbsent(living.getUUID(), k -> new HealthBarAnimator(health + absorption, 4.0F));
        animator.update(health + absorption);
        float animatedHealth = animator.getDisplayedHealth();

        String healthText = "HP: " + String.format("%.0f", animatedHealth) + " / " + String.format("%.0f", maxHealth);
        float healthTextY = (float) (textY + Fonts.harmony.getHeight(true, 0.30F) + 2.0F);
        Fonts.harmony.render(graphics.pose(), healthText, (double) textX, (double) healthTextY, Color.WHITE, true, 0.30F);


        float healthBarX = x + avatarSize + padding * 2;
        float healthBarY = y + hudHeight - padding - 8;
        float healthBarWidth = hudWidth - (healthBarX - x) - padding;
        float healthBarHeight = 6.0F;
        float cornerRadius = 4.0F;

        float healthRatio = animatedHealth / maxHealth;
        if (healthRatio > 1.0F) healthRatio = 1.0F;
        float currentHealthWidth = healthBarWidth * healthRatio;


        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);


        RenderUtils.drawRoundedRect(graphics.pose(), healthBarX, healthBarY, healthBarWidth, healthBarHeight, cornerRadius, 0x80404040);


        if (currentHealthWidth > 0) {

            float foregroundRadius = Math.min(cornerRadius, currentHealthWidth / 2);


            RenderUtils.drawRoundedRect(
                    graphics.pose(),
                    healthBarX,
                    healthBarY,
                    currentHealthWidth,
                    healthBarHeight,
                    foregroundRadius,
                    0xFF66CCFF
            );
        }
        return blurMatrix;
    }

    private static Vector4f renderExhibitionStyle(GuiGraphics graphics, LivingEntity living, float x, float y) {
        float hudWidth = 170.0F;
        float hudHeight = 60.0F;
        float avatarSize = 40.0F;
        float padding = 5.0F;


        RenderUtils.fill(graphics.pose(), x, y, x + hudWidth, y + hudHeight, new Color(50, 50, 50, 200).getRGB());
        RenderUtils.fill(graphics.pose(), x + 1, y + 1, x + hudWidth - 1, y + hudHeight - 1, new Color(50, 50, 50, 200).getRGB());
        RenderUtils.fill(graphics.pose(), x + 2, y + 2, x + hudWidth - 2, y + hudHeight - 2, new Color(50, 50, 50, 200).getRGB());

        RenderUtils.fill(graphics.pose(), x + 3, y + 3, x + hudWidth - 3, y + hudHeight - 3, new Color(25, 25, 25, 240).getRGB());


        float avatarX = x + padding + 3;
        float modelSize = avatarSize - 6.0F;
        float avatarY = y + (hudHeight - modelSize) / 2.0f;
                
        RenderUtils.fill(graphics.pose(), avatarX - 2, avatarY - 2, avatarX + modelSize + 2, avatarY + modelSize + 2, new Color(10, 10, 10).getRGB());
       
        RenderUtils.fill(graphics.pose(), avatarX - 2, avatarY - 4, avatarX + modelSize + 2, avatarY - 3, new Color(50, 50, 50).getRGB());
        RenderUtils.fill(graphics.pose(), avatarX - 2, avatarY + modelSize + 3, avatarX + modelSize + 2, avatarY + modelSize + 4, new Color(50, 60, 60).getRGB());
        RenderUtils.fill(graphics.pose(), avatarX - 2, avatarY - 3, avatarX - 1, avatarY + modelSize + 3, new Color(50, 50, 50).getRGB());
        RenderUtils.fill(graphics.pose(), avatarX + modelSize + 1, avatarY - 3, avatarX + modelSize + 2, avatarY + modelSize + 3, new Color(60, 60, 60).getRGB());
        drawPlayerModel(graphics, living, avatarX, avatarY, modelSize);


        float textX = x + avatarSize + padding * 2 + 2;
        float textY = y + padding + 2;


        String targetName = living.getName().getString();
        Fonts.harmony.render(graphics.pose(), targetName, (double) textX, (double) textY, Color.WHITE, true, 0.35F);


        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();
        float absorption = living.getAbsorptionAmount();

        float healthBarWidth = hudWidth - (avatarSize + padding * 3) - 4;
        float healthBarHeight = 5.0F;
        float healthBarX = textX;
        float healthBarY = textY + (float)Fonts.harmony.getHeight(true, 0.35F) + padding;

        int numSegments = 10;
        float gap = 1.0f;
        float totalGapWidth = gap * (numSegments - 1);
        float segmentWidth = (healthBarWidth - totalGapWidth) / numSegments;

        float healthPerSegment = maxHealth / numSegments;
        float currentHealthAmount = health + absorption;
        float healthRatioForColor = (health + absorption) / maxHealth;
        Color healthColor = getHealthColor(healthRatioForColor);


        for (int i = 0; i < numSegments; i++) {
            float segmentX = healthBarX + i * (segmentWidth + gap);


            RenderUtils.fill(graphics.pose(), segmentX, healthBarY, segmentX + segmentWidth, healthBarY + healthBarHeight, new Color(50, 50, 50).getRGB());

            if (currentHealthAmount > 0) {
                float fillWidth = segmentWidth;
                if (currentHealthAmount < healthPerSegment) {
                    fillWidth = segmentWidth * (currentHealthAmount / healthPerSegment);
                }

                RenderUtils.fill(graphics.pose(), segmentX, healthBarY, segmentX + fillWidth, healthBarY + healthBarHeight, healthColor.getRGB());

                currentHealthAmount -= healthPerSegment;
            }
        }


        float itemY = healthBarY + healthBarHeight + 5.0F;
        renderPlayerItems(graphics, living, textX, itemY);

        return new Vector4f(x, y, hudWidth, hudHeight);
    }

    private static void drawPlayerModel(GuiGraphics graphics, LivingEntity living, float x, float y, float size) {
        com.mojang.blaze3d.vertex.PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
       
        poseStack.translate(x + size / 2.0F, y + size / 2F + (size * 0.7f * living.getBbHeight()) / 2F, 100.0F);
        poseStack.scale(size * 0.7f, size * 0.7f, -size * 0.7f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
       
        poseStack.mulPose(Axis.YP.rotationDegrees(-30.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));


        EntityRenderDispatcher entityRenderDispatcher = mc.getEntityRenderDispatcher();
        entityRenderDispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() -> {
            entityRenderDispatcher.render(living, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, graphics.bufferSource(), 15728880);
        });
        graphics.flush();
        entityRenderDispatcher.setRenderShadow(true);
        poseStack.popPose();
    }

    private static void renderPlayerItems(GuiGraphics graphics, LivingEntity living, float x, float y) {
        if (living instanceof Player player) {
            float itemSize = 16.0F;
            float currentX = x;
            for (int i = 3; i >= 0; i--) {
                ItemStack armorStack = player.getInventory().getArmor(i);
                if (!armorStack.isEmpty()) {
                    graphics.renderItem(armorStack, (int) currentX, (int) y);
                    graphics.renderItemDecorations(mc.font, armorStack, (int) currentX, (int) y);
                    currentX += itemSize + 2.0F;
                }
            }

           
            ItemStack mainHandStack = player.getMainHandItem();
            if (!mainHandStack.isEmpty()) {
                graphics.renderItem(mainHandStack, (int) currentX, (int) y);
                graphics.renderItemDecorations(mc.font, mainHandStack, (int) currentX, (int) y);
            }
            
           
            ItemStack offHandStack = player.getOffhandItem();
            if (!offHandStack.isEmpty()) {
                graphics.renderItem(offHandStack, (int) (currentX + itemSize + 2.0F), (int) y);
                graphics.renderItemDecorations(mc.font, offHandStack, (int) (currentX + itemSize + 2.0F), (int) y);
            }
        }
    }

    private static Color getHealthColor(float healthRatio) {
        if (healthRatio > 0.6) {
            return new Color(0, 255, 0);
        } else if (healthRatio > 0.3) {
            return new Color(255, 255, 0);
        } else {
            return new Color(255, 0, 0);
        }
    }
}
