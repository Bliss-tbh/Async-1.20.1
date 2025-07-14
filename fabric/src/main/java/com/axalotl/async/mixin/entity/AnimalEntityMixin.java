package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends Entity {
    @Unique
    private final AtomicBoolean breedingFlag = new AtomicBoolean(false);
    @Unique
    private final AtomicBoolean breedingBabyFlag = new AtomicBoolean(false);

    public AnimalEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapMethod(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V")
    private void breed(ServerWorld world, AnimalEntity other, Operation<Void> original) {
        if (this.getId() > other.getId()) return;
        AnimalEntityMixin otherMixin = (AnimalEntityMixin) (Object) other;
        if (this.breedingFlag.compareAndSet(false, true) && otherMixin.breedingFlag.compareAndSet(false, true)) {
            try {
                original.call(world, other);
            } finally {
                this.breedingFlag.set(false);
                otherMixin.breedingFlag.set(false);
            }
        }
    }

    @WrapMethod(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;Lnet/minecraft/entity/passive/PassiveEntity;)V")
    private void breed(ServerWorld world, AnimalEntity other, PassiveEntity baby, Operation<Void> original) {
        if (this.getId() > other.getId()) return;
        AnimalEntityMixin otherMixin = (AnimalEntityMixin) (Object) other;
        if (this.breedingBabyFlag.compareAndSet(false, true) && otherMixin.breedingBabyFlag.compareAndSet(false, true)) {
            try {
                original.call(world, other, baby);
            } finally {
                this.breedingBabyFlag.set(false);
                otherMixin.breedingBabyFlag.set(false);
            }
        }
    }
}