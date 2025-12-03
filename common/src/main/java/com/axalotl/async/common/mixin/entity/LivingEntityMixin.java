package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.Holder;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityMixin extends Entity {
    @Mutable
    @Shadow
    @Final
    private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(EntityType<?> entityType, Level level, CallbackInfo ci) {
        this.activeEffects = new ConcurrentHashMap<>();
    }

    @WrapMethod(method = "die")
    private synchronized void die(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }

    @WrapMethod(method = "dropFromLootTable")
    private synchronized void dropFromLootTable(DamageSource damageSource, boolean causedByPlayer, Operation<Void> original) {
        original.call(damageSource, causedByPlayer);
    }

    @WrapMethod(method = "blockedByShield")
    private void blockedByShield(LivingEntity target, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(target);
        }
    }

    @WrapMethod(method = "tickEffects")
    private void tickEffects(Operation<Void> original) {
        synchronized (async$lock) {
            original.call();
        }
    }

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean addEffect(MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(effect, source);
        }
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void onClimbable(CallbackInfoReturnable<Boolean> cir) {
        BlockState blockState = this.getFeetBlockState();
        if (blockState == null) cir.setReturnValue(false);
    }
}
