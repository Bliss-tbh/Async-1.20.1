package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    public ItemEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapMethod(method = "tryMerge()V")
    private void tryMerge(Operation<Void> original) {
        synchronized (lock) {
            original.call();
        }
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        synchronized (lock) {
            super.move(movementType, movement);
        }
    }

    @WrapMethod(method = "onPlayerCollision")
    private void onPlayerCollision(PlayerEntity player, Operation<Void> original) {
        synchronized (lock) {
            original.call(player);
        }
    }
}
