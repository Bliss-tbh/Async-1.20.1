package com.axalotl.async.mixin.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.PlayWithVillagerBabiesTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayWithVillagerBabiesTask.class)
public class PlayWithVillagerBabiesTaskMixin {
    @Inject(method = "getInteractionTarget", at = @At("HEAD"), cancellable = true)
    private static void onGetInteractionTarget(LivingEntity baby, CallbackInfoReturnable<LivingEntity> cir) {
        cir.setReturnValue(baby.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).orElse(null));
    }
}
