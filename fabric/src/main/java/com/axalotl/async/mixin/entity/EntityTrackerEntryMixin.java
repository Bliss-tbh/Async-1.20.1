package com.axalotl.async.mixin.entity;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = EntityTrackerEntry.class, priority = 1500)
public class EntityTrackerEntryMixin {

    //TODO Fix removed entity warn
    @Redirect(method = "sendPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isRemoved()Z"))
    private boolean skipWarnRemovedEntityPacked(Entity instance) {
        return false;
    }
}