package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(PersistentEntitySectionManager.Callback.class)
public abstract class ServerEntityManagerListenerMixin implements AutoCloseable {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @WrapMethod(method = "onMove")
    private void updateEntityPosition(Operation<Void> original) {
        synchronized (lock) {
            original.call();
        }
    }

    @WrapMethod(method = "onRemove")
    private void remove(Entity.RemovalReason reason, Operation<Void> original) {
        synchronized (lock) {
            original.call(reason);
        }
    }
}
