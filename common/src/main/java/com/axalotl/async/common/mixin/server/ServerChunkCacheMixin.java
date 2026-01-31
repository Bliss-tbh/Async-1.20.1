package com.axalotl.async.common.mixin.server;

import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.config.AsyncConfig;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
    Thread mainThread;

    @Shadow
    protected abstract @Nullable ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow @Final public ChunkMap chunkMap;

    @Shadow protected abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getChunkFutureMainThread(int x, int y, ChunkStatus chunkStatus, boolean load);

    @Shadow @Final
    private ServerChunkCache.MainThreadExecutor mainThreadProcessor;

    @Unique
    private final List<Long> async$chunksToTick = new ArrayList<>();

    //TODO: Implement our own getChunk without modifying the vanilla method
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, CallbackInfoReturnable<ChunkAccess> cir) {
        if (Thread.currentThread() == this.mainThread) return;

        ChunkAccess fast = async$tryGetChunkFast(x, z, leastStatus);
        if (fast != null) {
            cir.setReturnValue(fast);
            return;
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = CompletableFuture.supplyAsync(
                () -> this.getChunkFutureMainThread(x, z, leastStatus, create),
                this.mainThreadProcessor
                )
                .thenCompose(f -> f);

        Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> resultEither = future.join();

        if (resultEither != null) {
            resultEither.ifLeft(result -> {
                if (result instanceof ImposterProtoChunk readOnlyChunk) {
                    result = readOnlyChunk.getWrapped();
                }
                cir.setReturnValue(result);
            });
        }
    }

    @Unique
    private @Nullable ChunkAccess async$tryGetChunkFast(int x, int z, ChunkStatus leastStatus) {
        ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(x, z));
        if (holder == null) return null;

        ChunkAccess chunk = holder.getLastAvailable();
        if (chunk != null && chunk.getStatus().isOrAfter(leastStatus)) {
            if (chunk instanceof ImposterProtoChunk imposter) {
                return imposter.getWrapped();
            }
            return chunk;
        }

        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = holder.getOrScheduleFuture(leastStatus, this.chunkMap);

        if (future.isDone()) {
            ChunkAccess result = future.join().left().orElse(null);
            if (result != null) {
                if (result instanceof ImposterProtoChunk imposter) {
                    return imposter.getWrapped();
                }
                return result;
            }
        }

        return null;
    }

    //Experimental
    @Inject(method = "getChunkNow", at = @At("HEAD"), cancellable = true)
    private void shortcutGetChunkNow(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (Thread.currentThread() == this.mainThread) return;

        ChunkHolder holder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
        if (holder != null) {
            ChunkAccess chunk = holder.getLastAvailable();
            if (chunk != null && chunk.getStatus().isOrAfter(ChunkStatus.FULL)) {
                if (chunk instanceof LevelChunk levelChunk) {
                    cir.setReturnValue(levelChunk);
                    return;
                }
            }
        }
        cir.setReturnValue(null);
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickChunk(Lnet/minecraft/world/level/chunk/LevelChunk;I)V"))
    private void collectChunksToTick(ServerLevel level, LevelChunk chunk, int randomTickSpeed) {
        if (!AsyncConfig.disabled.getValue() && AsyncConfig.enableAsyncRandomTicks.getValue()) {
            this.async$chunksToTick.add(chunk.getPos().toLong());
        } else {
            level.tickChunk(chunk, randomTickSpeed);
        }
    }

    @Inject(method = "tickChunks", at = @At("TAIL"))
    private void processCollectedChunks(CallbackInfo ci) {
        if (!AsyncConfig.disabled.getValue() && AsyncConfig.enableAsyncRandomTicks.getValue() && !this.async$chunksToTick.isEmpty()) {

            int randomTickSpeed = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);

            for (long posLong : this.async$chunksToTick) {
                LevelChunk chunk = this.level.getChunkSource().getChunkNow(ChunkPos.getX(posLong), ChunkPos.getZ(posLong));

                // If the chunk unloaded since we collected the ID, skip it safely
                if (chunk == null || !chunk.getLevel().getChunkSource().hasChunk(chunk.getPos().x, chunk.getPos().z)) continue;

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Final safety check inside the async thread
                    if (chunk.getLevel() != null && chunk.getLevel().getChunkSource().hasChunk(chunk.getPos().x, chunk.getPos().z)) { //TODO: AT chunk.loaded probably more performant
                        this.level.tickChunk(chunk, randomTickSpeed);
                    }
                }, ParallelProcessor.tickPool).exceptionally(e -> {
                    ParallelProcessor.LOGGER.error("Error in async random tick", e);
                    return null;
                });

                ParallelProcessor.addTask(future);
            }

            // Clear the collection list for the next tick
            this.async$chunksToTick.clear();
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
    private void onSpawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState spawnState, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        ParallelProcessor.asyncSpawnForChunk(level, chunk, spawnState, spawnAnimals, spawnMonsters, rareSpawn);
    }
}