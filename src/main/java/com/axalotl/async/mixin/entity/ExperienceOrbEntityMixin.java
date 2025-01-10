package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.concurrent.locks.ReentrantLock;

@Mixin(ExperienceOrbEntity.class)
public class ExperienceOrbEntityMixin {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @WrapMethod(method = "wasMergedIntoExistingOrb")
    private static boolean wasMergedIntoExistingOrb(ServerWorld world, Vec3d pos, int amount, Operation<Boolean> original) {
        synchronized (lock) {
            return original.call(world, pos, amount);
        }
    }
}
