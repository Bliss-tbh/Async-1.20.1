package com.axalotl.async.mixin.entity;

import com.axalotl.async.config.AsyncConfig;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();

    @WrapMethod(method = "move")
    private synchronized void move(MoverType type, Vec3 movement, Operation<Void> original) {
        if (AsyncConfig.enableEntityMoveSync) {
            synchronized (async$lock) {
                original.call(type, movement);
            }
        } else {
            original.call(type, movement);
        }
    }

    //tickBlockCollision is simpler on 1.20.1
    @WrapMethod(method = "checkInsideBlocks()V")
    private void checkInsideBlocks(Operation<Void> original) {
        if (AsyncConfig.enableEntityMoveSync) {
            synchronized (async$lock) {
                original.call();
            }
        } else {
            original.call();
        }
    }

    @WrapMethod(method = "setRemoved")
    private synchronized void setRemoved(Entity.RemovalReason reason, Operation<Void> original) {
        original.call(reason);
    }
}