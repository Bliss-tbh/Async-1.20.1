package com.axalotl.async;

import com.axalotl.async.config.AsyncConfig;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ParallelProcessor {
    private static final Logger LOGGER = LogManager.getLogger();
    @Getter
    @Setter
    public static MinecraftServer server;
    public static AtomicInteger currentEntities = new AtomicInteger();
    private static final AtomicInteger ThreadPoolID = new AtomicInteger();
    private static ExecutorService tickPool;
    private static final Queue<CompletableFuture<Void>> taskQueue = new ConcurrentLinkedQueue<>();
    public static final Set<UUID> blacklistedEntity = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<Thread>> mcThreadTracker = new ConcurrentHashMap<>();
    public static final Set<Class<?>> specialEntities = Set.of(
            FallingBlockEntity.class,
            PlayerEntity.class,
            ServerPlayerEntity.class
    );

    public static void setupThreadPool(int parallelism) {
        ForkJoinPool.ForkJoinWorkerThreadFactory tickThreadFactory = p -> {
            ForkJoinWorkerThread factory = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            factory.setName("Async-Tick-Pool-Thread-%d".formatted(ThreadPoolID.getAndIncrement()));
            regThread("Async-Tick", factory);
            factory.setDaemon(true);
            factory.setContextClassLoader(Async.class.getClassLoader());
            return factory;
        };
        tickPool = new ForkJoinPool(parallelism, tickThreadFactory, (t, e) -> LOGGER.error("Uncaught exception in thread {}: {}", t.getName(), e), true);
        LOGGER.info("Initialized ForkJoinPool with {} threads", parallelism);
    }

    public static void regThread(String poolName, Thread thread) {
        mcThreadTracker.computeIfAbsent(poolName, s -> ConcurrentHashMap.newKeySet()).add(thread);
    }

    public static boolean isThreadPooled(String poolName, Thread t) {
        return mcThreadTracker.containsKey(poolName) && mcThreadTracker.get(poolName).contains(t);
    }

    public static boolean serverExecutionThreadPatch() {
        return isThreadPooled("Async-Tick", Thread.currentThread());
    }

    public static void callEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        if (shouldTickSynchronously(entity)) {
            tickSynchronously(tickConsumer, entity);
        } else {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> performAsyncEntityTick(tickConsumer, entity),
                    tickPool
            ).exceptionally(e -> {
                LOGGER.error("Error ticking entity {} asynchronously, switching to synchronous processing", entity.getType().getName(), e);
                tickSynchronously(tickConsumer, entity);
                blacklistedEntity.add(entity.getUuid());
                return null;
            });
            taskQueue.add(future);
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        if (AsyncConfig.disabled || blacklistedEntity.contains(entity.getUuid())) {
            return true;
        }
        return specialEntities.contains(entity.getClass()) ||
                tickPortalSynchronously(entity) ||
                entity instanceof AbstractMinecartEntity ||
                (AsyncConfig.disableTNT && entity instanceof TntEntity);
    }

    private static boolean tickPortalSynchronously(Entity entity) {
        return entity.portalManager != null && entity.portalManager.isInPortal();
    }


    private static void tickSynchronously(Consumer<Entity> tickConsumer, Entity entity) {
        try {
            tickConsumer.accept(entity);
        } catch (Exception e) {
            LOGGER.error("Error ticking entity {} synchronously",
                    entity.getType().getName(),
                    e);
        }
    }

    private static void performAsyncEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        try {
            currentEntities.incrementAndGet();
            tickConsumer.accept(entity);
        } finally {
            currentEntities.decrementAndGet();
        }
    }


    public static void postEntityTick() {
        if (!AsyncConfig.disabled) {
            try {
                List<CompletableFuture<Void>> futuresList = new ArrayList<>(taskQueue);
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(futuresList.toArray(new CompletableFuture[0]));
                allTasks.orTimeout(60, TimeUnit.SECONDS).exceptionally(ex -> {
                    LOGGER.error("Timeout during entity tick processing", ex);
                    server.shutdown();
                    return null;
                });
                server.getWorlds().forEach(world -> {
                    world.getChunkManager().executeQueuedTasks();
                    world.getChunkManager().mainThreadExecutor.runTasks(allTasks::isDone);
                });
            } catch (CompletionException e) {
                LOGGER.error("Critical error during entity tick processing", e);
                server.shutdown();
            } finally {
                taskQueue.clear();
            }
        }
    }

    public static void stop() {
        tickPool.shutdown();
        try {
            if (!tickPool.awaitTermination(10, TimeUnit.SECONDS)) {
                tickPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            tickPool.shutdownNow();
        }
    }
}