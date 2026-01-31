package com.axalotl.async.common.mixin.entity;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(AttributeInstance.class)
public class AttributeInstanceMixin {

    @Shadow
    @Final
    @Mutable
    private Map<ResourceLocation, AttributeModifier> modifierById;

    @Shadow
    @Final
    @Mutable
    private Map<ResourceLocation, AttributeModifier> permanentModifiers;

    @Shadow
    private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = ConcurrentCollections.newHashMap();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void makeThreadSafe(CallbackInfo ci) {
        modifierById = new ConcurrentHashMap<>(modifierById);
        permanentModifiers = new ConcurrentHashMap<>(permanentModifiers);
    }

    //parreleliseeseseeseieiles get modifiers map lolololo not herekesoifgjsdfujiogzdjnfuio
}
