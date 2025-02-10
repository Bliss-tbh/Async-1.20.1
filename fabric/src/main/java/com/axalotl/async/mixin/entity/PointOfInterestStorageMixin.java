package com.axalotl.async.mixin.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.spongepowered.asm.mixin.*;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(PointOfInterestStorage.class)
public class PointOfInterestStorageMixin {
    @Unique
    private static final ReentrantLock lock = new ReentrantLock();

    @WrapMethod(method = "getInSquare")
    private Stream<PointOfInterest> getInSquare(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Stream<PointOfInterest>> original) {
        synchronized (lock) {
            return original.call(typePredicate, pos, radius, occupationStatus);
        }
    }

    @WrapMethod(method = "getInCircle")
    private Stream<PointOfInterest> getInCircle(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Stream<PointOfInterest>> original) {
        synchronized (lock) {
            return original.call(typePredicate, pos, radius, occupationStatus);
        }
    }

    @WrapMethod(method = "getInChunk")
    private Stream<PointOfInterest> getInChunk(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, ChunkPos chunkPos, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Stream<PointOfInterest>> original) {
        synchronized (lock) {
            return original.call(typePredicate, chunkPos, occupationStatus);
        }
    }

    @WrapMethod(method = "count")
    private long getInChunk(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Long> original) {
        synchronized (lock) {
            return original.call(typePredicate, pos, radius, occupationStatus);
        }
    }

    @WrapMethod(method = "getNearestPosition(Ljava/util/function/Predicate;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;)Ljava/util/Optional;")
    private Optional<BlockPos> getNearestPosition(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Optional<BlockPos>> original) {
        synchronized (lock) {
            return original.call(typePredicate, pos, radius, occupationStatus);
        }
    }

    @WrapMethod(method = "getNearestPosition(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;)Ljava/util/Optional;")
    private Optional<BlockPos> getNearestPosition(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, Predicate<BlockPos> posPredicate, BlockPos pos, int radius, PointOfInterestStorage.OccupationStatus occupationStatus, Operation<Optional<BlockPos>> original) {
        synchronized (lock) {
            return original.call(typePredicate, posPredicate, pos, radius, occupationStatus);
        }
    }

    @WrapMethod(method = "getPosition(Ljava/util/function/Predicate;Ljava/util/function/Predicate;Lnet/minecraft/world/poi/PointOfInterestStorage$OccupationStatus;Lnet/minecraft/util/math/BlockPos;ILnet/minecraft/util/math/random/Random;)Ljava/util/Optional;")
    private Optional<BlockPos> getNearestPosition(Predicate<RegistryEntry<PointOfInterestType>> typePredicate, Predicate<BlockPos> positionPredicate, PointOfInterestStorage.OccupationStatus occupationStatus, BlockPos pos, int radius, Random random, Operation<Optional<BlockPos>> original) {
        synchronized (lock) {
            return original.call(typePredicate, positionPredicate, occupationStatus, pos, radius, random);
        }
    }
}