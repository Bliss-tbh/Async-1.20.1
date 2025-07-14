package com.axalotl.async.mixin.entity;

import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TurtleEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TurtleEntity.MateGoal.class)
public abstract class TurtleEntityMixin extends AnimalMateGoal {
    @Shadow
    @Final
    private TurtleEntity turtle;

    public TurtleEntityMixin(AnimalEntity animal, double speed) {
        super(animal, speed);
    }

    @Redirect(method = "breed()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/TurtleEntity;setHasEgg(Z)V"))
    private void redirectSetHasEgg(TurtleEntity instance, boolean value) {
        if (this.mate != null && !this.turtle.hasEgg() && !((TurtleEntity) this.mate).hasEgg()) {
            if (this.turtle.getRandom().nextBoolean()) {
                this.turtle.setHasEgg(true);
            } else {
                ((TurtleEntity) this.mate).setHasEgg(true);
            }
        }
    }
}