package com.axalotl.async.mixin.world;

import com.axalotl.async.ParallelProcessor;
import com.axalotl.async.parallelised.ConcurrentCollections;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(value = ServerWorld.class, priority = 1500)
public abstract class ServerWorldMixin extends World implements StructureWorldAccess {
    @Unique
    ConcurrentLinkedQueue<BlockEvent> syncedBlockEventQueue;
    @Shadow
    @Final
    @Mutable
    Set<MobEntity> loadedMobs;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        loadedMobs = ConcurrentCollections.newHashSet();
        syncedBlockEventQueue = new ConcurrentLinkedQueue<>();
    }

    @Inject(method = "tick", at = @At(value = "TAIL", target = "Lnet/minecraft/world/EntityList;forEach(Ljava/util/function/Consumer;)V"))
    private void afterTickEntity(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if ((Object) this instanceof ServerWorld) {
            ParallelProcessor.postEntityTick();
        }
    }

    @Redirect(method = "method_31420", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;tickEntity(Ljava/util/function/Consumer;Lnet/minecraft/entity/Entity;)V"))
    private void overwriteEntityTicking(ServerWorld instance, Consumer<Entity> consumer, Entity entity) {
        ParallelProcessor.callEntityTick(consumer, entity);
    }

    @Redirect(
            method = "addSyncedBlockEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;add(Ljava/lang/Object;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueAdd(ObjectLinkedOpenHashSet<BlockEvent> objectLinkedOpenHashSet, Object object) {
        return syncedBlockEventQueue.add((BlockEvent) object);
    }

    @Redirect(
            method = "clearUpdatesInArea",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeIf(Ljava/util/function/Predicate;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueRemoveIf(ObjectLinkedOpenHashSet<BlockEvent> objectLinkedOpenHashSet, Predicate<BlockEvent> filter) {
        return syncedBlockEventQueue.removeIf(filter);
    }

    @Redirect(
            method = "processSyncedBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;isEmpty()Z",
                    remap = false
            )
    )
    private boolean overwriteEmptyCheck(ObjectLinkedOpenHashSet<BlockEvent> objectLinkedOpenHashSet) {
        return syncedBlockEventQueue.isEmpty();
    }

    @Redirect(
            method = "processSyncedBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;removeFirst()Ljava/lang/Object;",
                    remap = false
            )
    )
    private Object overwriteQueueRemoveFirst(ObjectLinkedOpenHashSet<BlockEvent> objectLinkedOpenHashSet) {
        return syncedBlockEventQueue.poll();
    }

    @Redirect(
            method = "processSyncedBlockEvents",
            at = @At(
                    value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/objects/ObjectLinkedOpenHashSet;addAll(Ljava/util/Collection;)Z",
                    remap = false
            )
    )
    private boolean overwriteQueueAddAll(ObjectLinkedOpenHashSet<BlockEvent> instance, Collection<? extends BlockEvent> c) {
        return syncedBlockEventQueue.addAll(c);
    }

    @Redirect(method = "updateListeners", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ServerWorld;duringListenerUpdate:Z", opcode = Opcodes.PUTFIELD))
    private void skipSendBlockUpdatedCheck(ServerWorld instance, boolean value) {
    }
}