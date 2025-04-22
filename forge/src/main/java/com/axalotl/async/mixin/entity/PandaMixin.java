package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.entity.ItemEntity;
import net.minecraft.world.entity.animal.Panda;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Panda.class)
public class PandaMixin {
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "pickUpItem")
    private void pickUpItem(ItemEntity itemEntity, Operation<Void> original) {
        synchronized (async$lock) {
            if (!itemEntity.isRemoved()) {
                original.call(itemEntity);
            }
        }
    }
}
