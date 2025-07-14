package com.axalotl.async.mixin.world;

import com.axalotl.async.parallelised.ConcurrentCollections;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.RandomSequence;
import net.minecraft.util.math.random.RandomSequencesState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(RandomSequencesState.class)
public class RandomSequencesStateMixin {
    @Shadow
    private final Map<Identifier, RandomSequence> sequences = ConcurrentCollections.newHashMap();
}