package com.axalotl.async.mixin.entity;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import net.minecraft.entity.mob.MobVisibilityCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MobVisibilityCache.class)
public class MobVisibilityCacheMixin {
    @Shadow
    private final IntSet visibleEntities = IntSets.synchronize(new IntOpenHashSet());
    @Shadow
    private final IntSet invisibleEntities = IntSets.synchronize(new IntOpenHashSet());
}
