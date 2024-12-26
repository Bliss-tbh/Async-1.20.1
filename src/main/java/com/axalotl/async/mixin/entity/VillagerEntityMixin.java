package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @WrapMethod(method = "loot")
    private void loot(ItemEntity item, Operation<Void> original) {
        synchronized (lock) {
            if (!item.isRemoved() && item.getEntityWorld() != null) {
                original.call(item);
            }
        }
    }

    @WrapMethod(method = "summonGolem")
    private void summonGolem(ServerWorld world, long time, int requiredCount, Operation<Void> original) {
        synchronized (lock) {
            original.call(world, time, requiredCount);
        }
    }
}