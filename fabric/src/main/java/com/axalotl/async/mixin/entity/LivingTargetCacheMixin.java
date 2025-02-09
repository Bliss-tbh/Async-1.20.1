package com.axalotl.async.mixin.entity;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.LivingTargetCache;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Predicate;

@Mixin(LivingTargetCache.class)
public class LivingTargetCacheMixin {
    @Mutable
    @Shadow
    @Final
    private Predicate<LivingEntity> targetPredicate;

    @Inject(method = "<init>(Lnet/minecraft/entity/LivingEntity;Ljava/util/List;)V", at = @At("RETURN"))
    private void init(LivingEntity owner, List entities, CallbackInfo ci) {
        Object2BooleanOpenHashMap<LivingEntity> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(entities.size());
        Predicate<LivingEntity> predicate = target -> Sensor.testTargetPredicate(owner, target);
        this.targetPredicate = entity -> {
            synchronized (object2BooleanOpenHashMap) {
                return object2BooleanOpenHashMap.computeIfAbsent(entity, predicate);
            }
        };
    }
}
