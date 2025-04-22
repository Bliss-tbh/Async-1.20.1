package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.World;
import net.minecraft.entity.*
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ServerLevel.EntityCallbacks.class)
public class EntityCallbacksMixin {

    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "onTickingStart(Lnet.minecraft.entity.*
    private synchronized void onTickingStart(Entity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "onTickingEnd(Lnet.minecraft.entity.*
    private synchronized void onTickingEnd(Entity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(entity);
        }
    }
}