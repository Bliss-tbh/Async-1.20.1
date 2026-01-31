package com.axalotl.async.common.mixin.entity.spawn;

import com.axalotl.async.common.ParallelProcessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {
    @Redirect(
            method = "spawnForChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;")
    )
    private static ProfilerFiller async$safeProfiler(ServerLevel instance) {
        return ParallelProcessor.isServerExecutionThread()
                ? InactiveProfiler.INSTANCE
                : instance.getProfiler();
    }
}
