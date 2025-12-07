package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.HackerCheck;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.ui.notification.Notification;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
/// skid
@ModuleInfo(
        name = "NameTags",
        category = Category.RENDER,
        description = "Renders name tags"
)
public class NameTags extends Module {
    // 添加模式值以在不同样式之间切换
    public ModeValue style = ValueBuilder.create(this, "Style").setModes("Normal", "Capsule", "Stno").setDefaultModeIndex(0).build().getModeValue();
    // 添加圆角半径值
    public FloatValue cornerRadius = ValueBuilder.create(this, "Corner Radius")
            .setDefaultFloatValue(4.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    // 添加胶囊之间的间距值
    public FloatValue capsuleSpacing = ValueBuilder.create(this, "Capsule Spacing")
            .setDefaultFloatValue(2.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setVisibility(() -> this.style.getCurrentMode().equals("Capsule"))
            .build()
            .getFloatValue();
    // 添加不透明度值
    public FloatValue opacity = ValueBuilder.create(this, "Opacity")
            .setDefaultFloatValue(0.4F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();
    public BooleanValue mcf = ValueBuilder.create(this, "Middle Click Friend").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue showCompassPosition = ValueBuilder.create(this, "Compass Position").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue compassOnly = ValueBuilder.create(this, "Compass Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue noPlayerOnly = ValueBuilder.create(this, "No Player Only")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.showCompassPosition.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue shared = ValueBuilder.create(this, "Shared ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    public FloatValue scale = ValueBuilder.create(this, "Scale")
            .setDefaultFloatValue(0.3F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(0.5F)
            .build()
            .getFloatValue();
    private final Map<Entity, Vector2f> entityPositions = new ConcurrentHashMap<>();
    private final List<NameTags.NameTagData> sharedPositions = new CopyOnWriteArrayList<>();
    List<Vector4f> blurMatrices = new ArrayList<>();
    private final Map<Entity, StnoRenderData> stnoEntityRenderData = new ConcurrentHashMap<>();
    private BlockPos spawnPosition;
    private Vector2f compassPosition;
    private final Map<Player, Integer> aimTicks = new ConcurrentHashMap<>();
    private Player aimingPlayer;

    private boolean hasPlayer() {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity != mc.player && !(entity instanceof BlinkingPlayer) && entity instanceof Player) {
                return true;
            }
        }

        return false;
    }

    private BlockPos getSpawnPosition(ClientLevel p_117922_) {
        return p_117922_.dimensionType().natural() ? p_117922_.getSharedSpawnPos() : null;
    }

    @EventTarget
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            if (!this.mcf.getCurrentValue()) {
                this.aimingPlayer = null;
            } else {
                for (Player player : mc.level.players()) {
                    if (!(player instanceof BlinkingPlayer) && player != mc.player) {
                        if (isAiming(player, mc.player.getYRot(), mc.player.getXRot())) {
                            if (this.aimTicks.containsKey(player)) {
                                this.aimTicks.put(player, this.aimTicks.get(player) + 1);
                            } else {
                                this.aimTicks.put(player, 1);
                            }

                            if (this.aimTicks.get(player) >= 10) {
                                this.aimingPlayer = player;
                                break;
                            }
                        } else if (this.aimTicks.containsKey(player) && this.aimTicks.get(player) > 0) {
                            this.aimTicks.put(player, this.aimTicks.get(player) - 1);
                        } else {
                            this.aimTicks.put(player, 0);
                        }
                    }
                }

                if (this.aimingPlayer != null && this.aimTicks.containsKey(this.aimingPlayer) && this.aimTicks.get(this.aimingPlayer) <= 0) {
                    this.aimingPlayer = null;
                }
            }

            this.spawnPosition = null;
            if (!InventoryUtils.hasItem(Items.COMPASS) && this.compassOnly.getCurrentValue()) {
                return;
            }

            if (this.hasPlayer() && this.noPlayerOnly.getCurrentValue()) {
                return;
            }

            this.spawnPosition = this.getSpawnPosition(mc.level);
        }
    }

    public static boolean isAiming(Entity targetEntity, float yaw, float pitch) {
        Vec3 playerEye = new Vec3(mc.player.getX(), mc.player.getY() + (double)mc.player.getEyeHeight(), mc.player.getZ());
        HitResult intercept = RotationUtils.getIntercept(targetEntity.getBoundingBox(), new Vector2f(yaw, pitch), playerEye, 150.0);
        if (intercept == null) {
            return false;
        } else {
            return intercept.getType() != Type.ENTITY ? false : intercept.getLocation().distanceTo(playerEye) < 150.0;
        }
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (e.getType() != EventType.BLUR || !this.isEnabled()) return;
        
        // 统一使用 blurMatrices 渲染模糊效果
        for (Vector4f blurMatrix : this.blurMatrices) {
            float x = blurMatrix.x();
            float y = blurMatrix.y();
            float width = blurMatrix.z() - x;
            float height = blurMatrix.w() - y;
            RenderUtils.drawRoundedRect(e.getStack(), x, y, width, height, this.cornerRadius.getCurrentValue(), 1073741824);
        }
    }

    @EventTarget
    public void update(EventRender e) {
        try {
            this.updatePositions(e.getRenderPartialTicks());
            this.compassPosition = null;
            if (this.spawnPosition != null) {
                this.compassPosition = ProjectionUtils.project(
                        (double)this.spawnPosition.getX() + 0.5,
                        (double)this.spawnPosition.getY() + 1.75,
                        (double)this.spawnPosition.getZ() + 0.5,
                        e.getRenderPartialTicks()
                );
            }
            
            // Stno 模式的数据更新
            if (this.style.getCurrentMode().equals("Stno")) {
                stnoEntityRenderData.clear();
                if (mc.level == null) return;
                
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof Player player && entity != mc.player && !entity.isRemoved()) {
                        double x = MathUtils.interpolate(e.getRenderPartialTicks(), entity.xo, entity.getX());
                        double y = MathUtils.interpolate(e.getRenderPartialTicks(), entity.yo, entity.getY()) + entity.getBbHeight() + 0.5;
                        double z = MathUtils.interpolate(e.getRenderPartialTicks(), entity.zo, entity.getZ());
                        Vector2f projectedPos = ProjectionUtils.project(x, y, z, e.getRenderPartialTicks());
                        
                        if (projectedPos != null) {
                            StnoRenderData data = new StnoRenderData(projectedPos);
                            data.calculateDimensions(player);
                            stnoEntityRenderData.put(entity, data);
                        }
                    }
                }
            }
        } catch (Exception var3) {
        }
    }

    @EventTarget
    public void onMouseKey(EventMouseClick e) {
        if (e.getKey() == 2 && !e.isState() && this.mcf.getCurrentValue() && this.aimingPlayer != null) {
            if (FriendManager.isFriend(this.aimingPlayer)) {
                Notification notification = new Notification("Removed " + this.aimingPlayer.getName().getString() + " from friends!", false);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                FriendManager.removeFriend(this.aimingPlayer);
            } else {
                Notification notification = new Notification("Added " + this.aimingPlayer.getName().getString() + " as friends!", true);
                Naven.getInstance().getNotificationManager().addNotification(notification);
                FriendManager.addFriend(this.aimingPlayer);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrices.clear();
        int color1 = new Color(0, 0, 0, (int) (this.opacity.getCurrentValue() * 100)).getRGB();
        int color2 = new Color(0, 0, 0, (int) (this.opacity.getCurrentValue() * 200)).getRGB();
        if (this.compassPosition != null) {
            Vector2f position = this.compassPosition;
            float scale = Math.max(
                    80.0F
                            - Mth.sqrt(
                            (float)mc.player
                                    .distanceToSqr(
                                            (double)this.spawnPosition.getX() + 0.5, (double)this.spawnPosition.getY() + 1.75, (double)this.spawnPosition.getZ() + 0.5
                                    )
                    ),
                    0.0F
            )
                    * this.scale.getCurrentValue()
                    / 80.0F;
            String text = "Compass";
            float width = Fonts.harmony.getWidth(text, (double)scale);
            double height = Fonts.harmony.getHeight(true, (double)scale);
            this.blurMatrices
                    .add(new Vector4f(position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height + 2.0F)));
            StencilUtils.write(false);
            RenderUtils.drawRoundedRect(
                    e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, width + 4.0F, (float) (height + 2.0F), this.cornerRadius.getCurrentValue(), -1
            );
            StencilUtils.erase(true);
            RenderUtils.drawRoundedRect(
                    e.getStack(), position.x - width / 2.0F - 2.0F, position.y - 2.0F, width + 4.0F, (float) (height + 2.0F), this.cornerRadius.getCurrentValue(), color1
            );
            StencilUtils.dispose();
            Fonts.harmony.setAlpha(0.8F);
            Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
        }

        for (Entry<Entity, Vector2f> entry : this.entityPositions.entrySet()) {
            if (entry.getKey() != mc.player && entry.getKey() instanceof Player) {
                Player living = (Player)entry.getKey();
                e.getStack().pushPose();
                float hp = living.getHealth();
                if (hp > 20.0F) {
                    living.setHealth(20.0F);
                }

                Vector2f position = entry.getValue();
                // 根据所选样式渲染名称标签
                if (this.style.getCurrentMode().equals("Normal")) {
                    String text = "";
                    Module hackerCheckModule = Naven.getInstance().getModuleManager().getModule(HackerCheck.class);
                    if (hackerCheckModule != null && hackerCheckModule.isEnabled() && HackerCheck.isHacker(living)) {
                        text += "§c[Hacker]§f | ";
                    }

                    if (Teams.isSameTeam(living)) {
                        text = text + "§aTeam§f | ";
                    }

                    if (FriendManager.isFriend(living)) {
                        text = text + "§aFriend§f | ";
                    }

                    if (this.aimingPlayer == living) {
                        text = text + "§cAiming§f | ";
                    }

                    text = text + living.getName().getString();
                    text = text + "§f | §c" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";
                    float scale = this.scale.getCurrentValue();
                    float width = Fonts.harmony.getWidth(text, (double)scale);
                    float delta = 1.0F - living.getHealth() / living.getMaxHealth();
                    double height = Fonts.harmony.getHeight(true, (double)scale);
                    this.blurMatrices
                            .add(new Vector4f(position.x - width / 2.0F - 2.0F, position.y - 2.0F, position.x + width / 2.0F + 2.0F, (float)((double)position.y + height + 2.0F)));
                    RenderUtils.drawRoundedRect(
                            e.getStack(),
                            position.x - width / 2.0F - 2.0F,
                            position.y - 2.0F,
                            width + 4.0F,
                            (float) (height + 2.0F),
                            this.cornerRadius.getCurrentValue(),
                            color1
                    );
                    RenderUtils.drawRoundedRect(
                            e.getStack(),
                            position.x - width / 2.0F - 2.0F,
                            position.y - 2.0F,
                            (width + 4.0F) * (1.0F - delta),
                            (float) (height + 2.0F),
                            this.cornerRadius.getCurrentValue(),
                            color2
                    );
                    Fonts.harmony.setAlpha(0.8F);
                    Fonts.harmony.render(e.getStack(), text, (double)(position.x - width / 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
                    Fonts.harmony.setAlpha(1.0F);
                } else if (this.style.getCurrentMode().equals("Capsule")) {
                    // 新的胶囊渲染逻辑
                    float scale = this.scale.getCurrentValue();
                    double height = Fonts.harmony.getHeight(true, (double)scale);
                    float spacing = this.capsuleSpacing.getCurrentValue();
                    
                    // 准备所有胶囊数据
                    List<CapsuleData> capsules = new ArrayList<>();
                    
                    // 0. Team状态（如果是队友）
                    if (Teams.isSameTeam(living)) {
                        String teamText = "§aTeam";
                        float teamWidth = Fonts.harmony.getWidth(teamText, (double)scale);
                        capsules.add(new CapsuleData(teamText, teamWidth));
                    }
                    
                    // 1. Friend状态（如果是好友）
                    if (FriendManager.isFriend(living)) {
                        String friendText = "§aFriend";
                        float friendWidth = Fonts.harmony.getWidth(friendText, (double)scale);
                        capsules.add(new CapsuleData(friendText, friendWidth));
                    }
                    
                    // 2. 血量
                    String healthText = "§a" + Math.round(hp) + (living.getAbsorptionAmount() > 0.0F ? "+" + Math.round(living.getAbsorptionAmount()) : "") + "HP";
                    float healthWidth = Fonts.harmony.getWidth(healthText, (double)scale);
                    capsules.add(new CapsuleData(healthText, healthWidth));
                    
                    // 3. 名字（带截断，固定最大宽度80）
                    String originalName = living.getName().getString();
                    String nameToRender = originalName;
                    float maxNameWidth = 80.0F;
                    // 当名字宽度超过最大宽度时进行截断
                    if (Fonts.harmony.getWidth("§c" + nameToRender, (double)scale) > maxNameWidth) {
                        while (nameToRender.length() > 0 && Fonts.harmony.getWidth("§c" + nameToRender + "...", (double)scale) > maxNameWidth) {
                            nameToRender = nameToRender.substring(0, nameToRender.length() - 1);
                        }
                        nameToRender += "...";
                    }
                    String nameText = "§c" + nameToRender;
                    // 重新计算实际宽度，确保不超过最大宽度
                    float nameMeasuredWidth = Fonts.harmony.getWidth(nameText, (double)scale);
                    float nameWidthClamped = Math.min(nameMeasuredWidth, maxNameWidth);
                    capsules.add(new CapsuleData(nameText, nameWidthClamped));
                    
                    // 4. 距离
                    float distance = mc.player.distanceTo(living);
                    String distanceText = "§7" + String.format("%.1f", distance) + "m";
                    float distanceWidth = Fonts.harmony.getWidth(distanceText, (double)scale);
                    capsules.add(new CapsuleData(distanceText, distanceWidth));
                    
                    // 计算总宽度
                    float totalWidth = capsules.stream()
                            .map(c -> c.width + 4.0F)
                            .reduce(0.0F, Float::sum)
                            + spacing * (capsules.size() - 1);
                    
                    // 从左到右渲染所有胶囊
                    float startX = position.x - totalWidth / 2.0F;
                    float currentX = startX;
                    
                    Fonts.harmony.setAlpha(0.8F);
                    for (CapsuleData capsule : capsules) {
                        float capsuleWidth = capsule.width + 4.0F;
                        float capsuleEndX = currentX + capsuleWidth;
                        
                        
                        this.blurMatrices.add(new Vector4f(currentX, position.y - 2.0F, capsuleEndX, (float)(position.y + height)));
                        
                        // 使用模板裁剪，防止文本溢出到下一个胶囊
                        StencilUtils.write(false);
                        RenderUtils.drawRoundedRect(e.getStack(), currentX, position.y - 2.0F, capsuleWidth, (float)(height + 2.0F), this.cornerRadius.getCurrentValue(), -1);
                        StencilUtils.erase(true);
                        RenderUtils.drawRoundedRect(e.getStack(), currentX, position.y - 2.0F, capsuleWidth, (float)(height + 2.0F), this.cornerRadius.getCurrentValue(), color1);
                        Fonts.harmony.render(e.getStack(), capsule.text, (double)(currentX + 2.0F), (double)(position.y - 1.0F), Color.WHITE, true, (double)scale);
                        StencilUtils.dispose();
                        
                        currentX = capsuleEndX + spacing;
                    }
                    Fonts.harmony.setAlpha(1.0F);
                } else if (this.style.getCurrentMode().equals("Stno")) {
                    if (stnoEntityRenderData.containsKey(living)) {
                        StnoRenderData data = stnoEntityRenderData.get(living);
                        if (data.isValid()) {
                            e.getStack().pushPose();
                            
                            // 添加整体的blur背景到blurMatrices
                            this.blurMatrices.add(new Vector4f(data.startX, data.startY, data.startX + data.totalWidth, data.startY + data.totalHeight));
                            
                            float currentX = data.startX;
                            float startY = data.startY;
                            float scale = this.scale.getCurrentValue();
                            Color backgroundColor = new Color(20, 20, 20, 120);
                            
                            if (!data.prefix.isEmpty()) {
                                RenderUtils.drawRoundedRect(e.getStack(), currentX, startY, data.prefixWidth, data.totalHeight, this.cornerRadius.getCurrentValue(), backgroundColor.getRGB());
                                Fonts.harmony.render(e.getStack(), data.prefix, currentX + 6.0F, startY + 2.5F, data.prefixColor, true, scale);
                                currentX += data.prefixWidth + 2.0F;
                            }
                            
                            RenderUtils.drawRoundedRect(e.getStack(), currentX, startY, data.healthWidth, data.totalHeight, this.cornerRadius.getCurrentValue(), backgroundColor.getRGB());
                            Fonts.harmony.render(e.getStack(), data.healthText, currentX + 6.0F, startY + 2.5F, data.healthColor, true, scale);
                            currentX += data.healthWidth + 2.0F;
                            
                            RenderUtils.drawRoundedRect(e.getStack(), currentX, startY, data.nameWidth, data.totalHeight, this.cornerRadius.getCurrentValue(), backgroundColor.getRGB());
                            Fonts.harmony.render(e.getStack(), data.nameText, currentX + 6.0F, startY + 2.5F, Color.WHITE, true, scale);
                            currentX += data.nameWidth + 2.0F;
            
                            RenderUtils.drawRoundedRect(e.getStack(), currentX, startY, data.pingWidth, data.totalHeight, this.cornerRadius.getCurrentValue(), backgroundColor.getRGB());
                            Fonts.harmony.render(e.getStack(), data.pingText, currentX + 6.0F, startY + 2.5F, data.pingColor, true, scale);
                            
                            e.getStack().popPose();
                        }
                    }
                }

                e.getStack().popPose();
            }
        }

        if (this.shared.getCurrentValue()) {
            for (NameTags.NameTagData data : this.sharedPositions) {
                e.getStack().pushPose();
                Vector2f positionx = data.getRender();
                String textx = "§aShared§f | " + data.getDisplayName();
                float scale = this.scale.getCurrentValue();
                float width = Fonts.harmony.getWidth(textx, (double)scale);
                double delta = 1.0 - data.getHealth() / data.getMaxHealth();
                double height = Fonts.harmony.getHeight(true, (double)scale);
                this.blurMatrices
                        .add(
                                new Vector4f(positionx.x - width / 2.0F - 2.0F, positionx.y - 2.0F, positionx.x + width / 2.0F + 2.0F, (float)((double)positionx.y + height + 2.0F))
                        );
                RenderUtils.drawRoundedRect(
                        e.getStack(),
                        positionx.x - width / 2.0F - 2.0F,
                        positionx.y - 2.0F,
                        width + 4.0F,
                        (float) (height + 2.0F),
                        this.cornerRadius.getCurrentValue(),
                        color1
                );
                RenderUtils.drawRoundedRect(
                        e.getStack(),
                        positionx.x - width / 2.0F - 2.0F,
                        positionx.y - 2.0F,
                        (float)((double)(width + 4.0F) * (1.0 - delta)),
                        (float) (height + 2.0F),
                        this.cornerRadius.getCurrentValue(),
                        color2
                );
                Fonts.harmony.setAlpha(0.8F);
                Fonts.harmony.render(e.getStack(), textx, (double)(positionx.x - width / 2.0F), (double)(positionx.y - 1.0F), Color.WHITE, true, (double)scale);
                Fonts.harmony.setAlpha(1.0F);
                e.getStack().popPose();
            }
        }
    }

    private void updatePositions(float renderPartialTicks) {
        this.entityPositions.clear();
        this.sharedPositions.clear();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player && !entity.getName().getString().startsWith("CIT-")) {
                double x = MathUtils.interpolate(renderPartialTicks, entity.xo, entity.getX());
                double y = MathUtils.interpolate(renderPartialTicks, entity.yo, entity.getY()) + (double)entity.getBbHeight() + 0.5;
                double z = MathUtils.interpolate(renderPartialTicks, entity.zo, entity.getZ());
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                vector.setY(vector.getY() - 2.0F);
                this.entityPositions.put(entity, vector);
            }
        }

        if (this.shared.getCurrentValue()) {
            Map<String, SharedESPData> dataMap = EntityWatcher.getSharedESPData();

            for (SharedESPData value : dataMap.values()) {
                double x = value.getPosX();
                double y = value.getPosY() + (double)mc.player.getBbHeight() + 0.5;
                double z = value.getPosZ();
                Vector2f vector = ProjectionUtils.project(x, y, z, renderPartialTicks);
                vector.setY(vector.getY() - 2.0F);
                String displayName = value.getDisplayName();
                displayName = displayName
                        + "§f | §c"
                        + Math.round(value.getHealth())
                        + (value.getAbsorption() > 0.0 ? "+" + Math.round(value.getAbsorption()) : "")
                        + "HP";
                this.sharedPositions
                        .add(new NameTags.NameTagData(displayName, value.getHealth(), value.getMaxHealth(), value.getAbsorption(), new Vec3(x, y, z), vector));
            }
        }
    }

    private static class CapsuleData {
        private final String text;
        private final float width;

        public CapsuleData(String text, float width) {
            this.text = text;
            this.width = width;
        }
    }
    
    private class StnoRenderData {
        private static final float PADDING = 6.0F;
        private static final float VERTICAL_PADDING = 2.5F;
        private static final float GAP = 2.0F;
        
        private final Vector2f projectedPos;
        float totalWidth = 0, totalHeight = 0;
        float startX = 0, startY = 0;
        float prefixWidth = 0, healthWidth = 0, nameWidth = 0, pingWidth = 0;
        String prefix, healthText, nameText, pingText;
        Color healthColor, pingColor, prefixColor;
        
        StnoRenderData(Vector2f projectedPos) {
            this.projectedPos = projectedPos;
        }
        
        void calculateDimensions(Player player) {
            this.prefix = getStnoPrefix(player);
            switch (this.prefix) {
                case "Hacker": this.prefixColor = Color.RED; break;
                case "Team": this.prefixColor = Color.GREEN; break;
                case "Friend": this.prefixColor = new Color(0, 255, 255); break;
                default: this.prefixColor = Color.WHITE; break;
            }
            
            float health = player.getHealth() + player.getAbsorptionAmount();
            this.healthText = String.format("%.1f", health);
            this.healthColor = getStnoHealthColor(player.getHealth());
            
            net.minecraft.client.multiplayer.PlayerInfo playerInfo = null;
            if (mc.getConnection() != null) {
                playerInfo = mc.getConnection().getPlayerInfo(player.getUUID());
            }
            int ping = 0;
            
            if (playerInfo != null) {
                this.nameText = playerInfo.getTabListDisplayName() != null ? playerInfo.getTabListDisplayName().getString() : playerInfo.getProfile().getName();
                ping = playerInfo.getLatency();
            } else {
                this.nameText = player.getName().getString();
            }
            this.pingText = ping + "ms";
            this.pingColor = getStnoPingColor(ping);
            
            float scaleValue = scale.getCurrentValue();
            float fontHeight = (float) Fonts.harmony.getHeight(true, scaleValue);
            this.totalHeight = fontHeight + VERTICAL_PADDING * 2;
            
            this.prefixWidth = prefix.isEmpty() ? 0 : (Fonts.harmony.getWidth(prefix, scaleValue) + PADDING * 2);
            this.healthWidth = Fonts.harmony.getWidth(healthText, scaleValue) + PADDING * 2;
            this.nameWidth = Fonts.harmony.getWidth(nameText, scaleValue) + PADDING * 2;
            this.pingWidth = Fonts.harmony.getWidth(pingText, scaleValue) + PADDING * 2;
            
            this.totalWidth = healthWidth + nameWidth + pingWidth + (GAP * 2);
            if (!prefix.isEmpty()) {
                this.totalWidth += prefixWidth + GAP;
            }
            
            this.startX = projectedPos.x - totalWidth / 2.0f;
            this.startY = projectedPos.y;
        }
        
        boolean isValid() {
            return this.projectedPos != null && this.totalWidth > 0;
        }
    }
    
    private String getStnoPrefix(Player player) {
        Module hackerCheckModule = Naven.getInstance().getModuleManager().getModule(HackerCheck.class);
        if (hackerCheckModule != null && hackerCheckModule.isEnabled() && HackerCheck.isHacker(player)) return "Hacker";
        if (Teams.isSameTeam(player)) return "Team";
        if (FriendManager.isFriend(player)) return "Friend";
        return "";
    }
    
    private Color getStnoHealthColor(float health) {
        if (health >= 16) return new Color(0, 255, 0);
        if (health >= 6) return new Color(255, 255, 0);
        return new Color(255, 0, 0);
    }
    
    private Color getStnoPingColor(int ping) {
        if (ping <= 70) return new Color(0, 255, 0);
        if (ping <= 150) return new Color(255, 255, 0);
        if (ping <= 250) return new Color(255, 165, 0);
        return new Color(255, 0, 0);
    }

    private static class NameTagData {
        private final String displayName;
        private final double health;
        private final double maxHealth;
        private final double absorption;
        private final Vec3 position;
        private final Vector2f render;

        public String getDisplayName() {
            return this.displayName;
        }

        public double getHealth() {
            return this.health;
        }

        public double getMaxHealth() {
            return this.maxHealth;
        }

        public double getAbsorption() {
            return this.absorption;
        }

        public Vec3 getPosition() {
            return this.position;
        }

        public Vector2f getRender() {
            return this.render;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (!(o instanceof NameTags.NameTagData other)) {
                return false;
            } else if (!other.canEqual(this)) {
                return false;
            } else if (Double.compare(this.getHealth(), other.getHealth()) != 0) {
                return false;
            } else if (Double.compare(this.getMaxHealth(), other.getMaxHealth()) != 0) {
                return false;
            } else if (Double.compare(this.getAbsorption(), other.getAbsorption()) != 0) {
                return false;
            } else {
                Object this$displayName = this.getDisplayName();
                Object other$displayName = other.getDisplayName();
                if (this$displayName == null ? other$displayName == null : this$displayName.equals(other$displayName)) {
                    Object this$position = this.getPosition();
                    Object other$position = other.getPosition();
                    if (this$position == null ? other$position == null : this$position.equals(other$position)) {
                        Object this$render = this.getRender();
                        Object other$render = other.getRender();
                        return this$render == null ? other$render == null : this$render.equals(other$render);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }

        protected boolean canEqual(Object other) {
            return other instanceof NameTags.NameTagData;
        }

        @Override
        public int hashCode() {
            int PRIME = 59;
            int result = 1;
            long $health = Double.doubleToLongBits(this.getHealth());
            result = result * 59 + (int)($health >>> 32 ^ $health);
            long $maxHealth = Double.doubleToLongBits(this.getMaxHealth());
            result = result * 59 + (int)($maxHealth >>> 32 ^ $maxHealth);
            long $absorption = Double.doubleToLongBits(this.getAbsorption());
            result = result * 59 + (int)($absorption >>> 32 ^ $absorption);
            Object $displayName = this.getDisplayName();
            result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
            Object $position = this.getPosition();
            result = result * 59 + ($position == null ? 43 : $position.hashCode());
            Object $render = this.getRender();
            return result * 59 + ($render == null ? 43 : $render.hashCode());
        }

        @Override
        public String toString() {
            return "NameTags.NameTagData(displayName="
                    + this.getDisplayName()
                    + ", health="
                    + this.getHealth()
                    + ", maxHealth="
                    + this.getMaxHealth()
                    + ", absorption="
                    + this.getAbsorption()
                    + ", position="
                    + this.getPosition()
                    + ", render="
                    + this.getRender()
                    + ")";
        }

        public NameTagData(String displayName, double health, double maxHealth, double absorption, Vec3 position, Vector2f render) {
            this.displayName = displayName;
            this.health = health;
            this.maxHealth = maxHealth;
            this.absorption = absorption;
            this.position = position;
            this.render = render;
        }
    }
}