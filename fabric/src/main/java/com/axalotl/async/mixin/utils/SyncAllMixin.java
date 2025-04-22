package com.axalotl.async.mixin.utils;

import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.entity.EntitySection;
import net.minecraft.world.gameevent.EuclideanGameEventListenerRegistry;
import net.minecraft.world.levelgen.LegacyRandomSource;
import net.minecraft.world.lighting.DynamicGraphMinFixedPoint;
import net.minecraft.world.pathfinder.BinaryHeap;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(value = {
        BinaryHeap.class,
        LevelChunkTicks.class,
        DynamicGraphMinFixedPoint.class,
        PathNavigation.class,
        LegacyRandomSource.class,
        EuclideanGameEventListenerRegistry.class,
        SimpleCriterionTrigger.class,
        AngerManagement.class,
        WorldBorder.class,
        EntitySection.class,
        ClassInstanceMultiMap.class,
        PalettedContainer.class,
        ActiveProfiler.class
})
public class SyncAllMixin {
}