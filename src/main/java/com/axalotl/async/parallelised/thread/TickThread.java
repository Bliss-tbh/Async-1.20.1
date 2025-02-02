package com.axalotl.async.parallelised.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class TickThread extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger("Async Tick Thread");

    private final ExecutorManager executorManager;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public TickThread(ExecutorManager executorManager) {
        this.executorManager = executorManager;
    }

    @Override
    public void run() {
        main_loop:
        while (true) {
            if (this.shutdown.get()) {
                return;
            }
            if (pollTasks()) {
                continue;
            }
            synchronized (this.executorManager.workerMonitor) {
                if (this.executorManager.hasPendingTasks()) continue main_loop;
                try {
                    this.executorManager.workerMonitor.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private boolean pollTasks() {
        final Task task = executorManager.pollExecutableTask();
        try {
            if (task != null) {
                AtomicBoolean released = new AtomicBoolean(false);
                try {
                    task.run(() -> {
                        if (released.compareAndSet(false, true)) {
                            executorManager.releaseLocks(task);
                        }
                    });
                } catch (Throwable t) {
                    try {
                        if (released.compareAndSet(false, true)) {
                            executorManager.releaseLocks(task);
                        }
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                        LOGGER.error("Exception thrown while releasing locks", t);
                    }
                    try {
                        task.propagateException(t);
                    } catch (Throwable t1) {
                        t.addSuppressed(t1);
                        LOGGER.error("Exception thrown while propagating exception", t);
                    }
                }
                return true;
            }
            return false;
        } catch (Throwable t) {
            LOGGER.error("Exception thrown while executing task", t);
            return true;
        }
    }

    public void shutdown() {
        shutdown.set(true);
        LockSupport.unpark(this);
    }


}
