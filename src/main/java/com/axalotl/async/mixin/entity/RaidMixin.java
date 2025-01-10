package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Raid.class)
public class RaidMixin {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @WrapMethod(method = "addToWave(ILnet/minecraft/entity/raid/RaiderEntity;)Z")
    private boolean addToWave(int wave, RaiderEntity entity, Operation<Boolean> original) {
        synchronized (lock) {
            return original.call(wave, entity);
        }
    }

    @WrapMethod(method = "addToWave(ILnet/minecraft/entity/raid/RaiderEntity;Z)Z")
    private boolean addToWave(int wave, RaiderEntity entity, boolean countHealth, Operation<Boolean> original) {
        synchronized (lock) {
            return original.call(wave, entity, countHealth);
        }
    }
}
