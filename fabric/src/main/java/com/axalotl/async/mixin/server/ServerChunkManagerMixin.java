package com.axalotl.async.mixin.server;

import com.axalotl.async.ParallelProcessor;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Either;
import net.minecraft.server.world.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Mixin(value = ServerChunkManager.class, priority = 1500)
public abstract class ServerChunkManagerMixin extends ChunkManager {
    @Shadow
    @Final
    Thread serverThread;

    @Shadow
    public abstract @Nullable ChunkHolder getChunkHolder(long pos);

    //chunkLoadingManager doesn't exist on 1.20.1 and is not necessary

    @WrapMethod(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;")
    private Chunk shortcutGetChunk(int x, int z, ChunkStatus leastStatus, boolean create, Operation<Chunk> original) {
        if (Thread.currentThread() != this.serverThread) {
            final ChunkHolder holder = this.getChunkHolder(ChunkPos.toLong(x, z));
            if (holder != null) {
                final CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = holder.getFutureFor(leastStatus);
                if (future.isDone()) {
                    try {
                        Either<Chunk, ChunkHolder.Unloaded> optionalChunk = future.get();

                        if (optionalChunk.left().isPresent()) {
                            Chunk chunk = optionalChunk.left().get();

                            if (chunk instanceof WrapperProtoChunk readOnlyChunk) {
                                chunk = readOnlyChunk.getWrappedChunk();
                            }

                            return chunk;
                        }
                    } catch (Exception e) {
                        return original.call(x, z, leastStatus, create);
                    }
                } else {
                    try {
                        Either<Chunk, ChunkHolder.Unloaded> optionalChunk = future.get(100, TimeUnit.MILLISECONDS);
                        if (optionalChunk.left().isPresent()) {
                            Chunk chunk = optionalChunk.left().get();

                            if (chunk instanceof WrapperProtoChunk readOnlyChunk) {
                                chunk = readOnlyChunk.getWrappedChunk();
                            }

                            return chunk;
                        }
                    } catch (Exception e) {
                        return original.call(x, z, leastStatus, create);
                    }
                }
            }
        }
        return original.call(x, z, leastStatus, create);
    }

    @WrapMethod(method = "getWorldChunk")
    private WorldChunk shortcutGetWorldChunk(int chunkX, int chunkZ, Operation<WorldChunk> original) {
        if (Thread.currentThread() != this.serverThread) {
            final ChunkHolder holder = this.getChunkHolder(ChunkPos.toLong(chunkX, chunkZ));
            if (holder != null) {
                final CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> future = holder.getFutureFor(ChunkStatus.FULL);
                if (future.isDone()) {
                    try {
                        Either<Chunk, ChunkHolder.Unloaded> optionalChunk = future.get();

                        if (optionalChunk.left().isPresent()) {
                            Chunk chunk = optionalChunk.left().get();

                            if (chunk instanceof WorldChunk worldChunk) {
                                return worldChunk;
                            }

                        }
                    } catch (Exception e) {
                        return original.call(chunkX, chunkZ);
                    }
                } else {
                    try {
                        Either<Chunk, ChunkHolder.Unloaded> optionalChunk = future.get(100, TimeUnit.MILLISECONDS);

                        if (optionalChunk.left().isPresent()) {
                            Chunk chunk = optionalChunk.left().get();

                            if (chunk instanceof WorldChunk worldChunk) {
                                return worldChunk;
                            }

                        }
                    } catch (Exception e) {
                        return original.call(chunkX, chunkZ);
                    }
                }
            }
        }
        return original.call(chunkX, chunkZ);
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/SpawnHelper;spawn(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/WorldChunk;Lnet/minecraft/world/SpawnHelper$Info;ZZZ)V"))
    private void tickChunks(ServerWorld world, WorldChunk chunk, SpawnHelper.Info info, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        ParallelProcessor.asyncSpawn(world, chunk, info, spawnAnimals, spawnMonsters, rareSpawn);
    }
}