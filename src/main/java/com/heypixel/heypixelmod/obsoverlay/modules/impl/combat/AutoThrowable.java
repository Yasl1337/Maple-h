package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.EventRunTicks;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.PacketUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

@ModuleInfo(
        name = "AutoThrowable",
        description = "Grim-Proof Auto Throw Snowball/Egg/Potion/Pearl",
        category = Category.COMBAT
)
public class AutoThrowable extends Module {
    private final Minecraft mc = Minecraft.getInstance();

    private final FloatValue detectionRange = ValueBuilder.create(this, "Detection Range")
            .setDefaultFloatValue(8.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(15.0F)
            .build()
            .getFloatValue();

    private final FloatValue throwRange = ValueBuilder.create(this, "Throw Range")
            .setDefaultFloatValue(6.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(12.0F)
            .build()
            .getFloatValue();

    private final FloatValue minRange = ValueBuilder.create(this, "Min Range")
            .setDefaultFloatValue(3.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    private final FloatValue delay = ValueBuilder.create(this, "Delay (ms)")
            .setDefaultFloatValue(500.0F)
            .setFloatStep(50.0F)
            .setMinFloatValue(100.0F)
            .setMaxFloatValue(2000.0F)
            .build()
            .getFloatValue();

    private final FloatValue fovLimit = ValueBuilder.create(this, "FOV Limit")
            .setDefaultFloatValue(90.0F)
            .setFloatStep(5.0F)
            .setMinFloatValue(30.0F)
            .setMaxFloatValue(180.0F)
            .build()
            .getFloatValue();

    public final BooleanValue ignoreTeammates = ValueBuilder.create(this, "Ignore Teammates")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue targetPlayers = ValueBuilder.create(this, "Players")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue targetMonsters = ValueBuilder.create(this, "Monsters")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue autoSwitch = ValueBuilder.create(this, "Switch Back")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue predictiveAiming = ValueBuilder.create(this, "Predictive Aiming")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue useOffhand = ValueBuilder.create(this, "Use Offhand")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue checkOtherModules = ValueBuilder.create(this, "Check Other Modules")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue botCheck = ValueBuilder.create(this, "Bot Check")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue useSnowballs = ValueBuilder.create(this, "Use Snowballs")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue useEggs = ValueBuilder.create(this, "Use Eggs")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue usePotions = ValueBuilder.create(this, "Use Potions")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue usePearls = ValueBuilder.create(this, "Use Enderpearls")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue useExpBottles = ValueBuilder.create(this, "Use Exp Bottles")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue noWall = ValueBuilder.create(this, "No Wall")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue autoManageOffhand = ValueBuilder.create(this, "Auto Manage Offhand")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    public final BooleanValue legit = ValueBuilder.create(this, "Legit Mode")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    public final BooleanValue debug = ValueBuilder.create(this, "Debug")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    private int tickCounter = 0;
    private int lastSlot = -1;
    private boolean isThrowing = false;
    private Entity target;
    private long nextThrow = 0;

    @Override
    public void onEnable() {
        tickCounter = 0;
        lastSlot = -1;
        isThrowing = false;
        target = null;
        nextThrow = 0;
    }

    @Override
    public void onDisable() {
        if (autoSwitch.getCurrentValue() && lastSlot != -1 && mc.player != null) {
            mc.player.getInventory().selected = lastSlot;
        }
        isThrowing = false;
        target = null;
    }

    private boolean shouldWork() {
        if (!checkOtherModules.getCurrentValue()) {
            return true;
        }

        try {
            Aura auraModule = (Aura) Naven.getInstance().getModuleManager().getModule(Aura.class);
            if (auraModule != null && auraModule.isEnabled()) {
                return false;
            }
        } catch (Exception e) {
            return true;
        }

        return true;
    }

    @EventTarget
    public void onTick(EventRunTicks event) {
        if (event.getType() != EventType.POST || mc.player == null || mc.level == null || isThrowing || mc.screen != null) {
            return;
        }

        if (!shouldWork()) {
            return;
        }

        LocalPlayer player = mc.player;
        long now = System.currentTimeMillis();

        if (now < nextThrow) {
            return;
        }

        if (autoManageOffhand.getCurrentValue() && useOffhand.getCurrentValue()) {
            manageOffhandItems();
        }

        if (tickCounter < delay.getCurrentValue() / 50) {
            tickCounter++;
            return;
        }
        tickCounter = 0;

        target = findTarget(player);
        if (target == null) return;

        double distance = player.distanceTo(target);
        if (distance > throwRange.getCurrentValue() || distance <= minRange.getCurrentValue()) {
            target = null;
            return;
        }

        int throwableSlot = findThrowableSlot();
        if (throwableSlot == -1) return;

        if (isAimingAtTarget(player, target)) {
            InteractionHand hand = findThrowableHand();
            if (hand == null) return;

            if (hand == InteractionHand.MAIN_HAND && autoSwitch.getCurrentValue()) {
                if (lastSlot == -1) {
                    lastSlot = player.getInventory().selected;
                }
                player.getInventory().selected = throwableSlot;
            }

            if (canHitTarget(player, target)) {
                isThrowing = true;
                throwItemWithAnimation(player, target, hand);
                isThrowing = false;

                if (hand == InteractionHand.MAIN_HAND && autoSwitch.getCurrentValue() && lastSlot != -1) {
                    mc.execute(() -> player.getInventory().selected = lastSlot);
                    lastSlot = -1;
                }

                nextThrow = now + (long) delay.getCurrentValue();
                debug("Threw item! Next throw in " + (nextThrow - now) + "ms");

                target = null;
            }
        }
    }

    private void manageOffhandItems() {
        if (mc.player == null || mc.level == null) return;

        ItemStack offhandItem = mc.player.getOffhandItem();
        if (!isThrowableItem(offhandItem)) {
            int bestSlot = findBestThrowableSlot();
            if (bestSlot != -1) {
                moveItemToOffhand(bestSlot);
            }
        }
    }

    private int findBestThrowableSlot() {
        if (mc.player == null) return -1;

        int eggSlot = -1, eggCount = 0;
        int snowballSlot = -1, snowballCount = 0;
        int potionSlot = -1, potionCount = 0;
        int pearlSlot = -1, pearlCount = 0;
        int expSlot = -1, expCount = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            Item item = stack.getItem();

            if (item instanceof EggItem && useEggs.getCurrentValue()) {
                if (stack.getCount() > eggCount) {
                    eggCount = stack.getCount();
                    eggSlot = i;
                }
            } else if (item instanceof SnowballItem && useSnowballs.getCurrentValue()) {
                if (stack.getCount() > snowballCount) {
                    snowballCount = stack.getCount();
                    snowballSlot = i;
                }
            } else if (item instanceof PotionItem && usePotions.getCurrentValue()) {
                if (stack.getCount() > potionCount) {
                    potionCount = stack.getCount();
                    potionSlot = i;
                }
            } else if (item instanceof EnderpearlItem && usePearls.getCurrentValue()) {
                if (stack.getCount() > pearlCount) {
                    pearlCount = stack.getCount();
                    pearlSlot = i;
                }
            } else if (item instanceof ExperienceBottleItem && useExpBottles.getCurrentValue()) {
                if (stack.getCount() > expCount) {
                    expCount = stack.getCount();
                    expSlot = i;
                }
            }
        }

        if (useEggs.getCurrentValue() && eggSlot != -1) return eggSlot;
        if (useSnowballs.getCurrentValue() && snowballSlot != -1) return snowballSlot;
        if (usePotions.getCurrentValue() && potionSlot != -1) return potionSlot;
        if (usePearls.getCurrentValue() && pearlSlot != -1) return pearlSlot;
        if (useExpBottles.getCurrentValue() && expSlot != -1) return expSlot;

        return -1;
    }

    private void moveItemToOffhand(int sourceSlot) {
        if (debug.getCurrentValue()) {
            debug("Moving item from slot " + sourceSlot + " to offhand");
        }
    }

    private boolean canHitTarget(LocalPlayer player, Entity target) {
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) {
            return true;
        }

        Vec3 startVec = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().scale(throwRange.getCurrentValue());
        Vec3 endVec = startVec.add(lookVec);

        AABB expandedBox = player.getBoundingBox().expandTowards(lookVec).inflate(1.0D);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(
                player, startVec, endVec, expandedBox,
                entity -> !entity.isSpectator() && entity.isPickable(),
                throwRange.getCurrentValue() * throwRange.getCurrentValue()
        );

        return entityHitResult != null && entityHitResult.getEntity() == target;
    }

    private Entity findTarget(LocalPlayer player) {
        float range = detectionRange.getCurrentValue();
        AABB detectionBox = new AABB(
                player.getX() - range,
                player.getY() - range,
                player.getZ() - range,
                player.getX() + range,
                player.getY() + range,
                player.getZ() + range
        );

        List<Entity> allEntities = mc.level.getEntities(null, detectionBox);

        return allEntities.stream()
                .filter(entity -> isValidTarget(player, entity))
                .min(Comparator.comparingDouble(entity -> player.distanceToSqr(entity)))
                .orElse(null);
    }

    private boolean isValidTarget(LocalPlayer player, Entity target) {
        if (target == player || !target.isAlive()) return false;

        double distance = player.distanceTo(target);
        if (distance > detectionRange.getCurrentValue() || distance <= minRange.getCurrentValue()) return false;

        if (!isInFieldOfView(player, target)) return false;

        // 使用 ClipContext 进行视线检测（高版本兼容）
        if (noWall.getCurrentValue() && !canSee(player, target)) {
            return false;
        }

        if (botCheck.getCurrentValue() && isBot(target)) {
            return false;
        }

        if (target instanceof Player) {
            Player playerTarget = (Player) target;
            return targetPlayers.getCurrentValue() && (!ignoreTeammates.getCurrentValue() || !isTeammate(player, playerTarget));
        } else if (target instanceof Monster) {
            return targetMonsters.getCurrentValue();
        }
        return false;
    }

    // 高版本兼容的视线检测：使用 ClipContext
    private boolean canSee(LocalPlayer player, Entity target) {
        if (mc.level == null) return false;
        Vec3 start = player.getEyePosition(1.0F);
        Vec3 end = target.getEyePosition(1.0F);

        // 创建 ClipContext：检测方块，不忽略液体，不检测实体
        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,  // 检测碰撞方块
                ClipContext.Fluid.NONE,      // 不忽略液体
                player                        // 忽略的实体（自身）
        );

        BlockHitResult hitResult = mc.level.clip(context);
        return hitResult.getType() == HitResult.Type.MISS;  // 无方块阻挡
    }

    private boolean isInFieldOfView(LocalPlayer player, Entity target) {
        if (fovLimit.getCurrentValue() >= 180.0F) return true;

        Vec3 playerLookVec = player.getLookAngle();
        Vec3 playerToTargetVec = target.position().subtract(player.position()).normalize();

        double dotProduct = playerLookVec.x * playerToTargetVec.x +
                playerLookVec.y * playerToTargetVec.y +
                playerLookVec.z * playerToTargetVec.z;
        double angle = Math.toDegrees(Math.acos(dotProduct));

        return angle <= fovLimit.getCurrentValue() / 2.0F;
    }

    private boolean isAimingAtTarget(LocalPlayer player, Entity target) {
        Vec3 targetPos = predictiveAiming.getCurrentValue() ?
                calculatePredictedPosition(target) :
                target.position().add(0, target.getBbHeight() / 2, 0);

        Vec3 playerPos = player.getEyePosition(1.0F);
        Vec3 playerLookVec = player.getLookAngle();

        Vec3 playerToTargetVec = targetPos.subtract(playerPos).normalize();
        double dotProduct = playerLookVec.x * playerToTargetVec.x +
                playerLookVec.y * playerToTargetVec.y +
                playerLookVec.z * playerToTargetVec.z;

        return dotProduct > 0.9;
    }

    // 简易 Bot 检测
    private boolean isBot(Entity entity) {
        if (!(entity instanceof Player)) return false;
        Player player = (Player) entity;
        return player.getGameProfile().getName().contains("Bot") ||
                player.getGameProfile().getName().toLowerCase().contains("npc");
    }

    private boolean isTeammate(LocalPlayer player, Player target) {
        return player.getTeam() != null &&
                target.getTeam() != null &&
                player.getTeam().getName().equals(target.getTeam().getName());
    }

    private InteractionHand findThrowableHand() {
        if (mc.player == null) return null;

        if (useOffhand.getCurrentValue() && isThrowableItem(mc.player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }

        if (isThrowableItem(mc.player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }

        if (autoSwitch.getCurrentValue() && findThrowableSlot() != -1) {
            return InteractionHand.MAIN_HAND;
        }

        return null;
    }

    private int findThrowableSlot() {
        LocalPlayer player = mc.player;
        if (player == null) return -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isThrowableItem(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isThrowableItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();

        return (useSnowballs.getCurrentValue() && item instanceof SnowballItem) ||
                (useEggs.getCurrentValue() && item instanceof EggItem) ||
                (usePotions.getCurrentValue() && item instanceof PotionItem) ||
                (usePearls.getCurrentValue() && item instanceof EnderpearlItem) ||
                (useExpBottles.getCurrentValue() && item instanceof ExperienceBottleItem);
    }

    private void throwItemWithAnimation(LocalPlayer player, Entity target, InteractionHand hand) {
        if (mc.gameMode == null || player == null) return;
        float originalYaw = player.getYRot();
        float originalPitch = player.getXRot();
        aimAtTarget(player, target);

        if (legit.getCurrentValue()) {
            KeyMapping.set(mc.options.keyUse.getKey(), true);
            KeyMapping.set(mc.options.keyUse.getKey(), false);
        } else {
            PacketUtils.sendSequencedPacket(id -> new ServerboundUseItemPacket(hand, id));
        }

        player.swing(hand);
        player.setYRot(originalYaw);
        player.setXRot(originalPitch);
    }

    private void aimAtTarget(LocalPlayer player, Entity target) {
        if (player == null || target == null) return;
        Vec3 targetPos = predictiveAiming.getCurrentValue() ?
                calculatePredictedPosition(target) :
                target.position().add(0, target.getBbHeight() / 2, 0);

        Vec3 playerPos = player.getEyePosition(1.0F);

        double dx = targetPos.x - playerPos.x;
        double dy = targetPos.y - playerPos.y;
        double dz = targetPos.z - playerPos.z;

        double yaw = Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double pitch = Math.atan2(dy, distance) * 180.0 / Math.PI;

        player.setYRot((float) yaw);
        player.setXRot((float) -pitch);
    }

    private Vec3 calculatePredictedPosition(Entity target) {
        Vec3 currentPos = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 velocity = target.getDeltaMovement();
        double distance = mc.player.distanceTo(target);
        double timeToTarget = distance / 1.5;

        return new Vec3(
                currentPos.x + velocity.x * timeToTarget,
                currentPos.y + velocity.y * timeToTarget,
                currentPos.z + velocity.z * timeToTarget
        );
    }

    private void debug(String message) {
        if (debug.getCurrentValue()) {
            System.out.println("[AutoThrowable] " + message);
        }
    }
}