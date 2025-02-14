package com.axalotl.async.mixin.entity;

import com.axalotl.async.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {
    @Mutable
    @Shadow
    @Final
    private Set<PrioritizedGoal> goals;

    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.goals = ConcurrentCollections.newHashSet();
    }

    @WrapMethod(method = "tickGoals")
    private void tickGoals(boolean tickAll, Operation<Void> original) {
        synchronized (lock) {
            original.call(tickAll);
        }
    }

    @WrapMethod(method = "remove")
    private void remove(Goal goal, Operation<Void> original) {
        synchronized (lock) {
            original.call(goal);
        }
    }
}
