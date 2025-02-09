package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.fastutil.ConcurrentLongSortedSet;
import com.axalotl.async.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(SectionedEntityCache.class)
public abstract class SectionedEntityCacheMixin<T extends EntityLike> {
    @Shadow
    @Final
    @Mutable
    private LongSortedSet trackedPositions;

    @Mutable
    @Shadow
    @Final
    private Long2ObjectMap<EntityTrackingSection<T>> trackingSections;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceConVars(CallbackInfo ci) {
        trackingSections = new Long2ObjectConcurrentHashMap<>();
        trackedPositions = new ConcurrentLongSortedSet();
    }
}