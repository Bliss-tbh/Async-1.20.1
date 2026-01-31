package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Mob.class)
public class MobMixin {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "equipItemIfPossible")
    private ItemStack tryEquip(ItemStack stack, Operation<ItemStack> original) {
        synchronized (async$lock) {
            return original.call(stack);
        }
    }

    @WrapMethod(method = "pickUpItem")
    private void pickUpItem(ItemEntity entity, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(entity);
        }
    }

    @WrapMethod(method = "setItemSlotAndDropWhenKilled")
    private void equipLootStack(EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(slot, stack);
        }
    }

    //just uses set item slot in 1.20.1

    @WrapMethod(method = "convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;")
    private <T extends Mob> @Nullable T convertTo(
            EntityType<T> entityType, boolean mysteryBool, Operation<T> original
    ) {
        synchronized (async$lock) {
            if (((Mob)(Object)this).isRemoved()) {
                return null;
            }
            return original.call(entityType, mysteryBool);
        }
    }
}