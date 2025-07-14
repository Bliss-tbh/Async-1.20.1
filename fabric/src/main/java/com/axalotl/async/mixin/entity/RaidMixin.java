package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.village.raid.Raid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Mixin(Raid.class)
public class RaidMixin {
    @Shadow
    private final Map<Integer, Set<RaiderEntity>> waveToRaiders = ConcurrentCollections.newHashMap();
    @Unique
    private static final Object lock = new Object();

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

    @Redirect(method = "addToWave(ILnet/minecraft/entity/raid/RaiderEntity;Z)Z", at = @At(value = "INVOKE", target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"))
    private Object redirectComputeIfAbsent(Map<Integer, Set<RaiderEntity>> instance, Object k, Function<?, ?> key) {
        return instance.computeIfAbsent((Integer) k, wave -> ConcurrentCollections.newHashSet());
    }
}