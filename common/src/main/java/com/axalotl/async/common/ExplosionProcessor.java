package com.axalotl.async.common;

import com.axalotl.async.common.mixin.world.ExplosionAccessor;
import com.axalotl.async.common.platform.PlatformEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Explosion;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ExplosionProcessor {
    public record ExplosionTask(Explosion explosion, boolean spawnParticles) {
    }

    private static final Logger LOGGER = ParallelProcessor.LOGGER;
    private static final BlockingQueue<ExplosionTask> workQueue = new LinkedBlockingQueue<>();
    private static volatile boolean running = false;
    private static Thread workerThread;
    private static final boolean fabric = PlatformEvents.getInstance().isFabric();

    public static void start() {
        if (running) {
            return;
        }
        running = true;
        workerThread = new Thread(ExplosionProcessor::processQueue, "Async-Explosion-Processor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public static void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    public static void queueExplosion(Explosion explosion, boolean spawnParticles) {
        workQueue.add(new ExplosionTask(explosion, spawnParticles));
    }

    private static void processQueue() {
        while (running) {
            try {
                // Block until a task is available
                ExplosionTask task = workQueue.take();

                // Do the heavy work on this async thread
                task.explosion().explode();
                if (fabric) {
                    task.explosion().finalizeExplosion(task.spawnParticles());
                } else {
                    ServerLevel level = (ServerLevel) ((ExplosionAccessor)task.explosion()).getLevel();
                    level.getServer().execute(() -> task.explosion().finalizeExplosion(task.spawnParticles()));
                }
            } catch (InterruptedException e) {
                // Expected on shutdown, just exit the loop.
                break;
            }
        }
    }
}