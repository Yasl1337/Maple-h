package com.heypixel.heypixelmod.obsoverlay.modules.impl.misc;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMotion;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventMoveInput;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRender2D;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Scaffold;
import com.heypixel.heypixelmod.obsoverlay.utils.InventoryUtils;
import com.heypixel.heypixelmod.obsoverlay.utils.TickTimeHelper;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;

@ModuleInfo(
        name = "ChestStealer",
        description = "Automatically steals items from chests",
        category = Category.MISC
)
public class ChestStealer extends Module {
    private static final TickTimeHelper timer = new TickTimeHelper();
    private static final TickTimeHelper timer1 = new TickTimeHelper();
    private static final TickTimeHelper closeTimer = new TickTimeHelper();
    private final FloatValue delay = ValueBuilder.create(this, "Delay (Ticks)")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(3.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final FloatValue instantDelay = ValueBuilder.create(this, "Delay (Ticks) (Multi)")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0f)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    private final BooleanValue instant = ValueBuilder.create(this, "Instant").setDefaultBooleanValue(false).build().getBooleanValue();
    private final BooleanValue pickEnderChest = ValueBuilder.create(this, "Ender Chest").setDefaultBooleanValue(false).build().getBooleanValue();
    public final BooleanValue noGui = ValueBuilder.create(this, "Cancel Gui").setDefaultBooleanValue(false).build().getBooleanValue();

    private Screen lastTickScreen;
    public static boolean isInScreen;
    public static boolean isWorking() {
        return !timer.delay(3);
    }
    public static float x = 0;
    public static float y = 0;


    private BlockPos currentChest;

    public BlockPos getCurrentChest(){return currentChest;}

    @EventTarget
    public void onTick(EventRunTicks e) {
        isInScreen = mc.screen instanceof ContainerScreen && noGui.getCurrentValue();
        if (isInScreen && noGui.getCurrentValue()) {
            mc.mouseHandler.grabMouse();
        }
    }

    @EventTarget(1)
    public void onMotion(EventMotion e) {
        x = mc.getWindow().getGuiScaledWidth() / 2.0F;
        y = mc.getWindow().getGuiScaledHeight() / 2.0F;
        if (mc.player == null || mc.level == null) return;

        if (e.getType() == EventType.PRE) {
            Screen currentScreen = mc.screen;
            if (currentScreen instanceof ContainerScreen container) {
                ChestMenu menu = (ChestMenu)container.getMenu();
                if (currentScreen != this.lastTickScreen) {
                    timer.reset();
                    timer1.reset();
                    closeTimer.reset();
                } else {
                    String chestTitle = container.getTitle().getString();
                    String chest = Component.translatable("container.chest").getString();
                    String largeChest = Component.translatable("container.chestDouble").getString();
                    String enderChest = Component.translatable("container.enderchest").getString();
                    if (chestTitle.equals(chest)
                            || chestTitle.equals(largeChest)
                            || chestTitle.equals("Chest")
                            || this.pickEnderChest.getCurrentValue() && chestTitle.equals(enderChest)) {
                        if (this.isChestEmpty(menu) && timer.delay(getRandomizedDelay())) {
                            mc.player.closeContainer();
                        } else {
                            List<Integer> slots = IntStream.range(0, menu.getRowCount() * 9).boxed().collect(Collectors.toList());
                            Collections.shuffle(slots);

                            for (Integer pSlotId : slots) {
                                ItemStack stack = menu.getSlot(pSlotId).getItem();
                                if (isItemUseful(stack) && this.isBestItemInChest(menu, stack) && (timer.delay(getRandomizedDelay()))) {
                                    if (instant.getCurrentValue()) {
                                        int slot = getFirstEmptySlot();
                                        if (slot != -1 && slot + 18 < 54) {
                                            if (stack.getCount() <= 1 && !(stack.getItem() instanceof EnderpearlItem) && stack.getItem() != Items.TNT) {
                                                if (slot < 9) {
                                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, slot, ClickType.SWAP, mc.player);
                                                } else {
                                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, slot + 18, 8, ClickType.SWAP, mc.player);
                                                    mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 8, ClickType.SWAP, mc.player);
                                                }
                                            } else if (timer1.delay(instantDelay.getCurrentValue())) {
                                                mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, mc.player);
                                                timer1.reset();
                                            }
                                        } else {
                                            mc.player.closeContainer();
                                        }
                                    } else {
                                        mc.gameMode.handleInventoryMouseClick(menu.containerId, pSlotId, 0, ClickType.QUICK_MOVE, mc.player);
                                    }
                                    timer.reset();
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            this.lastTickScreen = currentScreen;
        }
    }
    @EventTarget
    public void onMoveInput(EventMoveInput e) {
        if (isInScreen) {
            e.setForward(0);
        }
    }
    public static int getFirstEmptySlot() {
        Inventory inventory = mc.player.getInventory();

        for(int i = 0; i < inventory.items.size(); ++i) {
            if (i != 8 && inventory.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
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
                || InventoryUtils.getItemCount(Items.SNOWBALL) + InventoryUtils.getItemCount(Items.EGG) + stack.getCount()
                < InventoryCleaner.getMaxProjectileSize()
                && InventoryCleaner.shouldKeepProjectile()) {
            return stack.getItem() instanceof ItemNameBlockItem ? false : InventoryUtils.isCommonItemUseful(stack);
        } else {
            return false;
        }
    }

    private float getRandomizedDelay() {
        float base = this.delay.getCurrentValue();
        int offset = (Math.random() < 0.5 ? 1 : 2);
        if (Math.random() < 0.5) {
            base += offset;
        } else {
            base -= offset;
            if (base < this.delay.getMinValue()) {
                base = this.delay.getMinValue();
            }
        }

        if (Math.random() < 0.1) {
            base += (float)(Math.random());
        }

        return base;
    }
}
