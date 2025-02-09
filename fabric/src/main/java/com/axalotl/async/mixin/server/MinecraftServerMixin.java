package com.axalotl.async.mixin.server;

import com.axalotl.async.ParallelProcessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MinecraftServer.class, priority = Integer.MAX_VALUE)
public abstract class MinecraftServerMixin extends ReentrantThreadExecutor<ServerTask> implements CommandOutput, AutoCloseable {

    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Redirect(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isOnThread()Z"))
    private boolean onServerExecutionThreadPatch(MinecraftServer minecraftServer) {
        return ParallelProcessor.isServerExecutionThread();
    }
}

