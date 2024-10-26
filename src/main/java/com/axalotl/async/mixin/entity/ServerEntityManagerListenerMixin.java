package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ServerEntityManager.Listener.class)
public abstract class ServerEntityManagerListenerMixin<T extends EntityLike> implements AutoCloseable {
    @Shadow
    private EntityTrackingSection<T> section;
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @Redirect(method = "updateEntityPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityTrackingSection;remove(Lnet/minecraft/world/entity/EntityLike;)Z"))
    private boolean updateEntityPosition(EntityTrackingSection instance, T entity) {
        this.section.remove(entity);
        return true;
    }
    @Redirect(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityTrackingSection;remove(Lnet/minecraft/world/entity/EntityLike;)Z"))
    private boolean remove(EntityTrackingSection instance, T entity) {
        this.section.remove(entity);
        return true;
    }

    @WrapMethod(method = "updateEntityPosition")
    private void updateEntityPosition(Operation<Void> original) {
        lock.lock();
        try {
            original.call();
        } catch (Throwable ignored) {
        } finally {
            lock.unlock();
        }
    }

    @WrapMethod(method = "remove")
    private void remove(Entity.RemovalReason reason, Operation<Void> original) {
        lock.lock();
        try {
            original.call(reason);
        } catch (Throwable ignored) {
        } finally {
            lock.unlock();
        }
    }

}
