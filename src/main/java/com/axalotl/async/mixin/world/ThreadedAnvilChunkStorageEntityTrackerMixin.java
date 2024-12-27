package com.axalotl.async.mixin.world;

import com.axalotl.async.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(value = ThreadedAnvilChunkStorage.EntityTracker.class)
public class ThreadedAnvilChunkStorageEntityTrackerMixin {

    @Shadow
    final private Set<ServerPlayNetworkHandler> listeners = ConcurrentCollections.newHashSet();

    @WrapMethod(method = "updateTrackedStatus(Lnet/minecraft/server/network/ServerPlayerEntity;)V")
    private synchronized void updateTrackingStatus(ServerPlayerEntity player, Operation<Void> original) {
        original.call(player);
    }
}