package com.axalotl.async.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.Box;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityTrackingSection;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(EntityTrackingSection.class)
public class EntityTrackingSectionMixin<T extends EntityLike> {
    @WrapMethod(method = "forEach(Lnet/minecraft/util/math/Box;Lnet/minecraft/util/function/LazyIterationConsumer;)Lnet/minecraft/util/function/LazyIterationConsumer$NextIteration;")
    private synchronized LazyIterationConsumer.NextIteration forEach(Box box, LazyIterationConsumer<T> consumer, Operation<LazyIterationConsumer.NextIteration> original) {
        return original.call(box, consumer);
    }
}