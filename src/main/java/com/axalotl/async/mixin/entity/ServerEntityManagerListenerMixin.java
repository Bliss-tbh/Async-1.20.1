package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerEntityManager.Listener.class)
public abstract class ServerEntityManagerListenerMixin implements AutoCloseable {

    @WrapMethod(method = "updateEntityPosition")
    private synchronized void updateEntityPosition(Operation<Void> original) {
        original.call();
    }

    @WrapMethod(method = "remove")
    private synchronized void remove(Entity.RemovalReason reason, Operation<Void> original) {
        original.call(reason);
    }
}
