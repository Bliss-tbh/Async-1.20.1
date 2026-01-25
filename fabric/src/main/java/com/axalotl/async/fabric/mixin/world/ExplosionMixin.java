package com.axalotl.async.fabric.mixin.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Explosion.class)
public class ExplosionMixin {
    @Shadow @Final private Level level;

    @Redirect(
            method = {
                    "finalizeExplosion", // Target for Loom/inlined mappings
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z")
    )
    private boolean onExplosionSetBlock(Level instance, BlockPos blockPos, BlockState blockState, int i) {
        level.getServer().execute(() -> instance.setBlock(blockPos, blockState, i));
        return true;
    }

}
