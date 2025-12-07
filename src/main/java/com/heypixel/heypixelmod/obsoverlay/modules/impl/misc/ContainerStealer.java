package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ModuleInfo(
        name = "ContainerStealer",
        description = "Automatically steals items from chests",
        category = Category.MISC
)
public class ContainerStealer extends Module {
    private static final TickTimeHelper timer = new TickTimeHelper();
    private static final TickTimeHelper timer2 = new TickTimeHelper();
    private final BooleanValue swap = ValueBuilder.create(this, "Instant")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    private final FloatValue delay1 = ValueBuilder.create(this, "Multi Stack Delay (Ticks)")
            .setDefaultFloatValue(3.0f)
            .setFloatStep(1.0f)
            .setMinFloatValue(1.0f)
            .setMaxFloatValue(10.0f)
            .build()
            .getFloatValue();
    private final FloatValue delay = ValueBuilder.create(this, "Delay (ms)")
            .setDefaultFloatValue(150.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(500.0F)
            .build()
            .getFloatValue();
    private final BooleanValue pickEnderChest = ValueBuilder.create(this, "Ender Chest").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue brewingStand = ValueBuilder.create(this, "Brewing Stand").setDefaultBooleanValue(true).build().getBooleanValue();
    private final BooleanValue furnace = ValueBuilder.create(this, "Furnace").setDefaultBooleanValue(true).build().getBooleanValue();
    private Screen lastTickScreen;
    private final List<ChestInfo> chests = new CopyOnWriteArrayList<>();

    public static boolean isWorking() {
        return !timer.delay(3);
    }

    @EventTarget
    public void onEnable() {
        super.onEnable();
    }
    @EventTarget
    public void onTick(EventMotion eventMotion) {
        if (ContainerStealer.mc.options.keyUse.isDown()) {
            if (eventMotion.getType() == EventType.POST) {
                return;
            }
            this.ghostInteractWithChest();
        }
    }
    public boolean ghostInteractWithChest() {
        if (ContainerStealer.mc.player == null || ContainerStealer.mc.level == null) {
            return false;
        }
        Vec3 eyePos = ContainerStealer.mc.player.getEyePosition(1.0f);
        Vec3 lookVec = ContainerStealer.mc.player.getViewVector(1.0f);
        Vec3 reachEnd = eyePos.add(lookVec.scale(4.0));
        ChestBlockEntity targetChest = null;
        BlockHitResult fakeHit = null;
        double closestDist = Double.MAX_VALUE;
        ArrayList<BlockEntity> blockEntities = ChunkUtils.getLoadedBlockEntities().collect(Collectors.toCollection(ArrayList::new));
        for (BlockEntity be : blockEntities) {
            double dist;
            Optional<Vec3> hit;
            ChestBlockEntity chest;
            AABB box;
            if (!(be instanceof ChestBlockEntity) || (box = this.getChestBox(chest = (ChestBlockEntity)be)) == null || !(hit = box.clip(eyePos, reachEnd)).isPresent() || !((dist = hit.get().distanceTo(eyePos)) < closestDist)) continue;
            closestDist = dist;
            targetChest = chest;
            fakeHit = new BlockHitResult(hit.get(), Direction.UP, chest.getBlockPos(), false);
        }
        if (targetChest != null && fakeHit != null) {
            ContainerStealer.mc.gameMode.useItemOn(ContainerStealer.mc.player, InteractionHand.MAIN_HAND, fakeHit);
            ContainerStealer.mc.player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    @EventTarget
    public void onUpdate(EventRender event) {
        this.chests.clear();
        double range = 6.0;

        Vec3 cameraPos = ContainerStealer.mc.gameRenderer.getMainCamera().getPosition();
        BlockPos playerPos = ContainerStealer.mc.player.blockPosition();

        // 扫描玩家周围6格内的箱子
        for (BlockPos pos : BlockPos.betweenClosed(
                playerPos.offset(-6, -6, -6),
                playerPos.offset(6, 6, 6))) {

            BlockEntity be = ContainerStealer.mc.level.getBlockEntity(pos);

            if (!(be instanceof ChestBlockEntity)) {
                continue;
            }

            // 计算箱子上方中心位置
            Vec3 topCenter = new Vec3(
                    pos.getX() + 0.5,
                    pos.getY() + 1.2,
                    pos.getZ() + 0.5
            );
            if (topCenter.distanceTo(cameraPos) > range) {
                continue;
            }

            Vector2f screenPos = ProjectionUtils.project(
                    topCenter.x, topCenter.y, topCenter.z,
                    event.getRenderPartialTicks()
            );

            this.chests.add(new ChestInfo(pos, screenPos));
        }
    }
    @EventTarget
    public void onRender(EventRender2D event) {
        for (ChestInfo chest : this.chests) {
            Vector2f pos = chest.getScreenPos();
            Fonts.harmony.render(
                    event.getStack(),
                    "[Chest]",
                    pos.x, pos.y,
                    Color.YELLOW,
                    true,
                    0.4f
            );
        }
    }
    private static class ChestInfo {
        BlockPos blockPos;
        Vector2f screenPos;

        public ChestInfo(BlockPos blockPos, Vector2f screenPos) {
            this.blockPos = blockPos;
            this.screenPos = screenPos;
        }

        public BlockPos getBlockPos() {
            return this.blockPos;
        }

        public Vector2f getScreenPos() {
            return this.screenPos;
        }

        public void setBlockPos(BlockPos blockPos) {
            this.blockPos = blockPos;
        }

        public void setScreenPos(Vector2f screenPos) {
            this.screenPos = screenPos;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof ChestInfo)) {
                return false;
            }
            ChestInfo other = (ChestInfo) o;
            if (!other.canEqual(this)) {
                return false;
            }
            BlockPos this$blockPos = this.getBlockPos();
            BlockPos other$blockPos = other.getBlockPos();
            if (this$blockPos == null ? other$blockPos != null : !this$blockPos.equals(other$blockPos)) {
                return false;
            }
            Vector2f this$screenPos = this.getScreenPos();
            Vector2f other$screenPos = other.getScreenPos();
            return this$screenPos == null ? other$screenPos == null : this$screenPos.equals(other$screenPos);
        }

        protected boolean canEqual(Object other) {
            return other instanceof ChestInfo;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            BlockPos $blockPos = this.getBlockPos();
            result = result * PRIME + ($blockPos == null ? 43 : $blockPos.hashCode());
            Vector2f $screenPos = this.getScreenPos();
            result = result * PRIME + ($screenPos == null ? 43 : $screenPos.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "ContainerStealer.ChestInfo(blockPos=" + this.getBlockPos() + ", screenPos=" + this.getScreenPos() + ")";
        }
    }

    @EventTarget(value=1)
    public void onMotion(EventMotion e) {
        if (e.getType() == EventType.PRE) {
            Screen currentScreen = ContainerStealer.mc.screen;

            if (currentScreen instanceof ContainerScreen) {
                ContainerScreen container = (ContainerScreen) currentScreen;
                ChestMenu menu = (ChestMenu) container.getMenu();

                if (currentScreen != this.lastTickScreen) {
                    timer.reset();
                } else {
                    String chestTitle = container.getTitle().getString();
                    String chest = Component.translatable("container.chest").getString();
                    String largeChest = Component.translatable("container.chestDouble").getString();
                    String enderChest = Component.translatable("container.enderchest").getString();
                    if (chestTitle.equals(chest) || chestTitle.equals(largeChest) || chestTitle.equals("Chest") ||
                            (this.pickEnderChest.getCurrentValue() && chestTitle.equals(enderChest))) {
                        if (this.isChestEmpty(menu) && timer.delay(this.delay.getCurrentValue())) {
                            ContainerStealer.mc.player.closeContainer();
                        } else {
                            List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9)
                                    .boxed()
                                    .collect(Collectors.toList());
                            Collections.shuffle(slots);

                            for (Integer pSlotId : slots) {
                                ItemStack stack = menu.getSlot(pSlotId).getItem();
                                if (!ContainerStealer.isItemUseful(stack) ||
                                        !this.isBestItemInChest(menu, stack) ||
                                        !timer.delay(this.delay.getCurrentValue())) {
                                    continue;
                                }

                                if (this.swap.getCurrentValue()) {
                                    int slot = ContainerStealer.getFirstEmptySlot();

                                    if (slot != -1 && slot + 18 < 54) {
                                        if (slot < 9) {
                                            ContainerStealer.mc.gameMode.handleInventoryMouseClick(
                                                    menu.containerId, pSlotId, slot, ClickType.SWAP, ContainerStealer.mc.player);
                                        } else {
                                            ContainerStealer.mc.gameMode.handleInventoryMouseClick(
                                                    menu.containerId, slot + 18, 8, ClickType.SWAP, ContainerStealer.mc.player);
                                            ContainerStealer.mc.gameMode.handleInventoryMouseClick(
                                                    menu.containerId, pSlotId, 8, ClickType.SWAP, ContainerStealer.mc.player);
                                        }
                                    } else {
                                        ContainerStealer.mc.player.closeContainer();
                                    }
                                } else {
                                    ContainerStealer.mc.gameMode.handleInventoryMouseClick(
                                            menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, ContainerStealer.mc.player);
                                }

                                timer.reset();
                                break;
                            }
                        }
                    }
                }
            }

            this.lastTickScreen = currentScreen;
        }
    }
    private AABB getChestBox(ChestBlockEntity chestBE) {
        BlockPos pos2;
        BlockState state = chestBE.getBlockState();
        if (!state.hasProperty(ChestBlock.TYPE)) {
            return null;
        }
        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.LEFT) {
            return null;
        }
        BlockPos pos = chestBE.getBlockPos();
        AABB box = BlockUtils.getBoundingBox(pos);
        if (chestType != ChestType.SINGLE && BlockUtils.canBeClicked(pos2 = pos.relative(ChestBlock.getConnectedDirection(state)))) {
            AABB box2 = BlockUtils.getBoundingBox(pos2);
            box = box.minmax(box2);
        }
        return box;
    }

    private boolean isBestItemInChest(ChestMenu menu, ItemStack stack) {
        if (!InventoryUtils.isGodItem(stack) && !InventoryUtils.isSharpnessAxe(stack)) {
            for (int i = 0; i < menu.getRowCount() * 9; i++) {
                ItemStack checkStack = menu.getSlot(i).getItem();
                if (stack.getItem() instanceof ArmorItem && checkStack.getItem() instanceof ArmorItem) {
                    ArmorItem item = (ArmorItem)stack.getItem();
                    ArmorItem checkItem = (ArmorItem)checkStack.getItem();
                    if (item.getEquipmentSlot() == checkItem.getEquipmentSlot() && InventoryUtils.getProtection(checkStack) > InventoryUtils.getProtection(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof SwordItem && checkStack.getItem() instanceof SwordItem) {
                    if (InventoryUtils.getSwordDamage(checkStack) > InventoryUtils.getSwordDamage(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof PickaxeItem && checkStack.getItem() instanceof PickaxeItem) {
                    if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof AxeItem && checkStack.getItem() instanceof AxeItem) {
                    if (InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                        return false;
                    }
                } else if (stack.getItem() instanceof ShovelItem
                        && checkStack.getItem() instanceof ShovelItem
                        && InventoryUtils.getToolScore(checkStack) > InventoryUtils.getToolScore(stack)) {
                    return false;
                }
            }

            return true;
        } else {
            return true;
        }
    }

    private boolean isChestEmpty(ChestMenu menu) {
        for (int i = 0; i < menu.getRowCount() * 9; i++) {
            ItemStack item = menu.getSlot(i).getItem();
            if (!item.isEmpty() && isItemUseful(item) && this.isBestItemInChest(menu, item)) {
                return false;
            }
        }

        return true;
    }
    public static int getFirstEmptySlot() {
        // 获取第一个空槽位
        Inventory inventory = ContainerStealer.mc.player.getInventory();

        for (int i = 0; i < inventory.items.size(); ++i) {
            if (i == 8 || !inventory.getItem(i).isEmpty()) {
                continue;
            }

            return i;
        }

        return -1;
    }

    public static boolean isItemUseful(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else if (InventoryUtils.isGodItem(stack) || InventoryUtils.isSharpnessAxe(stack)) {
            return true;
        } else if (stack.getItem() instanceof ArmorItem) {
            ArmorItem item = (ArmorItem)stack.getItem();
            float protection = InventoryUtils.getProtection(stack);
            float bestArmor = InventoryUtils.getBestArmorScore(item.getEquipmentSlot());
            return !(protection <= bestArmor);
        } else if (stack.getItem() instanceof SwordItem) {
            float damage = InventoryUtils.getSwordDamage(stack);
            float bestDamage = InventoryUtils.getBestSwordDamage();
            return !(damage <= bestDamage);
        } else if (stack.getItem() instanceof PickaxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestPickaxeScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof AxeItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestAxeScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof ShovelItem) {
            float score = InventoryUtils.getToolScore(stack);
            float bestScore = InventoryUtils.getBestShovelScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof CrossbowItem) {
            float score = InventoryUtils.getCrossbowScore(stack);
            float bestScore = InventoryUtils.getBestCrossbowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPunchBow(stack)) {
            float score = InventoryUtils.getPunchBowScore(stack);
            float bestScore = InventoryUtils.getBestPunchBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() instanceof BowItem && InventoryUtils.isPowerBow(stack)) {
            float score = InventoryUtils.getPowerBowScore(stack);
            float bestScore = InventoryUtils.getBestPowerBowScore();
            return !(score <= bestScore);
        } else if (stack.getItem() == Items.COMPASS) {
            return !InventoryUtils.hasItem(stack.getItem());
        } else if (stack.getItem() == Items.WATER_BUCKET && InventoryUtils.getItemCount(Items.WATER_BUCKET) >= InventoryCleaner.getWaterBucketCount()) {
            return false;
        } else if (stack.getItem() == Items.LAVA_BUCKET && InventoryUtils.getItemCount(Items.LAVA_BUCKET) >= InventoryCleaner.getLavaBucketCount()) {
            return false;
        } else if (stack.getItem() instanceof BlockItem
                && Scaffold.isValidStack(stack)
                && InventoryUtils.getBlockCountInInventory() + stack.getCount() >= InventoryCleaner.getMaxBlockSize()) {
            return false;
        } else if (stack.getItem() == Items.ARROW && InventoryUtils.getItemCount(Items.ARROW) + stack.getCount() >= InventoryCleaner.getMaxArrowSize()) {
            return false;
        } else if (stack.getItem() instanceof FishingRodItem && InventoryUtils.getItemCount(Items.FISHING_ROD) >= 1) {
            return false;
        } else if (stack.getItem() != Items.SNOWBALL && stack.getItem() != Items.EGG
                || InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG) + stack.getCount() < InventoryCleaner.getMaxProjectileSize()
                && InventoryCleaner.shouldKeepProjectile()) {
            return stack.getItem() instanceof ItemNameBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
        } else {
            return false;
        }
    }
}