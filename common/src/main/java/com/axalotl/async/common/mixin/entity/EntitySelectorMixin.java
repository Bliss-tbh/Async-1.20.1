package com.axalotl.async.common.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(EntitySelector.class)
public class EntitySelectorMixin {

    @Unique
    private static final Object async$lock = new Object();

    @WrapMethod(method = "findEntities(Lnet/minecraft/commands/CommandSourceStack;)Ljava/util/List;")
    private List<? extends Entity> move(CommandSourceStack source, Operation<List<? extends Entity>> original) {
        synchronized (async$lock) {
            return original.call(source);
        }
    }
}