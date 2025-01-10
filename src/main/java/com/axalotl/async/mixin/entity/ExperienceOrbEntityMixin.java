package com.axalotl.async.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.concurrent.locks.ReentrantLock;
@Mixin(ExperienceOrbEntity.class)
public abstract class ExperienceOrbEntityMixin extends Entity {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();
    public ExperienceOrbEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }
    @WrapMethod(method = "isMergeable(Lnet/minecraft/entity/ExperienceOrbEntity;)Z")
    private boolean isMergeable(ExperienceOrbEntity other, Operation<Boolean> original) {
        synchronized (lock) {
            return original.call(other);
        }
    }
}