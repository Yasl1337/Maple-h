package com.heypixel.heypixelmod.obsoverlay.modules.impl.move;

import com.heypixel.heypixelmod.obsoverlay.annotations.FlowExclude;
import com.heypixel.heypixelmod.obsoverlay.annotations.ParameterObfuscationExclude;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.utils.renderer.Fonts;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.apache.commons.lang3.RandomUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@ModuleInfo(
        name = "Scaffold",
        description = "Automatically places blocks under you",
        category = Category.MOVEMENT
)
public class Scaffold extends Module {
    public static final List<Block> blacklistedBlocks = Arrays.asList(
            Blocks.AIR,
            Blocks.WATER,
            Blocks.LAVA,
            Blocks.ENCHANTING_TABLE,
            Blocks.GLASS_PANE,
            Blocks.GLASS_PANE,
            Blocks.IRON_BARS,
            Blocks.SNOW,
            Blocks.COAL_ORE,
            Blocks.DIAMOND_ORE,
            Blocks.EMERALD_ORE,
            Blocks.CHEST,
            Blocks.TRAPPED_CHEST,
            Blocks.TORCH,
            Blocks.ANVIL,
            Blocks.TRAPPED_CHEST,
            Blocks.NOTE_BLOCK,
            Blocks.JUKEBOX,
            Blocks.TNT,
            Blocks.GOLD_ORE,
            Blocks.IRON_ORE,
            Blocks.LAPIS_ORE,
            Blocks.STONE_PRESSURE_PLATE,
            Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
            Blocks.STONE_BUTTON,
            Blocks.LEVER,
            Blocks.TALL_GRASS,
            Blocks.TRIPWIRE,
            Blocks.TRIPWIRE_HOOK,
            Blocks.RAIL,
            Blocks.CORNFLOWER,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.VINE,
            Blocks.SUNFLOWER,
            Blocks.LADDER,
            Blocks.FURNACE,
            Blocks.SAND,
            Blocks.CACTUS,
            Blocks.DISPENSER,
            Blocks.DROPPER,
            Blocks.CRAFTING_TABLE,
            Blocks.COBWEB,
            Blocks.PUMPKIN,
            Blocks.COBBLESTONE_WALL,
            Blocks.OAK_FENCE,
            Blocks.REDSTONE_TORCH,
            Blocks.FLOWER_POT
    );
    public Vector2f correctRotation = new Vector2f();
    public Vector2f rots = new Vector2f();
    public Vector2f lastRots = new Vector2f();
    private int offGroundTicks = 0;
    private int rescueTicks = 0;
    private boolean isRescuing = false;
    private float originalOffGroundTicks = 5.0F;
    private double lastY = 0.0;
    private int fallDetectionTicks = 0;
    private Vec3 lastVelocity = Vec3.ZERO;
    private Vec3 lastAcceleration = Vec3.ZERO;
    public ModeValue mode = ValueBuilder.create(this, "Mode").setDefaultModeIndex(0).setModes("Normal", "Telly Bridge").build().getModeValue();
    public BooleanValue eagle = ValueBuilder.create(this, "Eagle2")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue renderBlockCounter = ValueBuilder.create(this, "Render Block Counter").setDefaultBooleanValue(false).build().getBooleanValue();
    public ModeValue blockCounterMode = ValueBuilder.create(this, "Block Counter Mode")
            .setVisibility(this.renderBlockCounter::getCurrentValue)
            .setModes("Normal", "Capsule")
            .setDefaultModeIndex(0)
            .build()
            .getModeValue();
    public BooleanValue sneak = ValueBuilder.create(this, "Sneak").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue snap = ValueBuilder.create(this, "Snap")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal"))
            .build()
            .getBooleanValue();
    public BooleanValue hideSnap = ValueBuilder.create(this, "Hide Snap Rotation")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Normal") && this.snap.getCurrentValue())
            .build()
            .getBooleanValue();
    public BooleanValue renderItemSpoof = ValueBuilder.create(this, "Render Item Spoof").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue keepFoV = ValueBuilder.create(this, "Keep FoV").setDefaultBooleanValue(true).build().getBooleanValue();
    public BooleanValue cancelSprintFovChange = ValueBuilder.create(this, "CancelSprintFovChange")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    FloatValue speedFov = ValueBuilder.create(this, "FoV")
            .setDefaultFloatValue(1.0F)
            .setMaxFloatValue(2.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(0.1F)
            .build()
            .getFloatValue();
    public BooleanValue randomOffGroundTicks = ValueBuilder.create(this, "Random OffGround Ticks")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
            .build()
            .getBooleanValue();
    public BooleanValue smartOffGround = ValueBuilder.create(this, "Smart OffGround")
            .setDefaultBooleanValue(true)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
            .build()
            .getBooleanValue();
    FloatValue fallDetectionThreshold = ValueBuilder.create(this, "Fall Detection Threshold")
            .setDefaultFloatValue(0.5F)
            .setMaxFloatValue(2.0F)
            .setMinFloatValue(0.1F)
            .setFloatStep(0.1F)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge") && this.smartOffGround.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue rescueDelay = ValueBuilder.create(this, "SetPerform Delay")
            .setDefaultFloatValue(2.0F)
            .setMaxFloatValue(5.0F)
            .setMinFloatValue(0.5F)
            .setFloatStep(0.25F)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge") && this.smartOffGround.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue tellyRotationSpeed = ValueBuilder.create(this, "rotateToYaw")
            .setDefaultFloatValue(120.0F)
            .setMaxFloatValue(180.0F)
            .setMinFloatValue(10.0F)
            .setFloatStep(10.0F)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
            .build()
            .getFloatValue();
    FloatValue rotationSpeed = ValueBuilder.create(this, "Rotation Speed")
            .setDefaultFloatValue(180.0F)
            .setMaxFloatValue(360.0F)
            .setMinFloatValue(10.0F)
            .setFloatStep(10.0F)
            .build()
            .getFloatValue();
    public BooleanValue stepRotation = ValueBuilder.create(this, "Step Rotation")
            .setDefaultBooleanValue(false)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge"))
            .build()
            .getBooleanValue();
    FloatValue step = ValueBuilder.create(this, "Step")
            .setDefaultFloatValue(45.0F)
            .setMaxFloatValue(180.0F)
            .setMinFloatValue(10.0F)
            .setFloatStep(5.0F)
            .setVisibility(() -> this.mode.isCurrentMode("Telly Bridge") && this.stepRotation.getCurrentValue())
            .build()
            .getFloatValue();
    public BooleanValue acceleration = ValueBuilder.create(this, "Acceleration")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();
    FloatValue probability = ValueBuilder.create(this, "Probability")
            .setDefaultFloatValue(0.5F)
            .setMaxFloatValue(1.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(0.1F)
            .setVisibility(() -> this.acceleration.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue accelerationStrength = ValueBuilder.create(this, "Acceleration Strength")
            .setDefaultFloatValue(10.0F)
            .setMaxFloatValue(50.0F)
            .setMinFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setVisibility(() -> this.acceleration.getCurrentValue())
            .build()
            .getFloatValue();
    public BooleanValue swing = ValueBuilder.create(this, "Swing")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    FloatValue maxOffGroundTicks = ValueBuilder.create(this, "Max OffGround Ticks")
            .setDefaultFloatValue(5.0F)
            .setMaxFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(0.15F)
            .setVisibility(() -> this.randomOffGroundTicks.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue minOffGroundTicks = ValueBuilder.create(this, "Min OffGround Ticks")
            .setDefaultFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .setMinFloatValue(0.0F)
            .setFloatStep(0.15F)
            .setVisibility(() -> this.randomOffGroundTicks.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue offGroundTicksValue = ValueBuilder.create(this, "OffGround Ticks")
            .setDefaultFloatValue(2.5F)
            .setMaxFloatValue(5.0F)
            .setMinFloatValue(0.1F)
            .setFloatStep(0.15F)
            .setVisibility(() -> !this.randomOffGroundTicks.getCurrentValue())
            .build()
            .getFloatValue();
    int oldSlot;
    private Scaffold.BlockPosWithFacing pos;
    private int lastSneakTicks;
    public int baseY = -1;

    private float blockCounterWidth;
    private float blockCounterHeight;
    private long lastPlaceTime = 0;
    private int initialBlockCount = 0; 

    
    private int getBlockCount() {
        if (mc.player == null) return 0;
        int count = 0;
        for (ItemStack itemStack : mc.player.getInventory().items) {
            if (itemStack.getItem() instanceof BlockItem) {
                count += itemStack.getCount();
            }
        }
        return count;
    }

    public static boolean isValidStack(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof BlockItem) || stack.getCount() <= 1) {
            return false;
        } else if (!InventoryUtils.isItemValid(stack)) {
            return false;
        } else {
            String string = stack.getDisplayName().getString();
            if (string.contains("Click") || string.contains("点击")) {
                return false;
            } else if (stack.getItem() instanceof ItemNameBlockItem) {
                return false;
            } else {
                Block block = ((BlockItem)stack.getItem()).getBlock();
                if (block instanceof FlowerBlock) {
                    return false;
                } else if (block instanceof BushBlock) {
                    return false;
                } else if (block instanceof FungusBlock) {
                    return false;
                } else if (block instanceof CropBlock) {
                    return false;
                } else {
                    return block instanceof SlabBlock ? false : !blacklistedBlocks.contains(block);
                }
            }
        }
    }

    public static boolean isOnBlockEdge(float sensitivity) {
        return !mc.level
                .getCollisions(mc.player, mc.player.getBoundingBox().move(0.0, -0.5, 0.0).inflate((double)(-sensitivity), 0.0, (double)(-sensitivity)))
                .iterator()
                .hasNext();
    }

    @EventTarget
    public void onFoV(EventUpdateFoV e) {
        if (this.keepFoV.getCurrentValue()) {
            if (this.cancelSprintFovChange.getCurrentValue()) {
                e.setFov(this.speedFov.getCurrentValue());
                return;
            }

            float baseFov = e.getFov();
            float moveBonus = MoveUtils.isMoving() ? (float) PlayerUtils.getMoveSpeedEffectAmplifier() * 00.13F : 0.0F;
            float yawDelta = Math.abs(this.rots.getX() - this.lastRots.getX());
            float pitchDelta = Math.abs(this.rots.getY() - this.lastRots.getY());
            float rotationSpeed = (float) Math.sqrt(yawDelta * yawDelta + pitchDelta * pitchDelta);
            float normalized = Math.min(rotationSpeed / 180.0F, 1.0F);
            float rotationFoV = normalized * 180.0F * this.speedFov.getCurrentValue();
            e.setFov(baseFov + moveBonus + rotationFoV);
        }
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            this.oldSlot = mc.player.getInventory().selected;
            this.rots.set(mc.player.getYRot() - 180.0F, mc.player.getXRot());
            this.lastRots.set(mc.player.yRotO - 180.0F, mc.player.xRotO);
            this.pos = null;
            this.baseY = 10000;
            this.rescueTicks = 0;
            this.isRescuing = false;
            if (this.randomOffGroundTicks.getCurrentValue()) {
                this.originalOffGroundTicks = (this.maxOffGroundTicks.getCurrentValue() + this.minOffGroundTicks.getCurrentValue()) / 2.0F;
            } else {
                this.originalOffGroundTicks = this.offGroundTicksValue.getCurrentValue();
            }
            this.lastY = mc.player.getY();
            this.fallDetectionTicks = 0;
            this.lastVelocity = mc.player.getDeltaMovement();
            this.lastAcceleration = Vec3.ZERO;
            this.initialBlockCount = getBlockCount();
        }
    }

    @Override
    public void onDisable() {
        boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
        boolean isHoldingShift = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyShift.getKey().getValue());
        mc.options.keyJump.setDown(isHoldingJump);
        mc.options.keyShift.setDown(isHoldingShift);
        mc.options.keyUse.setDown(false);
        mc.player.getInventory().selected = this.oldSlot;
    }

    @EventTarget
    public void onUpdateHeldItem(EventUpdateHeldItem e) {
        if (this.renderItemSpoof.getCurrentValue() && e.getHand() == InteractionHand.MAIN_HAND) {
            e.setItem(mc.player.getInventory().getItem(this.oldSlot));
        }
    }

    @EventTarget(1)
    public void onEventEarlyTick(EventRunTicks e) {
        if (e.getType() == EventType.PRE && mc.screen == null && mc.player != null) {
            int slotID = -1;

            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof BlockItem && isValidStack(stack)) {
                    slotID = i;
                    break;
                }
            }

            if (mc.player.onGround()) {
                this.offGroundTicks = 0;
                this.fallDetectionTicks = 0;
                if (this.isRescuing) {
                    this.isRescuing = false;
                }
            } else {
                this.offGroundTicks++;
            }

            if (this.mode.isCurrentMode("Telly Bridge") && this.smartOffGround.getCurrentValue()) {
                this.performFallDetection();
            }

            if (slotID != -1 && mc.player.getInventory().selected != slotID) {
                mc.player.getInventory().selected = slotID;
            }

            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (this.baseY == -1
                    || this.baseY > (int)Math.floor(mc.player.getY()) - 1
                    || mc.player.onGround()
                    || !PlayerUtils.movementInput()
                    || isHoldingJump
                    || this.mode.isCurrentMode("Normal")) {
                this.baseY = (int)Math.floor(mc.player.getY()) - 1;
            }

            this.getBlockPos();
            if (this.pos != null) {
                this.correctRotation = this.getPlayerYawRotation();
                if (this.mode.isCurrentMode("Normal")) {
                    if (this.stepRotation.getCurrentValue()) {
                        float targetYaw = this.correctRotation.getX();
                        float currentYaw = this.rots.getX();
                        float angleDifference = RotationUtils.getAngleDifference(targetYaw, currentYaw);
                        float stepSize = this.step.getCurrentValue();
                        if (Math.abs(angleDifference) > stepSize) {
                            this.rots.setX(currentYaw + stepSize * Math.signum(angleDifference));
                        } else {
                            this.rots.setX(targetYaw);
                        }
                    } else if (this.snap.getCurrentValue()) {
                        this.rots.setX(this.correctRotation.getX());
                    } else {
                        this.rots.setX(RotationUtils.rotateToYaw(this.rotationSpeed.getCurrentValue(), this.rots.getX(), this.correctRotation.getX()));
                    }
                } else {
                    this.rots.setX(RotationUtils.rotateToYaw(this.rotationSpeed.getCurrentValue(), this.rots.getX(), this.correctRotation.getX()));
                }

                this.rots.setY(this.correctRotation.getY());
            }

            if (this.sneak.getCurrentValue()) {
                this.lastSneakTicks++;
                System.out.println(this.lastSneakTicks);
                if (this.lastSneakTicks == 18) {
                    if (mc.player.isSprinting()) {
                        mc.options.keySprint.setDown(false);
                        mc.player.setSprinting(false);
                    }

                    mc.options.keyShift.setDown(true);
                } else if (this.lastSneakTicks >= 21) {
                    mc.options.keyShift.setDown(false);
                    this.lastSneakTicks = 0;
                }
            }

            if (this.mode.isCurrentMode("Telly Bridge")) {
                mc.options.keyJump.setDown(PlayerUtils.movementInput() || isHoldingJump);
                if (this.offGroundTicks < 1 && PlayerUtils.movementInput()) {
                    float targetYaw = mc.player.getYRot();

                    if (this.acceleration.getCurrentValue() && RandomUtils.nextFloat(0.0F, 1.0F) < this.probability.getCurrentValue()) {
                        Vec3 currentVelocity = mc.player.getDeltaMovement();
                        Vec3 acceleration = currentVelocity.subtract(this.lastVelocity);
                        this.lastAcceleration = acceleration;
                        this.lastVelocity = currentVelocity;
                        

                        double accelerationMagnitude = acceleration.horizontalDistance();
                        float yawOffset = (float) (accelerationMagnitude * this.accelerationStrength.getCurrentValue());
                        float noiseFactor = Math.min(yawOffset * 0.5F, 20.0F);
                        float randomNoise = RandomUtils.nextFloat(-noiseFactor, noiseFactor);
                        targetYaw += randomNoise;
                    } else {
                        this.lastVelocity = mc.player.getDeltaMovement();
                    }


                    this.rots.setX(RotationUtils.rotateToYaw(this.rotationSpeed.getCurrentValue(), this.rots.getX(), targetYaw));
                    this.lastRots.set(this.rots.getX(), this.rots.getY());
                    return;
                }
            } else {
                if (this.eagle.getCurrentValue()) {
                    mc.options.keyShift.setDown(mc.player.onGround() && isOnBlockEdge(0.3F));
                }

                if (this.snap.getCurrentValue() && !isHoldingJump) {
                    this.doSnap();
                }
            }

            this.lastRots.set(this.rots.getX(), this.rots.getY());
        }
    }

    private void doSnap() {
        boolean shouldPlaceBlock = false;
        HitResult objectPosition = RayTraceUtils.rayCast(1.0F, this.rots);
        if (objectPosition.getType() == Type.BLOCK) {
            BlockHitResult position = (BlockHitResult)objectPosition;
            if (position.getBlockPos().equals(this.pos) && position.getDirection() != Direction.UP) {
                shouldPlaceBlock = true;
            }
        }

        if (!shouldPlaceBlock) {
            this.rots.setX(mc.player.getYRot() + RandomUtils.nextFloat(0.0F, 0.5F) - 0.25F);
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        e.setCancelled(true);
        float currentThreshold;
        if (this.randomOffGroundTicks.getCurrentValue()) {
            currentThreshold = (this.maxOffGroundTicks.getCurrentValue() + this.minOffGroundTicks.getCurrentValue()) / 2.0F;
        } else {
            currentThreshold = this.offGroundTicksValue.getCurrentValue();
        }
        if (mc.screen == null && mc.player != null && this.pos != null && (!this.mode.isCurrentMode("Telly Bridge") || this.offGroundTicks >= currentThreshold)) {
            if (!this.checkPlace(this.pos)) {
                return;
            }

            this.placeBlock();

            this.lastPlaceTime = System.currentTimeMillis();
        }
    }

    private boolean checkPlace(Scaffold.BlockPosWithFacing data) {
        Vec3 center = new Vec3((double)data.position.getX() + 0.5, (double)((float)data.position.getY() + 0.5F), (double)data.position.getZ() + 0.5);
        Vec3 hit = center.add(
                new Vec3((double)data.facing.getNormal().getX() * 0.5, (double)data.facing.getNormal().getY() * 0.5, (double)data.facing.getNormal().getZ() * 0.5)
        );
        Vec3 relevant = hit.subtract(mc.player.getEyePosition());
        return relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(data.facing.getNormal().multiply(-1)).normalize()) >= 0.0;
    }

    private void placeBlock() {
        if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
            Direction sbFace = this.pos.facing();
            boolean isHoldingJump = InputConstants.isKeyDown(mc.getWindow().getWindow(), mc.options.keyJump.getKey().getValue());
            if (sbFace != null
                    && (sbFace != Direction.UP || mc.player.onGround() || !PlayerUtils.movementInput() || isHoldingJump || this.mode.isCurrentMode("Normal"))
                    && this.shouldBuild()) {
                InteractionResult result = mc.gameMode
                        .useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(getVec3(this.pos.position(), sbFace), sbFace, this.pos.position(), false));
                if (result == InteractionResult.SUCCESS) {
                    if (this.swing.getCurrentValue()) {
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                    this.pos = null;
                }
            }
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private Vector2f getPlayerYawRotation() {
        if (mc.player != null && this.pos != null) {
            Vector2f targetRotation = new Vector2f(RotationUtils.getRotations(this.pos.position(), 0.0F).getYaw(), RotationUtils.getRotations(this.pos.position(), 0.0F).getPitch());
            Vector2f currentRotation = new Vector2f(this.rots.getX(), this.rots.getY());
            float yawDifference = Math.abs(RotationUtils.getAngleDifference(targetRotation.getX(), currentRotation.getX()));
            if (yawDifference > 180.0F) {
                float direction = Math.signum(RotationUtils.getAngleDifference(targetRotation.getX(), currentRotation.getX()));
                float limitedYaw = currentRotation.getX() + direction * 180.0F;
                targetRotation.setX(limitedYaw);
            }

            return targetRotation;
        }
        return new Vector2f(0.0F, 0.0F);
    }

    private boolean shouldBuild() {
        BlockPos playerPos = BlockPos.containing(mc.player.getX(), mc.player.getY() - 0.5, mc.player.getZ());
        return mc.level.isEmptyBlock(playerPos) && isValidStack(mc.player.getMainHandItem());
    }

    @FlowExclude
    @ParameterObfuscationExclude
    private void getBlockPos() {
        Vec3 baseVec = mc.player.getEyePosition().add(mc.player.getDeltaMovement().multiply(2.0, 2.0, 2.0));
        if (mc.player.getDeltaMovement().y < 0.01) {
            FallingPlayer fallingPlayer = new FallingPlayer(mc.player);
            fallingPlayer.calculate(2);
            baseVec = new Vec3(baseVec.x, Math.max(fallingPlayer.y + (double)mc.player.getEyeHeight(), baseVec.y), baseVec.z);
        }

        BlockPos base = BlockPos.containing(baseVec.x, (double)((float)this.baseY + 0.1F), baseVec.z);
        int baseX = base.getX();
        int baseZ = base.getZ();
        if (!mc.level.getBlockState(base).entityCanStandOn(mc.level, base, mc.player)) {
            if (!this.checkBlock(baseVec, base)) {
                for (int d = 1; d <= 6; d++) {
                    if (this.checkBlock(baseVec, new BlockPos(baseX, this.baseY - d, baseZ))) {
                        return;
                    }

                    for (int x = 1; x <= d; x++) {
                        for (int z = 0; z <= d - x; z++) {
                            int y = d - x - z;

                            for (int rev1 = 0; rev1 <= 1; rev1++) {
                                for (int rev2 = 0; rev2 <= 1; rev2++) {
                                    if (this.checkBlock(baseVec, new BlockPos(baseX + (rev1 == 0 ? x : -x), this.baseY - y, baseZ + (rev2 == 0 ? z : -z)))) {
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkBlock(Vec3 baseVec, BlockPos bp) {
        if (!(mc.level.getBlockState(bp).getBlock() instanceof AirBlock)) {
            return false;
        } else {
            Vec3 center = new Vec3((double)bp.getX() + 0.5, (double)((float)bp.getY() + 0.5F), (double)bp.getZ() + 0.5);

            for (Direction sbface : Direction.values()) {
                Vec3 hit = center.add(
                        new Vec3((double)sbface.getNormal().getX() * 0.5, (double)sbface.getNormal().getY() * 0.5, (double)sbface.getNormal().getZ() * 0.5)
                );
                Vec3i baseBlock = bp.offset(sbface.getNormal());
                BlockPos po = new BlockPos(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
                if (mc.level.getBlockState(po).entityCanStandOnFace(mc.level, po, mc.player, sbface)) {
                    Vec3 relevant = hit.subtract(baseVec);
                    if (relevant.lengthSqr() <= 20.25 && relevant.normalize().dot(Vec3.atLowerCornerOf(sbface.getNormal()).normalize()) >= 0.0) {
                        this.pos = new Scaffold.BlockPosWithFacing(new BlockPos(baseBlock), sbface.getOpposite());
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @FlowExclude
    @ParameterObfuscationExclude
    public static Vec3 getVec3(BlockPos pos, Direction face) {
        double x = (double)pos.getX() + 0.5;
        double y = (double)pos.getY() + 0.5;
        double z = (double)pos.getZ() + 0.5;
        if (face != Direction.UP && face != Direction.DOWN) {
            y += 0.08;
        } else {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.WEST || face == Direction.EAST) {
            z += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        if (face == Direction.SOUTH || face == Direction.NORTH) {
            x += MathUtils.getRandomDoubleInRange(0.3, -0.3);
        }

        return new Vec3(x, y, z);
    }

    @EventTarget
    public void onShader(EventShader e) {
        if (this.renderBlockCounter.getCurrentValue() && mc.player != null) {
            float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
            float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

            if (this.blockCounterMode.isCurrentMode("Capsule")) {

                String text = "Blocks: " + getBlockCount();
                double textScale = 0.35;
                this.blockCounterWidth = Fonts.opensans.getWidth(text, textScale) + 20.0F;
                this.blockCounterHeight = (float) Fonts.opensans.getHeight(true, textScale) + 10.0F;

                float x = (screenWidth - this.blockCounterWidth) / 2.0F;
                float y = screenHeight / 2.0F + 15.0F;


                RenderUtils.drawRoundedRect(e.getStack(), x, y, this.blockCounterWidth, this.blockCounterHeight, 6.0F, Integer.MIN_VALUE);
            } else {

                float x = (screenWidth - this.blockCounterWidth) / 2.0F - 3.0F;
                float y = screenHeight / 2.0F + 15.0F;
                RenderUtils.drawRoundedRect(e.getStack(), x, y, this.blockCounterWidth + 6.0F, this.blockCounterHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        if (this.renderBlockCounter.getCurrentValue() && mc.player != null) {
            if (this.blockCounterMode.isCurrentMode("Capsule")) {
                renderCapsuleBlockCounter(e);
            } else {
                renderNormalBlockCounter(e);
            }
        }
    }

    
    private void renderNormalBlockCounter(EventRender2D e) {
        int blockCount = getBlockCount();


        ItemStack itemToRender = mc.player.getMainHandItem();

        if (!isValidStack(itemToRender)) {
            itemToRender = null;
        }

        String text = "Blocks: " + blockCount;
        double backgroundScale = 0.4;
        double textScale = 0.35;


        float iconWidth = itemToRender != null ? 18.0f : 0;
        this.blockCounterWidth = Fonts.opensans.getWidth(text, backgroundScale) + iconWidth;
        this.blockCounterHeight = (float) Fonts.opensans.getHeight(true, backgroundScale);

        float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
        float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

        float backgroundX = (screenWidth - this.blockCounterWidth) / 2.0F - 3.0F;
        float backgroundY = screenHeight / 2.0F + 15.0F;

        float textWidth = Fonts.opensans.getWidth(text, textScale);
        float textHeight = (float) Fonts.opensans.getHeight(true, textScale);


        float textX = backgroundX + iconWidth + (this.blockCounterWidth - iconWidth + 6.0F - textWidth) / 2.0F;
        float textY = backgroundY + 4.0F + (this.blockCounterHeight + 4.0F) / 2.0F - textHeight / 2.0F - 2.0F;

        e.getStack().pushPose();

        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), backgroundX, backgroundY, this.blockCounterWidth + 6.0F, this.blockCounterHeight + 8.0F, 5.0F, Integer.MIN_VALUE);
        StencilUtils.erase(true);
        int headerColor = new Color(150, 45, 45, 255).getRGB();
        RenderUtils.fill(e.getStack(), backgroundX, backgroundY, backgroundX + this.blockCounterWidth + 6.0F, backgroundY + 3.0F, headerColor);

        int bodyColor = new Color(0, 0, 0, 120).getRGB();
        RenderUtils.fill(e.getStack(), backgroundX, backgroundY + 3.0F, backgroundX + this.blockCounterWidth + 6.0F, backgroundY + this.blockCounterHeight + 8.0F, bodyColor);


        if (itemToRender != null) {
            float itemX = backgroundX + 4;
            float itemY = backgroundY + 4.0F + (this.blockCounterHeight / 2.0F) - 8.0F;
            e.getGuiGraphics().renderFakeItem(itemToRender, (int)itemX, (int)itemY);
        }


        Fonts.opensans.render(e.getStack(), text, textX, textY, Color.WHITE, true, textScale);
        StencilUtils.dispose();
        e.getStack().popPose();
    }

    
    private void renderCapsuleBlockCounter(EventRender2D e) {
        int blockCount = getBlockCount();
        String text = "Blocks: " + blockCount;
        double textScale = 0.35;

        this.blockCounterWidth = Fonts.opensans.getWidth(text, textScale) + 20.0F;
        this.blockCounterHeight = (float) Fonts.opensans.getHeight(true, textScale) + 10.0F;

        float screenWidth = (float) mc.getWindow().getGuiScaledWidth();
        float screenHeight = (float) mc.getWindow().getGuiScaledHeight();

        float x = (screenWidth - this.blockCounterWidth) / 2.0F;
        float y = screenHeight / 2.0F + 15.0F;

        float cornerRadius = 6.0F;
        float borderWidth = 1.5F;


        float ratio = this.initialBlockCount > 0 ? (float) blockCount / (float) this.initialBlockCount : 0.0F;


        Color progressColor;
        if (ratio >= 0.6F) {
            progressColor = new Color(50, 200, 50, 255);
        } else if (ratio >= 0.4F) {
            progressColor = new Color(255, 200, 50, 255);
        } else {
            progressColor = new Color(200, 50, 50, 255);
        }

        e.getStack().pushPose();



        float perimeter = 2 * (this.blockCounterWidth + this.blockCounterHeight) - 8 * cornerRadius + 2 * (float) Math.PI * cornerRadius;
        float progressLength = perimeter * ratio;


        StencilUtils.write(false);
        RenderUtils.drawRoundedRect(e.getStack(), x, y, this.blockCounterWidth, this.blockCounterHeight, cornerRadius, Integer.MIN_VALUE);
        StencilUtils.erase(true);

        drawProgressBorder(e.getStack(), x, y, this.blockCounterWidth, this.blockCounterHeight, cornerRadius, borderWidth, progressLength, progressColor.getRGB());
        StencilUtils.dispose();


        float textWidth = Fonts.opensans.getWidth(text, textScale);
        float textHeight = (float) Fonts.opensans.getHeight(true, textScale);
        float textX = x + (this.blockCounterWidth - textWidth) / 2.0F;
        float textY = y + (this.blockCounterHeight - textHeight) / 2.0F;

        Fonts.opensans.render(e.getStack(), text, textX, textY, Color.WHITE, true, textScale);

        e.getStack().popPose();
    }

    
    private void drawProgressBorder(com.mojang.blaze3d.vertex.PoseStack stack, float x, float y, float width, float height, float radius, float borderWidth, float length, int color) {
        float currentLength = 0.0F;


        float topLength = (width - 2 * radius) / 2.0F;
        if (currentLength < length) {
            float drawLength = Math.min(topLength, length - currentLength);
            RenderUtils.fill(stack, x + width / 2.0F, y, x + width / 2.0F + drawLength, y + borderWidth, color);
            currentLength += drawLength;
        }


        float cornerArcLength = (float) (Math.PI * radius / 2.0F);
        if (currentLength < length) {
            float drawLength = Math.min(cornerArcLength, length - currentLength);
            int segments = (int)(drawLength / cornerArcLength * 90);
            drawPartialCornerArc(stack, x + width - radius, y + radius, radius, 270, 270 + segments, borderWidth, color);
            currentLength += drawLength;
        }


        float rightLength = height - 2 * radius;
        if (currentLength < length) {
            float drawLength = Math.min(rightLength, length - currentLength);
            RenderUtils.fill(stack, x + width - borderWidth, y + radius, x + width, y + radius + drawLength, color);
            currentLength += drawLength;
        }


        if (currentLength < length) {
            float drawLength = Math.min(cornerArcLength, length - currentLength);
            int segments = (int)(drawLength / cornerArcLength * 90);
            drawPartialCornerArc(stack, x + width - radius, y + height - radius, radius, 0, segments, borderWidth, color);
            currentLength += drawLength;
        }


        float bottomLength = width - 2 * radius;
        if (currentLength < length) {
            float drawLength = Math.min(bottomLength, length - currentLength);
            RenderUtils.fill(stack, x + width - radius - drawLength, y + height - borderWidth, x + width - radius, y + height, color);
            currentLength += drawLength;
        }


        if (currentLength < length) {
            float drawLength = Math.min(cornerArcLength, length - currentLength);
            int segments = (int)(drawLength / cornerArcLength * 90);
            drawPartialCornerArc(stack, x + radius, y + height - radius, radius, 90, 90 + segments, borderWidth, color);
            currentLength += drawLength;
        }


        float leftLength = height - 2 * radius;
        if (currentLength < length) {
            float drawLength = Math.min(leftLength, length - currentLength);
            RenderUtils.fill(stack, x, y + height - radius - drawLength, x + borderWidth, y + height - radius, color);
            currentLength += drawLength;
        }


        if (currentLength < length) {
            float drawLength = Math.min(cornerArcLength, length - currentLength);
            int segments = (int)(drawLength / cornerArcLength * 90);
            drawPartialCornerArc(stack, x + radius, y + radius, radius, 180, 180 + segments, borderWidth, color);
            currentLength += drawLength;
        }


        float topLeftLength = (width - 2 * radius) / 2.0F;
        if (currentLength < length) {
            float drawLength = Math.min(topLeftLength, length - currentLength);
            RenderUtils.fill(stack, x + width / 2.0F - drawLength, y, x + width / 2.0F, y + borderWidth, color);
        }
    }

    
    private void drawPartialCornerArc(com.mojang.blaze3d.vertex.PoseStack stack, float centerX, float centerY, float radius, int startAngle, int endAngle, float width, int color) {
        int degSegments = Math.abs(endAngle - startAngle);
        if (degSegments <= 0) return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f matrix = stack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        float innerRadius = Math.max(0.0F, radius - width);
        for (int i = 0; i <= degSegments; i++) {
            double angle = Math.toRadians(startAngle + i);
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);

            float ox = centerX + cos * radius;
            float oy = centerY + sin * radius;
            float ix = centerX + cos * innerRadius;
            float iy = centerY + sin * innerRadius;

            float a = ((color >> 24) & 0xFF) / 255.0F;
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            buffer.vertex(matrix, ox, oy, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, ix, iy, 0).color(r, g, b, a).endVertex();
        }
        Tesselator.getInstance().end();
    }

    private void performFallDetection() {
        if (mc.player == null) return;

        double currentY = mc.player.getY();
        double fallSpeed = this.lastY - currentY;
        this.lastY = currentY;

        if (fallSpeed > this.fallDetectionThreshold.getCurrentValue() && !mc.player.onGround()) {
            this.fallDetectionTicks++;

            if (this.fallDetectionTicks >= 3 && !this.isRescuing) {
                this.triggerRescue();
            }
        } else {
            this.fallDetectionTicks = 0;
        }

        if (this.isRescuing) {
            this.rescueTicks++;

            if (this.rescueTicks >= this.rescueDelay.getCurrentValue() * 20) {
                this.isRescuing = false;
                this.rescueTicks = 0;
            }
        }
    }

    private void triggerRescue() {
        this.isRescuing = true;
        this.rescueTicks = 0;

        if (this.pos != null && isValidStack(mc.player.getMainHandItem())) {
            this.placeBlock();
        }
    }

    public static record BlockPosWithFacing(BlockPos position, Direction facing) {
    }
}