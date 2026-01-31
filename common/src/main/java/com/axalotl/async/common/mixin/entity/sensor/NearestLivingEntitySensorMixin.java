package com.axalotl.async.common.mixin.entity.sensor;

import com.axalotl.async.common.config.AsyncConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.NearestLivingEntitySensor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

@Mixin(value = NearestLivingEntitySensor.class, priority = 1500)
public class NearestLivingEntitySensorMixin<T extends LivingEntity> {

    @Redirect(method = "doTick",
            at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private <E extends LivingEntity> Comparator<E> async$safeComparator(ToDoubleFunction<? super E> keyExtractor,
                                                                        ServerLevel world, T entity) {
        if (AsyncConfig.disabled.getValue()) {
            return Comparator.comparingDouble(keyExtractor);
        }

        final double ex = entity.getX();
        final double ey = entity.getY();
        final double ez = entity.getZ();

        Map<E, double[]> cache = new IdentityHashMap<>();

        return (t1, t2) -> {
            double[] p1 = cache.computeIfAbsent(t1, e -> new double[]{e.getX(), e.getY(), e.getZ()});
            double[] p2 = cache.computeIfAbsent(t2, e -> new double[]{e.getX(), e.getY(), e.getZ()});

            double d1 = (p1[0]-ex)*(p1[0]-ex) + (p1[1]-ey)*(p1[1]-ey) + (p1[2]-ez)*(p1[2]-ez);
            double d2 = (p2[0]-ex)*(p2[0]-ex) + (p2[1]-ey)*(p2[1]-ey) + (p2[2]-ez)*(p2[2]-ez);

            return Double.compare(d1, d2);
        };
    }
}