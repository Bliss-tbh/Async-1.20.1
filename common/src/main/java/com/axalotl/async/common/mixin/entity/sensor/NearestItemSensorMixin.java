package com.axalotl.async.common.mixin.entity.sensor;

import com.axalotl.async.common.config.AsyncConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.NearestItemSensor;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

@Mixin(value = NearestItemSensor.class, priority = 1500)
public class NearestItemSensorMixin {

    @Redirect(method = "doTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private <T extends ItemEntity> Comparator<T> async$safeComparator(ToDoubleFunction<? super T> keyExtractor,
                                                                      ServerLevel world, Mob entity) {
        if (AsyncConfig.disabled.getValue()) {
            return Comparator.comparingDouble(keyExtractor);
        }

        final double ex = entity.getX();
        final double ey = entity.getY();
        final double ez = entity.getZ();

        Map<T, double[]> cache = new IdentityHashMap<>();

        return (t1, t2) -> {
            double[] p1 = cache.computeIfAbsent(t1, e -> new double[]{e.getX(), e.getY(), e.getZ()});
            double[] p2 = cache.computeIfAbsent(t2, e -> new double[]{e.getX(), e.getY(), e.getZ()});

            double d1 = (p1[0]-ex)*(p1[0]-ex) + (p1[1]-ey)*(p1[1]-ey) + (p1[2]-ez)*(p1[2]-ez);
            double d2 = (p2[0]-ex)*(p2[0]-ex) + (p2[1]-ey)*(p2[1]-ey) + (p2[2]-ez)*(p2[2]-ez);

            return Double.compare(d1, d2);
        };
    }
}
