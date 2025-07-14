package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.SnifferEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(SnifferEntity.class)
public abstract class SnifferEntityMixin extends AnimalEntity {

    @Unique
    private final AtomicBoolean breedingFlag = new AtomicBoolean(false);

    protected SnifferEntityMixin(EntityType<? extends AnimalEntity> entityType, World world) {
        super(entityType, world);
    }

    @WrapMethod(method = "breed(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/AnimalEntity;)V")
    private void breed(ServerWorld world, AnimalEntity other, Operation<Void> original) {
        if (this.getId() > other.getId()) return;
        SnifferEntityMixin otherMixin = (SnifferEntityMixin) other;
        if (this.breedingFlag.compareAndSet(false, true) && otherMixin.breedingFlag.compareAndSet(false, true)) {
            try {
                original.call(world, other);
            } finally {
                this.breedingFlag.set(false);
                otherMixin.breedingFlag.set(false);
            }
        }
    }
}