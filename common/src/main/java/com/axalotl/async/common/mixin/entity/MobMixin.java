package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Mob.class)
public class MobMixin {
    @Unique
    private static final Object lock = new Object();

    @WrapMethod(method = "pickUpItem")
    private void pickUpItem(ItemEntity item, Operation<Void> original) {
        synchronized (lock) {
            if (!item.isRemoved()) {
                original.call(item);
            }
        }
    }
}