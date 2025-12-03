package com.axalotl.async.common.mixin.entity;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    volatile private ImmutableList<Entity> passengers = ImmutableList.of();
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;")
    private ItemEntity spawnAtLocation(ItemLike item, int yOffset, Operation<ItemEntity> original) {
        synchronized (async$lock) {
            return original.call(item, yOffset);
        }
    }

    @WrapMethod(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;")
    private ItemEntity spawnAtLocation(ItemStack stack, float yOffset, Operation<ItemEntity> original) {
        synchronized (async$lock) {
            return original.call(stack, yOffset);
        }
    }

    @WrapMethod(method = "setRemoved")
    private void setRemoved(Entity.RemovalReason reason, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(reason);
        }
    }

    @WrapMethod(method = "getFeetBlockState")
    private BlockState getFeetBlockState(Operation<BlockState> original) {
        BlockState blockState = original.call();
        if (blockState != null) {
            return blockState;
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @WrapMethod(method = "addPassenger")
    private void addPassenger(Entity passenger, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(passenger);
        }
    }

    @WrapMethod(method = "getIndirectPassengersStream")
    private Stream<Entity> getIndirectPassengersStream(Operation<Stream<Entity>> original) {
        synchronized (async$lock) {
            return original.call();
        }
    }

    @WrapMethod(method = "removePassenger")
    private void removePassenger(Entity passenger, Operation<Void> original) {
        synchronized (async$lock) {
            original.call(passenger);
        }
    }
}