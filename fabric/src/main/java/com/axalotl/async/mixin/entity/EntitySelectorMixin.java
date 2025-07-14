package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.command.EntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Unique
    private static final Object lock = new Object();

    @WrapMethod(method = "getEntities(Lnet/minecraft/server/command/ServerCommandSource;)Ljava/util/List;")
    private List<? extends Entity> move(ServerCommandSource source, Operation<List<? extends Entity>> original) {
        synchronized (lock) {
            return original.call(source);
        }
    }
}