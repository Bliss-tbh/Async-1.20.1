package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.animal.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Bee.class)
public abstract class BeeMixin {
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "wantsToEnterHive")
    private boolean loot(Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }
}