package com.axalotl.async.mixin.entity;
import net.minecraft.server.network.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerEntity.class, priority = 1500)
public class ServerEntityMixin {
    //TODO Fix removed entity warn
    @Redirect(method = "sendPairingData", at = @At(value = "INVOKE", target = "Lnet.minecraft.entity.*
    private boolean skipWarnRemovedEntityPacked(Entity instance) {
        return false;
    }
}