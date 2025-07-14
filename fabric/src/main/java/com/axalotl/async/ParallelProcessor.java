package com.axalotl.async;

import com.axalotl.async.config.AsyncConfig;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.Bootstrap;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.world.GameRules;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ParallelProcessor {
    private static final Logger LOGGER = LogManager.getLogger(ParallelProcessor.class);

    @Getter
    @Setter
    private static MinecraftServer server;

    public static AtomicInteger currentEntities = new AtomicInteger();
    private static final AtomicInteger ThreadPoolID = new AtomicInteger();
    private static ExecutorService tickPool;
    private static final Queue<CompletableFuture<Void>> taskQueue = new ConcurrentLinkedQueue<>();
    private static final Set<UUID> blacklistedEntity = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> portalTickSyncMap = new ConcurrentHashMap<>();
    private static final Map<String, Set<Thread>> mcThreadTracker = new ConcurrentHashMap<>();
    private static final Set<Class<?>> specialEntities = Set.of(
            FallingBlockEntity.class,
            PlayerEntity.class,
            ServerPlayerEntity.class
    );

    public static void setupThreadPool(int parallelism) {
        ForkJoinPool.ForkJoinWorkerThreadFactory tickThreadFactory = pool -> {
            ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("Async-Tick-Pool-Thread-" + ThreadPoolID.getAndIncrement());
            registerThread("Async-Tick", worker);
            worker.setDaemon(true);
            worker.setContextClassLoader(Async.class.getClassLoader());
            return worker;
        };

        tickPool = new ForkJoinPool(parallelism, tickThreadFactory, (t, e) ->
                LOGGER.error("Uncaught exception in thread {}: {}", t.getName(), e), true);
        LOGGER.info("Initialized ForkJoinPool with {} threads", parallelism);
    }

    public static void registerThread(String poolName, Thread thread) {
        mcThreadTracker.computeIfAbsent(poolName, key -> ConcurrentHashMap.newKeySet()).add(thread);
    }

    private static boolean isThreadInPool(Thread thread) {
        return mcThreadTracker.getOrDefault("Async-Tick", Set.of()).contains(thread);
    }

    public static boolean isServerExecutionThread() {
        return isThreadInPool(Thread.currentThread());
    }

    public static void callEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        if (shouldTickSynchronously(entity)) {
            tickSynchronously(tickConsumer, entity);
        } else {
            if (!tickPool.isShutdown() && !tickPool.isTerminated()) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                        performAsyncEntityTick(tickConsumer, entity), tickPool
                ).exceptionally(e -> {
                    logEntityError("Error in async tick, switching to synchronous", entity, e);
                    tickSynchronously(tickConsumer, entity);
                    blacklistedEntity.add(entity.getUuid());
                    return null;
                });
                taskQueue.add(future);
            }
        }
    }

    public static boolean shouldTickSynchronously(Entity entity) {
        UUID entityId = entity.getUuid();
        boolean requiresSyncTick = AsyncConfig.disabled ||
                        entity instanceof ProjectileEntity ||
                        entity instanceof AbstractMinecartEntity ||
                        entity instanceof ServerPlayerEntity ||
                        specialEntities.contains(entity.getClass()) ||
                        blacklistedEntity.contains(entityId) ||
                        AsyncConfig.synchronizedEntities.contains(EntityType.getId(entity.getType()));
        if (requiresSyncTick) {
            return true;
        }
        if (portalTickSyncMap.containsKey(entityId)) {
            int ticksLeft = portalTickSyncMap.get(entityId);
            if (ticksLeft > 0) {
                portalTickSyncMap.put(entityId, ticksLeft - 1);
                return true;
            }
        }
        if (isPortalTickRequired(entity)) {
            portalTickSyncMap.put(entityId, 39);
            return true;
        }
        return false;
    }

    private static boolean isPortalTickRequired(Entity entity) {
        return entity.inNetherPortal;
    }

    private static void tickSynchronously(Consumer<Entity> tickConsumer, Entity entity) {
        try {
            if (entity == null || entity.isRemoved()) return;
            tickConsumer.accept(entity);
        } catch (Exception e) {
            logEntityError("Error ticking synchronously", entity, e);
        }
    }

    private static void performAsyncEntityTick(Consumer<Entity> tickConsumer, Entity entity) {
        currentEntities.incrementAndGet();
        try {
            tickConsumer.accept(entity);
        } finally {
            currentEntities.decrementAndGet();
        }
    }

    public static void asyncSpawn(ServerWorld world, WorldChunk chunk, SpawnHelper.Info info, boolean spawnAnimals, boolean spawnMonsters, boolean rareSpawn) {
        if (AsyncConfig.enableAsyncSpawn) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    SpawnHelper.spawn(world, chunk, info, spawnAnimals, spawnMonsters, rareSpawn), tickPool
            ).exceptionally(e -> {
                LOGGER.error("Error in async spawn tick, switching to synchronous", e);
                SpawnHelper.spawn(world, chunk, info, spawnAnimals, spawnMonsters, rareSpawn);
                return null;
            });
            taskQueue.add(future);
        } else {
            SpawnHelper.spawn(world, chunk, info, spawnAnimals, spawnMonsters, rareSpawn);
        }
    }

    public static void postEntityTick() {
        if (!AsyncConfig.disabled) {
            List<CompletableFuture<?>> futuresList = new ArrayList<>();
            CompletableFuture<?> future;
            while ((future = taskQueue.poll()) != null) {
                futuresList.add(future);
            }

            CompletableFuture<?> allTasks = CompletableFuture.allOf(
                    futuresList.toArray(new CompletableFuture[0])
            );

            long maxTickTime;

            if (server instanceof MinecraftDedicatedServer dedicatedServer) {
                maxTickTime = dedicatedServer.getMaxTickTime();
            } else {
                maxTickTime = 60000;
            }

            if (maxTickTime > 0) {
                allTasks
                        .orTimeout(maxTickTime, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            Throwable cause = ex instanceof java.util.concurrent.CompletionException
                                    ? ex.getCause() : ex;
                            if (cause instanceof TimeoutException) {
                                crash("Timeout during entity tick processing: ", cause);
                            } else {
                                LOGGER.error("Error during entity tick processing: ", cause);
                            }
                            return null;
                        });
            } else {
                allTasks.exceptionally(ex -> {
                    Throwable cause = ex instanceof java.util.concurrent.CompletionException
                            ? ex.getCause() : ex;
                    LOGGER.error("Error during entity tick processing: ", cause);
                    return null;
                });
            }

            server.getWorlds().forEach(world -> {
                world.getChunkManager().executeQueuedTasks();
                world.getChunkManager().mainThreadExecutor.runTasks(allTasks::isDone);
            });
        }
    }

    public static void stop() {
        if (tickPool != null && !tickPool.isShutdown()) {
            tickPool.shutdown();
        }
    }

    public static void crash(String message, Throwable throwable) {
        String errorMessage = message + throwable.getMessage();
        LOGGER.error(errorMessage, LogUtils.FATAL_MARKER);
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
        StringBuilder stringBuilder = new StringBuilder();
        Error error = new Error("Watchdog");

        for (ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getThreadId() == server.getThread().getId()) {
                error.setStackTrace(threadInfo.getStackTrace());
            }

            stringBuilder.append(threadInfo);
            stringBuilder.append("\n");
        }

        CrashReport crashReport = new CrashReport("Watching Server", error);
        server.addSystemDetails(crashReport.getSystemDetailsSection());
        CrashReportSection crashReportSection = crashReport.addElement("Thread Dump");
        crashReportSection.add("Threads", stringBuilder);

        CrashReportSection threadDumpSection = crashReport.addElement("Async thread dump");
        threadDumpSection.add("All Threads", () -> {
            StringBuilder sb = new StringBuilder();
            Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();
            for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
                Thread t = entry.getKey();
                sb.append(String.format("\"%s\" [%s]%n", t.getName(), t.getState()));
                for (StackTraceElement ste : entry.getValue()) {
                    sb.append("\tat ").append(ste).append("\n");
                }
                sb.append("\n");
            }
            return sb.toString();
        });

        CrashReportSection crashReportSection2 = crashReport.addElement("Performance stats");
        crashReportSection2.add(
                "Random tick rate", () -> server.getSaveProperties().getGameRules().get(GameRules.RANDOM_TICK_SPEED).toString()
        );
        crashReportSection2.add(
                "Level stats",
                () -> Streams.stream(server.getWorlds())
                        .map(world -> world.getRegistryKey() + ": " + world.getDebugString())
                        .collect(Collectors.joining(",\n"))
        );
        Bootstrap.println("Crash report:\n" + crashReport.asString());
        Path path = server.getRunDirectory().toPath().resolve("crash-reports").resolve("crash-" + Util.getFormattedCurrentTime() + "-server.txt");
        if (crashReport.writeToFile(path.toFile())) {
            LOGGER.error("This crash report has been saved to: {}", path.toAbsolutePath());
        } else {
            LOGGER.error("We were unable to save this crash report to disk.");
        }

        shutdown();
    }

    private static void shutdown() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                public void run() {
                    Runtime.getRuntime().halt(1);
                }
            }, 10000L);
            System.exit(1);
        } catch (Throwable var2) {
            Runtime.getRuntime().halt(1);
        }
    }

    private static void logEntityError(String message, Entity entity, Throwable e) {
        LOGGER.error("{} Entity Type: {}, UUID: {}", message, entity.getType().getName(), entity.getUuid(), e);
    }
}