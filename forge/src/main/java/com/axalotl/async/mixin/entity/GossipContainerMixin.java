package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import net.minecraft.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;

@Mixin(GossipContainer.class)
public class GossipContainerMixin {
    @Shadow
    private final Map<UUID, GossipContainer.EntityGossips> gossips = ConcurrentCollections.newHashMap();
}
