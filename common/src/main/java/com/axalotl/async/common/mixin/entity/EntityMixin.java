package com.axalotl.async.common.mixin.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract Level level();

    @Shadow
    private ImmutableList<Entity> passengers;

    @Unique
    private final AtomicReference<ImmutableList<Entity>> async$passengersAtomic =
            new AtomicReference<>(ImmutableList.of());

    @WrapMethod(method = "setRemoved")
    private void setRemoved(Entity.RemovalReason reason, Operation<Void> original) {
        original.call(reason);
        async$passengersAtomic.set(ImmutableList.of());
    }

    @WrapMethod(method = "getFeetBlockState")
    private BlockState wrapFeetBlockState(Operation<BlockState> original) {
        BlockState blockState = original.call();

        if (blockState == null && this.level() instanceof ServerLevel serverLevel) {
            BlockPos pos = this.blockPosition();

            LevelChunk chunk = serverLevel.getChunkSource()
                    .getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);

            if (chunk != null) {
                return chunk.getBlockState(pos);
            }

            ChunkAccess access = serverLevel.getChunkSource()
                    .getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.FULL, true);

            if (access instanceof LevelChunk levelChunk) {
                return levelChunk.getBlockState(pos);
            }

            return Blocks.AIR.defaultBlockState();
        }

        return blockState;
    }

    @WrapMethod(method = "addPassenger")
    private void addPassenger(Entity passenger, Operation<Void> original) {
        Entity self = (Entity)(Object)this;

        if (passenger.getVehicle() != self) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        }

        while (true) {
            ImmutableList<Entity> current = async$passengersAtomic.get();
            ImmutableList<Entity> updated;

            if (current.isEmpty()) {
                updated = ImmutableList.of(passenger);
            } else {
                List<Entity> list = Lists.newArrayList(current);
                if (!this.level().isClientSide() && passenger instanceof Player && !(list.get(0) instanceof Player)) {
                    list.add(0, passenger);
                } else {
                    list.add(passenger);
                }
                updated = ImmutableList.copyOf(list);
            }

            if (async$passengersAtomic.compareAndSet(current, updated)) {
                this.passengers = updated;
                break;
            }
        }
    }

    @WrapMethod(method = "removePassenger")
    private void removePassenger(Entity passenger, Operation<Void> original) {
        Entity self = (Entity)(Object)this;

        if (passenger.getVehicle() == self) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        }

        while (true) {
            ImmutableList<Entity> current = async$passengersAtomic.get();
            ImmutableList<Entity> updated;

            if (current.size() == 1 && current.get(0) == passenger) {
                updated = ImmutableList.of();
            } else {
                updated = current.stream()
                        .filter(p -> p != passenger)
                        .collect(ImmutableList.toImmutableList());
            }

            if (async$passengersAtomic.compareAndSet(current, updated)) {
                this.passengers = updated;
                passenger.boardingCooldown = 60;
                break;
            }
        }
    }

    @WrapMethod(method = "ejectPassengers")
    private void ejectPassengers(Operation<Void> original) {
        ImmutableList<Entity> snapshot = async$passengersAtomic.get();

        for (int i = snapshot.size() - 1; i >= 0; i--) {
            snapshot.get(i).stopRiding();
        }
    }

    @Inject(method = "getPassengers", at = @At("HEAD"), cancellable = true)
    private void async$getPassengers(CallbackInfoReturnable<List<Entity>> cir) {
        cir.setReturnValue(async$passengersAtomic.get());
    }

    @Inject(method = "getFirstPassenger", at = @At("HEAD"), cancellable = true)
    private void async$getFirstPassenger(CallbackInfoReturnable<Entity> cir) {
        ImmutableList<Entity> snapshot = async$passengersAtomic.get();
        cir.setReturnValue(snapshot.isEmpty() ? null : snapshot.getFirst());
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void async$syncAfterLoad(CompoundTag tag, CallbackInfo ci) {
        async$passengersAtomic.set(this.passengers);
    }
}