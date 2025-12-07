package com.heypixel.heypixelmod.obsoverlay.modules.impl.combat;

import com.heypixel.heypixelmod.obsoverlay.Naven;
import com.heypixel.heypixelmod.obsoverlay.events.api.EventTarget;
import com.heypixel.heypixelmod.obsoverlay.events.api.types.EventType;
import com.heypixel.heypixelmod.obsoverlay.events.impl.*;
import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.KillSay;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.misc.Teams;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Blink;
import com.heypixel.heypixelmod.obsoverlay.modules.impl.move.Stuck;
import com.heypixel.heypixelmod.obsoverlay.ui.targethud.TargetHUD;
import com.heypixel.heypixelmod.obsoverlay.utils.*;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationManager;
import com.heypixel.heypixelmod.obsoverlay.utils.rotation.RotationUtils;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.FloatValue;
import com.heypixel.heypixelmod.obsoverlay.values.impl.ModeValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@ModuleInfo(
        name = "KillAura",
        description = "Automatically attacks entities",
        category = Category.COMBAT
)
public class Aura extends Module {
    private static final float[] targetColorRed = new float[]{0.78431374F, 0.0F, 0.0F, 0.23529412F};
    private static final float[] targetColorGreen = new float[]{0.0F, 0.78431374F, 0.0F, 0.23529412F};
    public static Entity target;
    public static Entity aimingTarget;
    public static List<Entity> targets = new ArrayList<>();
    public static Vector2f rotation;
    BooleanValue targetHud = ValueBuilder.create(this, "Target HUD").setDefaultBooleanValue(true).build().getBooleanValue();
    ModeValue targetHudStyle = ValueBuilder.create(this, "Target HUD Style")
            .setModes("Naven", "New", "MoonLight", "Rise", "Exhibition", "Capsule")
            .setDefaultModeIndex(0)
            .setVisibility(() -> Aura.this.targetHud.getCurrentValue())
            .build()
            .getModeValue();
    BooleanValue targetEsp = ValueBuilder.create(this, "Target ESP").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackPlayer = ValueBuilder.create(this, "Attack Player").setDefaultBooleanValue(true).build().getBooleanValue();
    BooleanValue attackInvisible = ValueBuilder.create(this, "Attack Invisible").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackFriends = ValueBuilder.create(this, "Attack Friends").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackAnimals = ValueBuilder.create(this, "Attack Animals").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue attackMobs = ValueBuilder.create(this, "Attack Mobs").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue multi = ValueBuilder.create(this, "Multi Attack").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue infSwitch = ValueBuilder.create(this, "Infinity Switch").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue preferBaby = ValueBuilder.create(this, "Prefer Baby").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue moreParticles = ValueBuilder.create(this, "More Particles").setDefaultBooleanValue(false).build().getBooleanValue();
    public BooleanValue fakeAutoblock = ValueBuilder.create(this, "Fake Autoblock").setDefaultBooleanValue(false).build().getBooleanValue();
    BooleanValue blockSprintFov = ValueBuilder.create(this, "Block Sprint FOV").setDefaultBooleanValue(true).build().getBooleanValue();
    FloatValue aimRange = ValueBuilder.create(this, "Aim Range")
            .setDefaultFloatValue(5.0F)
            .setFloatStep(0.1F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(6.0F)
            .build()
            .getFloatValue();

    FloatValue attackCpsMax = ValueBuilder.create(this, "Attack CPS Max")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();
    FloatValue attackCpsMin = ValueBuilder.create(this, "Attack CPS Min")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(20.0F)
            .build()
            .getFloatValue();

    BooleanValue checkCooldown = ValueBuilder.create(this, "Cooldown")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    FloatValue cooldownThreshold = ValueBuilder.create(this, "Cooldown Threshold")
            .setVisibility(checkCooldown::getCurrentValue)
            .setDefaultFloatValue(0.95F)
            .setFloatStep(0.01F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(1.0F)
            .build()
            .getFloatValue();

    BooleanValue advancedCPS = ValueBuilder.create(this, "AdvancedCPS")
            .setDefaultBooleanValue(false)
            .build()
            .getBooleanValue();

    FloatValue getCPSDelay = ValueBuilder.create(this, "Get CPS Delay")
            .setVisibility(advancedCPS::getCurrentValue)
            .setDefaultFloatValue(5.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(50.0F)
            .build()
            .getFloatValue();

    FloatValue switchSize = ValueBuilder.create(this, "Switch Size")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(5.0F)
            .setVisibility(() -> !this.infSwitch.getCurrentValue())
            .build()
            .getFloatValue();
    FloatValue switchAttackTimes = ValueBuilder.create(this, "Switch Delay (Attack Times)")
            .setDefaultFloatValue(1.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();
    FloatValue fov = ValueBuilder.create(this, "Fov")
            .setDefaultFloatValue(360.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(10.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    FloatValue hurtTime = ValueBuilder.create(this, "Hurt Time")
            .setDefaultFloatValue(10.0F)
            .setFloatStep(1.0F)
            .setMinFloatValue(0.0F)
            .setMaxFloatValue(10.0F)
            .build()
            .getFloatValue();

    FloatValue rotationSpeedMax = ValueBuilder.create(this, "RotationSpeedMax")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(2.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    FloatValue rotationSpeedMin = ValueBuilder.create(this, "RotationSpeedMin")
            .setDefaultFloatValue(180.0F)
            .setFloatStep(2.0F)
            .setMinFloatValue(1.0F)
            .setMaxFloatValue(360.0F)
            .build()
            .getFloatValue();
    public ModeValue TargetESPStyle = ValueBuilder.create(this, "TargetEsp Style")
            .setVisibility(this.targetEsp::getCurrentValue)
            .setDefaultModeIndex(0)
            .setModes("Naven", "Nitro","Colorful")
            .build()
            .getModeValue();
    ModeValue priority = ValueBuilder.create(this, "Priority").setModes("Health", "Fov", "Range", "None").build().getModeValue();
    RotationUtils.Data lastRotationData;
    RotationUtils.Data rotationData;
    int attackTimes = 0;
    float attacks = 0.0F;
    private int index;
    private Vector4f blurMatrix;
    private final Random random = new Random();

    private long lastCpsUpdate = 0;
    private float currentCps = 0.0F;
    private boolean cpsInitialized = false;


    private float baseFov = 1.0F;
    private long lastAttackTime = 0;
    private static final long FOV_BLOCK_DURATION = 500;

    @EventTarget
    public void onShader(EventShader e) {
        if (this.blurMatrix != null && this.targetHud.getCurrentValue()) {
            RenderUtils.drawRoundedRect(e.getStack(), this.blurMatrix.x(), this.blurMatrix.y(), this.blurMatrix.z(), this.blurMatrix.w(), 3.0F, 1073741824);
        }
    }

    public static Entity getTarget() {
        return target;
    }

    @EventTarget
    public void onFovUpdate(EventUpdateFoV e) {
        if (this.blockSprintFov.getCurrentValue() && mc.player != null) {
            long currentTime = System.currentTimeMillis();


            if (currentTime - lastAttackTime < FOV_BLOCK_DURATION) {
                e.setFov(baseFov);
                return;
            }


            if (target != null && aimingTarget != null) {

                if (mc.player.isSprinting()) {
                    baseFov = e.getFov();
                } else {

                    baseFov = e.getFov() * 1.3F;
                }
                e.setFov(baseFov);
            }
        }
    }

    @EventTarget
    public void onRender(EventRender2D e) {
        this.blurMatrix = null;
        if (target instanceof LivingEntity && this.targetHud.getCurrentValue()) {
            LivingEntity living = (LivingEntity)target;
            e.getStack().pushPose();


            com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor.HUDElement targetHudElement =
                    com.heypixel.heypixelmod.obsoverlay.ui.HUDEditor.getInstance().getHUDElement("targethud");

            float x, y;
            if (targetHudElement != null) {
                x = (float)targetHudElement.x;
                y = (float)targetHudElement.y;
            } else {

                x = (float)mc.getWindow().getGuiScaledWidth() / 2.0F + 10.0F;
                y = (float)mc.getWindow().getGuiScaledHeight() / 2.0F + 10.0F;
            }


            this.blurMatrix = TargetHUD.render(e.getGuiGraphics(), living, this.targetHudStyle.getCurrentMode(), x, y);


            if (targetHudElement != null && this.blurMatrix != null) {
                targetHudElement.width = this.blurMatrix.z();
                targetHudElement.height = this.blurMatrix.w();
            }

            e.getStack().popPose();
        }
    }

    @EventTarget
    public void onRender(EventRender e) {
        if (this.targetEsp.getCurrentValue()) {
            PoseStack stack = e.getPMatrixStack();
            float partialTicks = e.getRenderPartialTicks();
            stack.pushPose();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glEnable(2848);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderUtils.applyRegionalRenderOffset(stack);

            for (Entity entity : targets) {
                if (entity instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) entity;
                    float[] color = target == living ? targetColorRed : targetColorGreen;
                    stack.pushPose();
                    RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
                    double motionX = entity.getX() - entity.xo;
                    double motionY = entity.getY() - entity.yo;
                    double motionZ = entity.getZ() - entity.zo;
                    AABB boundingBox = entity.getBoundingBox()
                            .move(-motionX, -motionY, -motionZ)
                            .move((double)partialTicks * motionX, (double)partialTicks * motionY, (double)partialTicks * motionZ);
                    RenderUtils.drawSolidBox(boundingBox, stack);
                    stack.popPose();
                }
            }

            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(2848);
            stack.popPose();
            com.heypixel.heypixelmod.obsoverlay.ui.targethud.TargetESP.render(
                    e,
                    targets,
                    target,
                    this.TargetESPStyle.getCurrentMode()
            );
        }
    }

    @Override
    public void onEnable() {
        rotation = null;
        this.index = 0;
        target = null;
        aimingTarget = null;
        targets.clear();


        this.lastCpsUpdate = 0;
        this.currentCps = 0.0F;
        this.cpsInitialized = false;


        this.baseFov = 1.0F;
        this.lastAttackTime = 0;
    }

    @Override
    public void onDisable() {
        target = null;
        aimingTarget = null;


        this.baseFov = 1.0F;
        this.lastAttackTime = 0;

        super.onDisable();
    }

    @EventTarget
    public void onRespawn(EventRespawn e) {
        target = null;
        aimingTarget = null;
        this.toggle();
    }

    @EventTarget
    public void onMotion(EventRunTicks event) {
        if (event.getType() == EventType.PRE && mc.player != null) {
            
            if (mc.screen instanceof AbstractContainerScreen
                    || Naven.getInstance().getModuleManager().getModule(Stuck.class).isEnabled()
                    || InventoryUtils.shouldDisableFeatures()) {
                target = null;
                aimingTarget = null;
                this.rotationData = null;
                rotation = null;
                this.lastRotationData = null;
                targets.clear();
                return;
            }

            boolean isSwitch = this.switchSize.getCurrentValue() > 1.0F;
            this.setSuffix(this.multi.getCurrentValue() ? "Multi" : (isSwitch ? "Switch" : "Single"));
            this.updateAttackTargets();
            aimingTarget = this.shouldPreAim();
            this.lastRotationData = this.rotationData;
            this.rotationData = null;
            if (aimingTarget != null) {
                this.rotationData = RotationUtils.getRotationDataToEntity(aimingTarget);
                if (this.rotationData.getRotation() != null) {
                    rotation = this.rotationData.getRotation();
                } else {
                    rotation = null;
                }
            }

            if (targets.isEmpty()) {
                target = null;
                return;
            }

            if (this.index > targets.size() - 1) {
                this.index = 0;
            }

            if (targets.size() > 1
                    && ((float)this.attackTimes >= this.switchAttackTimes.getCurrentValue() || this.rotationData != null && this.rotationData.getDistance() > 3.0)) {
                this.attackTimes = 0;

                for (int i = 0; i < targets.size(); i++) {
                    this.index++;
                    if (this.index > targets.size() - 1) {
                        this.index = 0;
                    }

                    Entity nextTarget = targets.get(this.index);
                    RotationUtils.Data data = RotationUtils.getRotationDataToEntity(nextTarget);
                    if (data.getDistance() < 3.0) {
                        break;
                    }
                }
            }

            if (this.index > targets.size() - 1 || !isSwitch) {
                this.index = 0;
            }

            target = targets.get(this.index);

            this.attacks = this.attacks + this.getRandomCps() / 20.0F;
        }
    }

    @EventTarget
    public void onClick(EventClick e) {
        if (mc.player.getUseItem().isEmpty()
                && mc.screen == null
                && Naven.skipTasks.isEmpty()
                && !NetworkUtils.isServerLag()
                && !Naven.getInstance().getModuleManager().getModule(Blink.class).isEnabled()) {
            while (this.attacks >= 1.0F) {
                if (isCooldownReady()) {
                    this.doAttack();
                    this.attacks--;
                } else {
                    break;
                }
            }
        }
    }

    private boolean isCooldownReady() {
        if (!this.checkCooldown.getCurrentValue()) {
            return true;
        }
        return mc.player.getAttackStrengthScale(0.0F) >= this.cooldownThreshold.getCurrentValue();
    }

    public boolean shouldAutoBlock() {
        return this.isEnabled() && this.fakeAutoblock.getCurrentValue() && aimingTarget != null;
    }

    public Entity shouldPreAim() {
        Entity target = Aura.target;
        if (target == null) {
            List<Entity> aimTargets = this.getTargets();
            if (!aimTargets.isEmpty()) {
                target = aimTargets.get(0);
            }
        }

        return target;
    }

    public void doAttack() {
        if (!targets.isEmpty()) {
            HitResult hitResult = mc.hitResult;
            if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult)hitResult;
                if (AntiBots.isBot(result.getEntity())) {
                    ChatUtils.addChatMessage("Attacking Bot!");
                    return;
                }
            }

            if (this.multi.getCurrentValue()) {
                int attacked = 0;

                for (Entity entity : targets) {
                    if (RotationUtils.getDistance(entity, mc.player.getEyePosition(), RotationManager.rotations) < 3.0) {
                        this.attackEntity(entity);
                        if (++attacked >= 2) {
                            break;
                        }
                    }
                }
            } else if (hitResult.getType() == Type.ENTITY) {
                EntityHitResult result = (EntityHitResult)hitResult;
                this.attackEntity(result.getEntity());
            }
        }
    }

    public void updateAttackTargets() {
        targets = this.getTargets();
    }

    public boolean isValidTarget(Entity entity) {
        if (entity == mc.player) {
            return false;
        } else if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            if (living instanceof BlinkingPlayer) {
                return false;
            } else {
                AntiBots module = (AntiBots)Naven.getInstance().getModuleManager().getModule(AntiBots.class);
                if (module == null || !module.isEnabled() || !AntiBots.isBot(entity) && !AntiBots.isBedWarsBot(entity)) {
                    if (Teams.isSameTeam(living)) {
                        return false;
                    } else if (FriendManager.isFriend(living) && !this.attackFriends.getCurrentValue()) {
                        return false;
                    } else if (living.isDeadOrDying() || living.getHealth() <= 0.0F) {
                        return false;
                    } else if (entity instanceof ArmorStand) {
                        return false;
                    } else if (entity.isInvisible() && !this.attackInvisible.getCurrentValue()) {
                        return false;
                    } else if (entity instanceof Player && !this.attackPlayer.getCurrentValue()) {
                        return false;
                    } else if (!(entity instanceof Player) || !((double)entity.getBbWidth() < 0.5) && !living.isSleeping()) {
                        if ((entity instanceof Mob || entity instanceof Slime || entity instanceof Bat || entity instanceof AbstractGolem)
                                && !this.attackMobs.getCurrentValue()) {
                            return false;
                        } else if ((entity instanceof Animal || entity instanceof Squid) && !this.attackAnimals.getCurrentValue()) {
                            return false;
                        } else {
                            return entity instanceof Villager && !this.attackAnimals.getCurrentValue() ? false : !(entity instanceof Player) || !entity.isSpectator();
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public boolean isValidAttack(Entity entity) {
        if (!this.isValidTarget(entity)) {
            return false;
        } else if (entity instanceof LivingEntity && (float)((LivingEntity)entity).hurtTime > this.hurtTime.getCurrentValue()) {
            return false;
        } else {
            Vec3 closestPoint = RotationUtils.getClosestPoint(mc.player.getEyePosition(), entity.getBoundingBox());
            return closestPoint.distanceTo(mc.player.getEyePosition()) > (double)this.aimRange.getCurrentValue()
                    ? false
                    : RotationUtils.inFov(entity, this.fov.getCurrentValue() / 2.0F);
        }
    }

    public void attackEntity(Entity entity) {
        this.attackTimes++;


        this.lastAttackTime = System.currentTimeMillis();

        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        mc.player.setYRot(RotationManager.rotations.x);
        mc.player.setXRot(RotationManager.rotations.y);
        if (entity instanceof Player && !AntiBots.isBot(entity)) {
            KillSay.attackedPlayers.add(entity.getName().getString());
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        if (this.moreParticles.getCurrentValue()) {
            mc.player.magicCrit(entity);
            mc.player.crit(entity);
        }

        mc.player.setYRot(currentYaw);
        mc.player.setXRot(currentPitch);
    }

    private List<Entity> getTargets() {
        Stream<Entity> stream = StreamSupport.<Entity>stream(mc.level.entitiesForRendering().spliterator(), true)
                .filter(entity -> entity instanceof Entity)
                .filter(this::isValidAttack);
        List<Entity> possibleTargets = stream.collect(Collectors.toList());
        if (this.priority.isCurrentMode("Range")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> (double)o.distanceTo(mc.player)));
        } else if (this.priority.isCurrentMode("Fov")) {
            possibleTargets.sort(
                    Comparator.comparingDouble(o -> (double)RotationUtils.getDistanceBetweenAngles(RotationManager.rotations.x, RotationUtils.getRotations(o).x))
            );
        } else if (this.priority.isCurrentMode("Health")) {
            possibleTargets.sort(Comparator.comparingDouble(o -> o instanceof LivingEntity ? (double)((LivingEntity)o).getHealth() : 0.0));
        }

        if (this.preferBaby.getCurrentValue() && possibleTargets.stream().anyMatch(entity -> entity instanceof LivingEntity && ((LivingEntity)entity).isBaby())) {
            possibleTargets.removeIf(entity -> !(entity instanceof LivingEntity) || !((LivingEntity)entity).isBaby());
        }

        possibleTargets.sort(Comparator.comparing(o -> o instanceof EndCrystal ? 0 : 1));
        return this.infSwitch.getCurrentValue()
                ? possibleTargets
                : possibleTargets.subList(0, (int)Math.min((float)possibleTargets.size(), this.switchSize.getCurrentValue()));
    }


    public float getRandomRotationSpeed() {
        float min = Math.min(rotationSpeedMin.getCurrentValue(), rotationSpeedMax.getCurrentValue());
        float max = Math.max(rotationSpeedMin.getCurrentValue(), rotationSpeedMax.getCurrentValue());

        if (min == max) {
            return min;
        }

        return min + random.nextFloat() * (max - min);
    }


    public float getRandomCps() {
        long currentTime = System.currentTimeMillis();


        long cpsDelayMs = advancedCPS.getCurrentValue() ?
                (long)getCPSDelay.getCurrentValue() : 5L;

        if (!this.cpsInitialized || (currentTime - this.lastCpsUpdate) >= cpsDelayMs) {
            this.currentCps = this.generateRandomCps();
            this.lastCpsUpdate = currentTime;
            this.cpsInitialized = true;
        }
        return this.currentCps;
    }


    private float generateRandomCps() {
        float min = Math.min(attackCpsMin.getCurrentValue(), attackCpsMax.getCurrentValue());
        float max = Math.max(attackCpsMin.getCurrentValue(), attackCpsMax.getCurrentValue());

        if (min == max) {
            return min;
        }

        return min + random.nextFloat() * (max - min);
    }
}