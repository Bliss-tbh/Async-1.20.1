package com.axalotl.async.parallelised.thread;

import java.util.Objects;

public class TickTask implements Task {

    private final Runnable wrapped;
    private final int priority;

    public TickTask(Runnable wrapped, int priority) {
        this.wrapped = Objects.requireNonNull(wrapped);
        this.priority = priority;
    }

    @Override
    public void run(Runnable releaseLocks) {
        try {
            wrapped.run();
        } finally {
            releaseLocks.run();
        }
    }

    @Override
    public void propagateException(Throwable t) {
        t.printStackTrace();
    }

    @Override
    public LockToken[] lockTokens() {
        return new LockToken[0];
    }

    @Override
    public int priority() {
        return this.priority;
    }
}
