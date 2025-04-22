package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(targets = "net.minecraft.entity.EntityLookup$EntityCallbacks")
public class EntityCallbacksMixin {

    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "onTickingStart", at = @WrapMethod.At("HEAD"))
    private synchronized void onTickingStart(Entity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "onTickingEnd", at = @WrapMethod.At("HEAD"))
    private synchronized void onTickingEnd(Entity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(entity);
        }
    }
}