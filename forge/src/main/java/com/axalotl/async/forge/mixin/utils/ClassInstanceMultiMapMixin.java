package com.axalotl.async.forge.mixin.utils;

import com.axalotl.async.common.parallelised.ConcurrentCollections;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@Mixin(value = ClassInstanceMultiMap.class, priority = Integer.MIN_VALUE)
public abstract class ClassInstanceMultiMapMixin<T> extends AbstractCollection<T> {

    @Unique
    private static final Object async$lock = new Object();

    @Shadow
    private final Map<Class<?>, List<T>> byClass = new ConcurrentHashMap<>();

    @Shadow
    private final List<T> allInstances = new CopyOnWriteArrayList<>();

    @Shadow @Final
    private Class<T> baseClass;

    //TODO: check if casts are necessary
    /**
     * @author prydaran
     * @reason overwriting at highest priority (Integer.MIN_VALUE) should function identically to a @ModifyArg, with the exception being we are now the base implementation.
     */
    @Overwrite
    public <S> Collection<S> find(Class<S> pType) {
        if (!this.baseClass.isAssignableFrom(pType)) {
            throw new IllegalArgumentException("Don't know how to search for " + pType);
        } else {
            List<? extends T> Contents = (List)this.byClass.computeIfAbsent(pType, (p_13538_) -> {
                Stream var10000 = this.allInstances.stream();
                Objects.requireNonNull(p_13538_);
                return (List)var10000.filter(p_13538_::isInstance).collect(ConcurrentCollections.toList());
            });
            return (Collection<S>) Collections.unmodifiableCollection(Contents);
        }
    }

    @WrapMethod(method = "add")
    private boolean add(Object e, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(e);
        }
    }

    @WrapMethod(method = "remove")
    private boolean remove(Object o, Operation<Boolean> original) {
        synchronized (async$lock) {
            return original.call(o);
        }
    }
}
