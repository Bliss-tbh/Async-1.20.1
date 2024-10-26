package com.axalotl.async.mixin.world;

import com.axalotl.async.parallelised.fastutil.ConcurrentLongLinkedOpenHashSet;
import com.axalotl.async.parallelised.fastutil.Long2ObjectConcurrentHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(SerializingRegionBasedStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> implements AutoCloseable {
    @Shadow
    @Mutable
    private final Long2ObjectMap<Optional<R>> loadedElements = new Long2ObjectConcurrentHashMap<>();

    @Shadow
    @Mutable
    private final LongLinkedOpenHashSet unsavedElements = new ConcurrentLongLinkedOpenHashSet();
}
