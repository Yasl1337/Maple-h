package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventPacket;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRespawn;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ChestStealer;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.ContainerStealer;
import com.heypixel.heypixelmod.obsoverlay.utils.BlockUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.ChunkUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.RenderUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@ModuleInfo(
        name = "ChestESP",
        description = "Highlights chests and other storage blocks",
        category = Category.RENDER
)
public class ChestESP extends Module {
    private static final float[] chestColor = new float[]{1.0F, 0.89F, 0.0F}; // 黄色 - 普通箱子
    private static final float[] trappedChestColor = new float[]{1.0F, 0.36F, 0.34F}; // 红色 - 陷阱箱
    private static final float[] furnaceColor = new float[]{0.39F, 0.39F, 0.39F}; // 灰色 - 熔炉
    private static final float[] brewingStandColor = new float[]{1.0F, 0.65F, 0.0F}; // 橙色 - 酿造台
    private static final float[] stolenColor = new float[]{1.0F, 0.36F, 0.34F}; // 红色 - 已被偷取

    private final List<BlockPos> openedChests = new CopyOnWriteArrayList<>();
    private final List<AABB> renderBoundingBoxes = new CopyOnWriteArrayList<>();
    private final Map<BlockPos, StorageType> storageTypes = new HashMap<>();
    private final Map<BlockPos, Long> stealingAnimations = new HashMap<>();

    // 颜色设置
    public BooleanValue useCustomChestColor = ValueBuilder.create(this, "Use Custom Chest Color")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue chestRed = ValueBuilder.create(this, "Chest Red")
            .setDefaultFloatValue(255F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue chestGreen = ValueBuilder.create(this, "Chest Green")
            .setDefaultFloatValue(227F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue chestBlue = ValueBuilder.create(this, "Chest Blue")
            .setDefaultFloatValue(0F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public BooleanValue useCustomOpenedChestColor = ValueBuilder.create(this, "Use Custom Opened Chest Color")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public FloatValue openedChestRed = ValueBuilder.create(this, "Opened Chest Red")
            .setDefaultFloatValue(255F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue openedChestGreen = ValueBuilder.create(this, "Opened Chest Green")
            .setDefaultFloatValue(91F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public FloatValue openedChestBlue = ValueBuilder.create(this, "Opened Chest Blue")
            .setDefaultFloatValue(86F)
            .setFloatStep(5F)
            .setMinFloatValue(0F)
            .setMaxFloatValue(255F)
            .build()
            .getFloatValue();

    public BooleanValue showFurnaces = ValueBuilder.create(this, "Show Furnaces")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public BooleanValue showBrewingStands = ValueBuilder.create(this, "Show Brewing Stands")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public FloatValue opacity = ValueBuilder.create(this, "Opacity")
            .setDefaultFloatValue(0.8F)
            .setFloatStep(0.05F)
            .setMinFloatValue(0.1F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    public BooleanValue stealingAnimation = ValueBuilder.create(this, "Stealing Animation")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    @Override
    public void onDisable() {
        openedChests.clear();
        renderBoundingBoxes.clear();
        storageTypes.clear();
        stealingAnimations.clear();
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        this.openedChests.clear();
        this.stealingAnimations.clear();
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getType() == EventType.RECEIVE && e.getPacket() instanceof ClientboundBlockEventPacket) {
            ClientboundBlockEventPacket packet = (ClientboundBlockEventPacket)e.getPacket();
            if ((packet.getBlock() == Blocks.CHEST || packet.getBlock() == Blocks.TRAPPED_CHEST) && packet.getB0() == 1 && packet.getB1() == 1) {
                this.openedChests.add(packet.getPos());
            }
        }
    }

    @EventTarget
    public void onTick(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            // 修复：正确的收集方式
            ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities()
                    .collect(Collectors.toCollection(ArrayList::new));
            this.renderBoundingBoxes.clear();
            this.storageTypes.clear();

            // 更新偷取动画 -- 有屁用
            //updateStealingAnimations();

            for (BlockEntity blockEntity : blockEntities) {
                if (blockEntity instanceof ChestBlockEntity) {
                    ChestBlockEntity chestBE = (ChestBlockEntity)blockEntity;
                    AABB box = this.getChestBox(chestBE);
                    if (box != null) {
                        this.renderBoundingBoxes.add(box);
                        storageTypes.put(chestBE.getBlockPos(), StorageType.CHEST);
                    }
                } else if (showFurnaces.getCurrentValue() && blockEntity instanceof FurnaceBlockEntity) {
                    AABB box = BlockUtils.getBoundingBox(blockEntity.getBlockPos());
                    if (box != null) {
                        this.renderBoundingBoxes.add(box);
                        storageTypes.put(blockEntity.getBlockPos(), StorageType.FURNACE);
                    }
                } else if (showBrewingStands.getCurrentValue() && blockEntity instanceof BrewingStandBlockEntity) {
                    AABB box = BlockUtils.getBoundingBox(blockEntity.getBlockPos());
                    if (box != null) {
                        this.renderBoundingBoxes.add(box);
                        storageTypes.put(blockEntity.getBlockPos(), StorageType.BREWING_STAND);
                    }
                }
            }
        }
    }

    private void updateStealingAnimations() {
        // 检查当前正在偷取的箱子 - 使用正确的模块管理器引用
        ChestStealer stealer = (ChestStealer) Naven.getInstance().getModuleManager().getModule(ChestStealer.class);
        if (stealer != null && stealer.isEnabled()) {
            BlockPos currentChest = stealer.getCurrentChest();
            if (currentChest != null && !stealingAnimations.containsKey(currentChest)) {
                stealingAnimations.put(currentChest, System.currentTimeMillis());
            }
        }

        // 移除过期的动画（3秒后）
        long currentTime = System.currentTimeMillis();
        stealingAnimations.entrySet().removeIf(entry -> currentTime - entry.getValue() > 3000);
    }

    private AABB getChestBox(ChestBlockEntity chestBE) {
        BlockState state = chestBE.getBlockState();
        if (!state.hasProperty(ChestBlock.TYPE)) {
            return null;
        } else {
            ChestType chestType = (ChestType)state.getValue(ChestBlock.TYPE);
            if (chestType == ChestType.LEFT) {
                return null;
            } else {
                BlockPos pos = chestBE.getBlockPos();
                AABB box = BlockUtils.getBoundingBox(pos);
                if (chestType != ChestType.SINGLE) {
                    BlockPos pos2 = pos.relative(ChestBlock.getConnectedDirection(state));
                    if (BlockUtils.canBeClicked(pos2)) {
                        AABB box2 = BlockUtils.getBoundingBox(pos2);
                        box = box.minmax(box2);
                    }
                }
                return box;
            }
        }
    }

    @EventTarget
    public void onRender(EventRender e) {
        PoseStack stack = e.getPMatrixStack();
        stack.pushPose();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        Tesselator tessellator = RenderSystem.renderThreadTesselator();
        BufferBuilder bufferBuilder = tessellator.getBuilder();

        float alpha = opacity.getCurrentValue();

        for (AABB box : this.renderBoundingBoxes) {
            BlockPos pos = BlockPos.containing(box.minX, box.minY, box.minZ);
            boolean isOpened = this.openedChests.contains(pos);
            boolean isStealing = stealingAnimations.containsKey(pos);
            StorageType type = storageTypes.get(pos);

            if (type == null) continue;

            float red, green, blue;

            // 优先显示偷取动画
            if (isStealing && stealingAnimation.getCurrentValue()) {
                float progress = (float)(System.currentTimeMillis() - stealingAnimations.get(pos)) / 3000f;
                float pulse = (float)(Math.sin(progress * Math.PI * 8) * 0.3f + 0.7f);
                red = stolenColor[0] * pulse;
                green = stolenColor[1] * pulse;
                blue = stolenColor[2] * pulse;
            }
            // 已被打开的容器
            else if (isOpened && useCustomOpenedChestColor.getCurrentValue()) {
                red = openedChestRed.getCurrentValue() / 255.0f;
                green = openedChestGreen.getCurrentValue() / 255.0f;
                blue = openedChestBlue.getCurrentValue() / 255.0f;
            } else if (isOpened) {
                float[] defaultColor = getDefaultColor(type, true);
                red = defaultColor[0];
                green = defaultColor[1];
                blue = defaultColor[2];
            }
            // 自定义颜色的未打开容器
            else if (useCustomChestColor.getCurrentValue()) {
                red = chestRed.getCurrentValue() / 255.0f;
                green = chestGreen.getCurrentValue() / 255.0f;
                blue = chestBlue.getCurrentValue() / 255.0f;
            }
            // 默认颜色的未打开容器
            else {
                float[] defaultColor = getDefaultColor(type, false);
                red = defaultColor[0];
                green = defaultColor[1];
                blue = defaultColor[2];
            }

            RenderSystem.setShaderColor(red, green, blue, alpha);
            RenderUtils.drawSolidBox(bufferBuilder, stack.last().pose(), box);
        }

        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        stack.popPose();
    }

    private float[] getDefaultColor(StorageType type, boolean isOpened) {
        if (isOpened) {
            return stolenColor;
        }
        switch (type) {
            case CHEST:
                return chestColor;
            case TRAPPED_CHEST:
                return trappedChestColor;
            case FURNACE:
                return furnaceColor;
            case BREWING_STAND:
                return brewingStandColor;
            default:
                return chestColor;
        }
    }

    // 添加偷取动画的方法
    public void startStealingAnimation(BlockPos pos) {
        stealingAnimations.put(pos, System.currentTimeMillis());
    }

    private enum StorageType {
        CHEST,
        TRAPPED_CHEST,
        FURNACE,
        BREWING_STAND
    }
}