package com.axalotl.async.common.mixin.world;

import com.axalotl.async.common.ExplosionProcessor;
import com.axalotl.async.common.ParallelProcessor;
import com.axalotl.async.common.config.AsyncConfig;
import com.axalotl.async.common.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.*;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(value = ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements WorldGenLevel {

    @Shadow
    @Final
    EntityTickList entityTickList;

    @Unique
    ConcurrentLinkedQueue<BlockEventData> async$syncedBlockEventQueue;

    @Shadow
    @Final
    @Mutable
    Set<Mob> navigatingMobs;

    @Shadow
    @Final
    private ServerChunkCache chunkSource;

    protected ServerLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, Supplier<ProfilerFiller> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }

    @Shadow
    public abstract @NotNull ServerLevel getLevel();

    @Shadow
    @Mutable
    @Final
    List<ServerPlayer> players;

    @Unique
    private static final Object lock = new Object();


    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        navigatingMobs = ConcurrentCollections.newHashSet();
        async$syncedBlockEventQueue = new ConcurrentLinkedQueue<>();
        players = new CopyOnWriteArrayList<>();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void overwriteEntityTicking(EntityTickList entityList, Consumer<Entity> action) {
        ProfilerFiller profilerfiller = this.getProfiler();
        this.entityTickList.forEach(entity -> {
            if (!entity.isRemoved()) {
                profilerfiller.push("checkDespawn");
                ParallelProcessor.asyncDespawn(entity);
                profilerfiller.pop();
                if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(entity.chunkPosition().toLong())) {
                    Entity entity2 = entity.getVehicle();
                    if (entity2 != null) {
                        if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
                            return;
                        }
                        entity.stopRiding();
                    }
                    profilerfiller.push("tick");
                    ParallelProcessor.callEntityTick(this.getLevel(), entity);
                    profilerfiller.pop();
                }
            }
        });
        profilerfiller.push("tick");
        ParallelProcessor.postEntityTick();
        profilerfiller.pop();
    }

    @Redirect(
            method = "blockEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;add(Ljava/lang/Object;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueAdd(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet, Object object) {
        return async$syncedBlockEventQueue.add((BlockEventData) object);
    }

    @Redirect(
            method = "clearBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeIf(Ljava/util/function/Predicate;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueRemoveIf(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet, Predicate<BlockEventData> filter) {
        return async$syncedBlockEventQueue.removeIf(filter);
    }

    @Redirect(
            method = "runBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;isEmpty()Z",
                    remap = false
            )
    )
    private boolean overwriteEmptyCheck(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet) {
        return async$syncedBlockEventQueue.isEmpty();
    }

    @Redirect(
            method = "runBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeFirst()Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object overwriteQueueRemoveFirst(ObjectLinkedOpenHashSet<BlockEventData> objectLinkedOpenHashSet) {
        return async$syncedBlockEventQueue.poll();
    }

    @Redirect(
            method = "runBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;addAll(Ljava/util/Collection;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueAddAll(ObjectLinkedOpenHashSet<BlockEventData> instance, Collection<? extends BlockEventData> c) {
        return async$syncedBlockEventQueue.addAll(c);
    }

    @Redirect(method = "sendBlockUpdated", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;isUpdatingNavigations:Z", opcode = Opcodes.PUTFIELD))
    private void skipSendBlockUpdatedCheck(ServerLevel instance, boolean value) {
    }

    @WrapMethod(method = "addFreshEntity")
    private boolean wrapAddFreshEntity(Entity entity, Operation<Boolean> original) {
        if (AsyncConfig.disabled.getValue() || !AsyncConfig.enableAsyncSpawn.getValue()) {
            return original.call(entity);
        }

        synchronized (ParallelProcessor.getEntityAddLock()) {
            return original.call(entity);
        }
    }
}