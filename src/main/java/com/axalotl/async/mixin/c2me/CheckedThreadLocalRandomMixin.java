package com.axalotl.async.mixin.c2me;

import com.axalotl.async.ParallelProcessor;
import com.ishland.c2me.fixes.worldgen.threading_issues.common.CheckedThreadLocalRandom;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Supplier;

@Mixin(value = CheckedThreadLocalRandom.class, remap = false)
public abstract class CheckedThreadLocalRandomMixin extends SingleThreadedRandomSource {

    @Shadow
    @Final
    private Supplier<Thread> owner;

    public CheckedThreadLocalRandomMixin(long seed) {
        super(seed);
    }

    @Shadow
    protected abstract void handleNotOwner();

    @Inject(method = "isSafe", at = @org.spongepowered.asm.mixin.injection.At("HEAD"), cancellable = true)
    public void isSafe(CallbackInfoReturnable<Boolean> cir) {
        MinecraftServer server = ParallelProcessor.getServer();
        if (server == null) {
            cir.setReturnValue(true);
            return;
        }

        Thread currentOwner = owner != null ? owner.get() : null;
        Thread expectedOwner = server.isSameThread() ? currentOwner : Thread.currentThread();

        if (currentOwner != null && currentOwner != expectedOwner) {
            handleNotOwner();
            cir.setReturnValue(false);
        } else {
            cir.setReturnValue(true);
        }
    }
}
