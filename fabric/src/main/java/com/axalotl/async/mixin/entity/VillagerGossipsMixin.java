package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import net.minecraft.village.VillagerGossips;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.UUID;

@Mixin(VillagerGossips.class)
public class VillagerGossipsMixin {
    @Shadow
    private final Map<UUID, VillagerGossips.Reputation> entityReputation = ConcurrentCollections.newHashMap();
}
