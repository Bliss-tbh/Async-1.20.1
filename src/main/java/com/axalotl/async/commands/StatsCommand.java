package com.axalotl.async.commands;

import com.axalotl.async.config.AsyncConfig;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.axalotl.async.ParallelProcessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.axalotl.async.commands.AsyncCommand.prefix;
import static net.minecraft.server.command.CommandManager.literal;

public class StatsCommand {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.##");
    private static final int MAX_SAMPLES = 100;
    private static final long SAMPLING_INTERVAL_MS = 100;

    private static final Queue<Integer> threadSamples = new ConcurrentLinkedQueue<>();
    private static volatile boolean isRunning = true;
    private static Thread statsThread;

    public static LiteralArgumentBuilder<ServerCommandSource> registerStatus(LiteralArgumentBuilder<ServerCommandSource> root) {
        return root.then(literal("stats")
                .executes(cmdCtx -> {
                    showGeneralStats(cmdCtx.getSource());
                    return 1;
                })
                .then(literal("entity")
                        .executes(cmdCtx -> {
                            showEntityStats(cmdCtx.getSource());
                            return 1;
                        })));
    }

    private static void showGeneralStats(ServerCommandSource source) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        double avgThreads = calculateAverageThreads();
        double threadUtilization = (avgThreads / availableProcessors) * 100.0;

        MutableText message = prefix.copy()
                .append(Text.literal("Performance Statistics ").styled(style -> style.withColor(Formatting.GOLD)))
                .append(Text.literal("\nActive Processing Threads: ").styled(style -> style.withColor(Formatting.WHITE)))
                .append(Text.literal(DECIMAL_FORMAT.format(avgThreads)).styled(style -> style.withColor(Formatting.GREEN)))
                .append(Text.literal(" / " + availableProcessors).styled(style -> style.withColor(Formatting.GRAY)))
                .append(Text.literal("\nThread Utilization: ").styled(style -> style.withColor(Formatting.WHITE)))
                .append(Text.literal(DECIMAL_FORMAT.format(threadUtilization) + "%").styled(style -> style.withColor(Formatting.GREEN)))
                .append(Text.literal("\nAsync Status: ").styled(style -> style.withColor(Formatting.WHITE)))
                .append(Text.literal(AsyncConfig.disabled ? "Disabled" : "Enabled").styled(style ->
                        style.withColor(AsyncConfig.disabled ? Formatting.RED : Formatting.GREEN)));

        source.sendFeedback(() -> message, true);
    }

    private static void showEntityStats(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        MutableText message = prefix.copy()
                .append(Text.literal("Entity Statistics ").styled(style -> style.withColor(Formatting.GOLD)));

        AtomicInteger totalEntities = new AtomicInteger(0);
        AtomicInteger totalAsyncEntities = new AtomicInteger(0);

        server.getWorlds().forEach(world -> {
            String worldName = world.getRegistryKey().getValue().toString();
            AtomicInteger worldCount = new AtomicInteger(0);
            AtomicInteger asyncCount = new AtomicInteger(0);

            world.entityList.forEach(entity -> {
                if (entity.isAlive()) {
                    worldCount.incrementAndGet();
                    totalEntities.incrementAndGet();
                    if (!ParallelProcessor.shouldTickSynchronously(entity)) {
                        asyncCount.incrementAndGet();
                        totalAsyncEntities.incrementAndGet();
                    }
                }
            });

            message.append(Text.literal("\n" + worldName + ": ").styled(style -> style.withColor(Formatting.YELLOW)))
                    .append(Text.literal(String.valueOf(worldCount.get())).styled(style -> style.withColor(Formatting.GREEN)))
                    .append(Text.literal(" entities (").styled(style -> style.withColor(Formatting.GRAY)))
                    .append(Text.literal(String.valueOf(asyncCount.get())).styled(style -> style.withColor(Formatting.AQUA)))
                    .append(Text.literal(" async)").styled(style -> style.withColor(Formatting.GRAY)));
        });

        message.append(Text.literal("\nTotal Entities: ").styled(style -> style.withColor(Formatting.WHITE)))
                .append(Text.literal(String.valueOf(totalEntities.get())).styled(style -> style.withColor(Formatting.GOLD)))
                .append(Text.literal(" (").styled(style -> style.withColor(Formatting.GRAY)))
                .append(Text.literal(String.valueOf(totalAsyncEntities.get())).styled(style -> style.withColor(Formatting.AQUA)))
                .append(Text.literal(" async)").styled(style -> style.withColor(Formatting.GRAY)));

        source.sendFeedback(() -> message, true);
    }

    private static double calculateAverageThreads() {
        if (threadSamples.isEmpty()) {
            return 0.0;
        }
        int sum = 0;
        int count = 0;
        for (Integer sample : threadSamples) {
            sum += sample;
            count++;
        }
        return count > 0 ? (double) sum / count : 0.0;
    }

    public static void runStatsThread() {
        if (statsThread != null && statsThread.isAlive()) {
            return;
        }

        statsThread = new Thread(() -> {
            while (isRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    updateStats();
                    Thread.sleep(SAMPLING_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Async-Stats-Thread");

        statsThread.setDaemon(true);
        statsThread.start();
    }

    private static void updateStats() {
        if (AsyncConfig.disabled) {
            resetStats();
            return;
        }

        int currentThreads = ParallelProcessor.currentEntities.get();

        threadSamples.offer(currentThreads);

        while (threadSamples.size() > MAX_SAMPLES) {
            threadSamples.poll();
        }
    }

    private static void resetStats() {
        threadSamples.clear();
    }

    public static void shutdown() {
        isRunning = false;
        if (statsThread != null) {
            statsThread.interrupt();
        }
    }
}