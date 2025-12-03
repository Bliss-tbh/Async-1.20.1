package com.axalotl.async.common.mixin.server;

import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.config.AsyncConfig;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@Mixin(value = ServerChunkCache.class, priority = 1500)
public abstract class ServerChunkCacheMixin extends ChunkSource {

    @Final
    @Shadow
    public ServerLevel level;

    @Shadow
    @Final
    public ChunkMap chunkMap;

    @Shadow
    @Final
    Thread mainThread;

    @Shadow
    public abstract @Nullable ChunkHolder getVisibleChunkIfPresent(long pos);

    private final List<LevelChunk> async$chunksToTick = new ArrayList<>();

    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() != this.mainThread) {
            final ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(x, z));
            if (holder != null) {
                final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = holder.getFutureIfPresentUnchecked(leastStatus);
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> result = future.getNow(null);

                if (result != null) {
                    result.ifLeft(chunk -> {
                        if (chunk instanceof ImposterProtoChunk readOnlyChunk) {
                            chunk = readOnlyChunk.getWrapped();
                        }
                        cir.setReturnValue(chunk);
                    });
                }
            }
        }
    }

    //Experimental
    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunkNow(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (Thread.currentThread() != this.mainThread) {
            final ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
            if (holder != null) {
                final CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = holder.getFutureIfPresentUnchecked(ChunkStatus.FULL);
                Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> result = future.getNow(null);

                if (result != null) {
                    result.ifLeft(chunk -> {
                        if (chunk instanceof LevelChunk worldChunk) {
                            cir.setReturnValue(worldChunk);
                        }
                    });
                }
            }
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V"))
    private void collectChunksToTick(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        if (!AsyncConfig.disabled.getValue() && AsyncConfig.enableAsyncRandomTicks.getValue()) {
            this.async$chunksToTick.add(chunk);
        } else {
            level.tickChunk(chunk, randomTickSpeed);
        }
    }

    @Inject(method = "tickChunks", at = @At("TAIL"))
    private void processCollectedChunks(CallbackInfo ci, @Local(ordinal = 0) int randomTickSpeed) {
        if (!AsyncConfig.disabled.getValue() && AsyncConfig.enableAsyncRandomTicks.getValue() && !this.async$chunksToTick.isEmpty()) {
            final List<LevelChunk> chunksToProcess = new ArrayList<>(this.async$chunksToTick);
            CompletableFuture.runAsync(() -> {
                for (LevelChunk chunk : chunksToProcess) {
                    this.level.tickChunk(chunk, randomTickSpeed);
                }
            }, ParallelProcessor.tickPool).exceptionally(e -> {
                ParallelProcessor.LOGGER.error("Error in async random tick, switching to synchronous", e);
                return null;
            });
            this.async$chunksToTick.clear();
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
    private void onSpawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        ParallelProcessor.asyncSpawnForChunk(level, chunk, spawnState, spawnAnimals, spawnMonsters, rareSpawn);
    }
}