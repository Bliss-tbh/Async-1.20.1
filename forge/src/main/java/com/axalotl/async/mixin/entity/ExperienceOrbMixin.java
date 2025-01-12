package com.axalotl.async.mixin.entity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import java.util.concurrent.locks.ReentrantLock;
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends Entity {
    @Unique
    private static final ReentrantLock async$lock = new ReentrantLock();
    public ExperienceOrbMixin(EntityType<?> type, Level world) {
        super(type, world);
    }
    @WrapMethod(method = "canMerge(Lnet/minecraft/world/entity/ExperienceOrb;)Z")
    private boolean canMerge(ExperienceOrb other, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(other);
        }
    }
}