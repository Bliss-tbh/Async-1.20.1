package com.axalotl.async.mixin.utils;

import net.minecraft.util.collection.WeightedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mixin(WeightedList.class)
public abstract class WeightedListMixin<U> implements Iterable<U> {
    @Shadow
    protected final List<WeightedList.Entry<U>> entries = new CopyOnWriteArrayList<>();
}
