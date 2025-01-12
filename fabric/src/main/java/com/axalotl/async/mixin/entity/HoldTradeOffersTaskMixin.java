package com.axalotl.async.mixin.entity;

import net.minecraft.entity.ai.brain.task.HoldTradeOffersTask;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(HoldTradeOffersTask.class)
public class HoldTradeOffersTaskMixin {
    @Shadow
    private final List<ItemStack> offers = new CopyOnWriteArrayList<>();
}
