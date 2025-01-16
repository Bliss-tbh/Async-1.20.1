package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Raid.class)
public class RaidMixin {

    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "addWaveMob(ILnet/minecraft/world/entity/raid/Raider;)Z")
    private boolean addWaveMob(int wave, Raider entity, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(wave, entity);
        }
    }

    @WrapMethod(method = "addWaveMob(ILnet/minecraft/world/entity/raid/Raider;Z)Z")
    private boolean addWaveMob(int wave, Raider entity, boolean countHealth, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(wave, entity, countHealth);
        }
    }
}