package com.axalotl.async.mixin.world;

import com.mojang.datafixers.util.Either;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;


@Mixin(value = ServerChunkCache.class, priority = 1500)
public abstract class ServerChunkCacheMixin extends ChunkSource {
    @Shadow
    @Final
    Thread mainThread;

    @Shadow
    public abstract @Nullable ChunkHolder getVisibleChunkIfPresent(long pos);

    //chunkLoadingManager doesn't exist on 1.20.1 and is not necessary

    //Experimental
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
}