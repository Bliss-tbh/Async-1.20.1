package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityMixin extends Entity {

    @Shadow
    final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects = new ConcurrentHashMap<>();

    @Shadow
    protected abstract void onEffectUpdated(MobEffectInstance effect, boolean reapply, Entity source);

    @Shadow
    protected abstract void onEffectRemoved(MobEffectInstance effect);

    @Unique
    private static final Object async$lock = new Object();

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @WrapMethod(method = "die")
    private synchronized void die(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }

    @WrapMethod(method = "dropFromLootTable(Lnet/minecraft/world/damagesource/DamageSource;Z)V")
    private synchronized void dropFromLootTable(DamageSource damageSource, boolean playerKill, Operation<Void> original) {
        original.call(damageSource, playerKill);
    }

    @WrapMethod(method = "knockback")
    private synchronized void knockback(double strength, double x, double z, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(strength, x, z);
        }
    }

    @WrapMethod(method = "tickEffects")
    private void tickStatusEffects(Operation<Void> original) {
        synchronized (async$lock) {
            if (this.level() instanceof ServerLevel serverlevel) {
                List<Holder<MobEffect>> effectsToTick = new ArrayList<>(this.activeEffects.keySet());

                for (Holder<MobEffect> holder : effectsToTick) {
                    MobEffectInstance mobeffectinstance = this.activeEffects.get(holder);

                    if (mobeffectinstance != null) {
                        if (!mobeffectinstance.tick((LivingEntity)(Object)this,
                                () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
                            this.activeEffects.remove(holder);
                            this.onEffectRemoved(mobeffectinstance);
                        } else if (mobeffectinstance.getDuration() % 600 == 0) {
                            this.onEffectUpdated(mobeffectinstance, false, null);
                        }
                    }
                }
            } else {
                original.call();
            }
        }
    }

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean addEffect(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        synchronized (async$lock) {
            return effect != null ? original.call(effect, source) : false;
        }
    }

    @WrapMethod(method = "removeEffect")
    private boolean removeEffect(MobEffect effect, Operation<Boolean> original) {
        synchronized (async$lock) {
            return effect != null ? original.call(effect) : false;
        }
    }

    @WrapMethod(method = "hasEffect")
    public boolean hasEffect(MobEffect effect, Operation<Boolean> original) {
        return effect != null ? original.call(effect) : false;
    }

    @WrapMethod(method = "removeAllEffects")
    private boolean removeAllEffects(Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void causeFallDamage(float fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        BlockPos pos = new BlockPos(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
        BlockState currentBlock = this.level().getBlockState(pos);

        if (currentBlock.is(BlockTags.CLIMBABLE)) {
            cir.setReturnValue(false);
        }
    }
}
