package com.axalotl.async.common.mixin.world;

import com.axalotl.async.common.parallelised.fastutil.ConcurrentShortHashSet;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {

    @Mutable
    @Shadow
    @Final
    private ShortSet[] changedBlocksPerSection;

    @Shadow
    private boolean hasChangedSections;

    @Shadow
    @Final
    private LevelHeightAccessor levelHeightAccessor;

    @Shadow
    public abstract LevelChunk getTickingChunk();

    @Inject(method = "<init>", at = @At(value = "TAIL", target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;", opcode = Opcodes.PUTFIELD))
    private void overwriteShortSet(ChunkPos pos, int level, LevelHeightAccessor world, LevelLightEngine lightingProvider, ChunkHolder.LevelChangeListener levelUpdateListener, ChunkHolder.PlayerProvider playersWatchingChunkProvider, CallbackInfo ci) {
        this.changedBlocksPerSection = new ConcurrentShortHashSet[world.getSectionsCount()];
    }

    @Inject(method = "blockChanged", at = @At("HEAD"))
    private void replaceBlockChanged(BlockPos pos, CallbackInfo ci) {
        LevelChunk levelchunk = this.getTickingChunk();
        if (levelchunk == null) {
            return;
        }

        boolean flag = this.hasChangedSections;
        int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
        ShortSet shortset = this.changedBlocksPerSection[i];

        if (shortset == null) {
            synchronized (this.changedBlocksPerSection) {
                shortset = this.changedBlocksPerSection[i];
                if (shortset == null) {
                    this.hasChangedSections = true;
                    shortset = new ConcurrentShortHashSet();
                    this.changedBlocksPerSection[i] = shortset;
                }
            }
        }

        shortset.add(SectionPos.sectionRelativePos(pos));
    }
}
