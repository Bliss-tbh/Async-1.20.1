package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityMixin extends Entity {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapMethod(method = "onDeath")
    private synchronized void onDeath(DamageSource damageSource, Operation<Void> original) {
        original.call(damageSource);
    }

    @WrapMethod(method = "dropLoot")
    private synchronized void dropLoot(DamageSource damageSource, boolean causedByPlayer, Operation<Void> original) {
        original.call(damageSource, causedByPlayer);
    }

    @WrapMethod(method = "tickStatusEffects")
    private synchronized void tickStatusEffects(Operation<Void> original) {
        original.call();
    }

    @WrapMethod(method = "knockback")
    private void knockback(LivingEntity target, Operation<Void> original) {
        synchronized (lock) {
            original.call(target);
        }
    }

    @WrapMethod(method = "tickStatusEffects")
    private void tickStatusEffects(Operation<Void> original) {
        synchronized (lock) {
            original.call();
        }
    }

    @Inject(method = "isClimbing", at = @At("HEAD"), cancellable = true)
    private void isClimbing(CallbackInfoReturnable<Boolean> cir) {
        BlockState blockState = this.getBlockStateAtPos();
        if (blockState == null) cir.setReturnValue(false);
    }
}
