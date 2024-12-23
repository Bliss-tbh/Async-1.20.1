package com.axalotl.async.mixin.entity;
import com.axalotl.async.parallelised.fastutil.ConcurrentLongSortedSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PointOfInterestStorage.class)
public class PointOfInterestStorageMixin {
    @Mutable
    @Shadow
    @Final
    private LongSet preloadedChunks;
    @Inject(method = "<init>", at = @At("TAIL"))
    private void replaceConVars(CallbackInfo ci) {
        this.preloadedChunks = new ConcurrentLongSortedSet();
    }
}