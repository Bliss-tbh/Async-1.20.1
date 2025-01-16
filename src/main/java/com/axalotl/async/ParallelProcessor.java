package com.axalotl.async;

import com.axalotl.async.config.AsyncConfig;
import com.axalotl.async.parallelised.ConcurrentCollections;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
    private static final Map<String, Set<Thread>> mcThreadTracker = ConcurrentCollections.newHashMap();
    public static final Set<Class<?>> specialEntities = Set.of(
            FallingBlockEntity.class,
            Player.class,
            ServerPlayer.class
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
                logEntityError("Error ticking asynchronously, switching to synchronous processing", entity, e);
                tickSynchronously(tickConsumer, entity);
                blacklistedEntity.add(entity.getUUID());
                return null;
            });
            taskQueue.add(future);
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        return AsyncConfig.disabled
                || tickPortalSynchronously(entity)
                || entity.hasExactlyOnePlayerPassenger()

                || AsyncConfig.synchronizedEntities.contains(EntityType.getKey(entity.getType()))

                || blacklistedEntity.contains(entity.getUUID())
                || specialEntities.contains(entity.getClass());
    }

    private static boolean tickPortalSynchronously(Entity entity) {
        if (entity.portalProcess != null && entity.portalProcess.isInsidePortalThisTick()) {
            return true;
        }
        return entity instanceof Projectile;
    }

    private static void tickSynchronously(Consumer<Entity> tickConsumer, Entity entity) {
        try {
            tickConsumer.accept(entity);
        } catch (Exception e) {
            logEntityError("Error ticking synchronously", entity, e);
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
                    server.stopServer();
                    return null;
                });
                server.getAllLevels().forEach(world -> {
                    world.getChunkSource().pollTask();
                    world.getChunkSource().mainThreadProcessor.managedBlock(allTasks::isDone);
                });
            } catch (CompletionException e) {
                LOGGER.error("Critical error during entity tick processing", e);
                server.stopServer();
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

    private static void logEntityError(String message, Entity entity, Throwable e) {
        LOGGER.error("{} Entity Type: {}, UUID: {}", message, entity.getType().getDescription(), entity.getUUID(), e);
    }
}
