package com.axalotl.async.forge.mixin.utils;

import com.axalotl.async.common.ParallelProcessor;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

@Mixin(Util.class)
public abstract class UtilMixin {


    @Redirect(
            method = "makeExecutor(Ljava/lang/String;)Ljava/util/concurrent/ExecutorService;",
            at = @At(
                    value = "NEW",
                    target = "java/util/concurrent/ForkJoinPool"
            )
    )
    private static ForkJoinPool redirectForkJoinPool(
            int parallelism,
            java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory factory,
            java.lang.Thread.UncaughtExceptionHandler handler,
            boolean asyncMode,
            String serviceName
    ) {
        ForkJoinPool newPool = new ForkJoinPool(
                parallelism,
                runnable -> {
                    ForkJoinWorkerThread thread = factory.newThread(runnable);
                    ParallelProcessor.registerThread(serviceName, thread);
                    return thread;
                },
                handler,
                asyncMode
        );
        return newPool;
    }
}
