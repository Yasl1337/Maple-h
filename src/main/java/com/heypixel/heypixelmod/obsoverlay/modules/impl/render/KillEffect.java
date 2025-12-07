package com.heypixel.heypixelmod.obsoverlay.modules.impl.render;

import com.heypixel.heypixelmod.obsoverlay.modules.Category;
import com.heypixel.heypixelmod.obsoverlay.modules.Module;
import com.heypixel.heypixelmod.obsoverlay.modules.ModuleInfo;
import com.heypixel.heypixelmod.obsoverlay.values.ValueBuilder;
import com.heypixel.heypixelmod.obsoverlay.values.impl.BooleanValue;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@ModuleInfo(name = "KillEffect", description = "killeffect.description", category = Category.RENDER)
public final class KillEffect extends Module {

    private final BooleanValue lightning = ValueBuilder.create(this, "Lightning")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();
    private final BooleanValue explosion = ValueBuilder.create(this, "Explosion")
            .setDefaultBooleanValue(true)
            .build()
            .getBooleanValue();

    private LivingEntity target;

    @Override
    public void onEnable() {
        super.onEnable();
        // 注册到事件总线
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // 从事件总线取消注册
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
        target = null;
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (!isEnabled()) return;

        final Entity entity = event.getTarget();
        if (entity instanceof LivingEntity) {
            target = (LivingEntity) entity;
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!isEnabled() || target == null) return;

        if (event.getEntity().equals(target)) {
            if (this.lightning.getCurrentValue()) {
                // 正确创建 LightningBolt 实体
                LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(mc.level);
                if (lightningBolt != null) {
                    lightningBolt.moveTo(target.getX(), target.getY(), target.getZ());
                    mc.level.addFreshEntity(lightningBolt);
                    mc.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT, 1.0F, 1.0F);
                }
            }

            if (this.explosion.getCurrentValue()) {
                for (int i = 0; i <= 8; i++) {
                    mc.level.addParticle(ParticleTypes.FLAME,
                            target.getX(), target.getY(), target.getZ(),
                            0, 0, 0);
                }
                mc.level.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.FIRECHARGE_USE, SoundSource.AMBIENT, 1.0F, 1.0F);
            }

            this.target = null;
        }
    }
}