package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.InventoryOwner;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.AllayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AllayEntity.class)
public abstract class AllayEntityMixin implements InventoryOwner {
    @Unique
    private static final Object lock = new Object();

    @WrapMethod(method = "loot")
    private void loot(ItemEntity item, Operation<Void> original) {
        synchronized (lock) {
            if (!item.isRemoved()) {
                original.call(item);
            }
        }
    }
}