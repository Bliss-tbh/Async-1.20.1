package com.axalotl.async.parallelised.thread;

public interface Task {

    void run(Runnable releaseLocks);

    void propagateException(Throwable t);

    LockToken[] lockTokens();

    int priority();

}
