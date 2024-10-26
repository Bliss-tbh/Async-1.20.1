package com.axalotl.async.mixin.world;

import com.axalotl.async.parallelised.fastutil.Int2ObjectConcurrentHashMap;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.*;

import java.nio.file.Path;

@Mixin(value = ThreadedAnvilChunkStorage.class)
public abstract class ServerChunkLoadingManagerMixin extends VersionedChunkStorage implements ChunkHolder.PlayersWatchingChunkProvider {

    public ServerChunkLoadingManagerMixin(Path directory, DataFixer dataFixer, boolean dsync) {
        super(directory, dataFixer, dsync);
    }

    @Shadow
    @Final
    @Mutable
    private Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers = new Int2ObjectConcurrentHashMap<>();

}