package com.axalotl.async.parallelised.thread;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * A priority queue with fixed number of priorities and allows changing priorities of elements.
 *
 * @param <E> the type of elements held in this collection
 */
public class DynamicPriorityQueue<E> {

    private final AtomicIntegerArray taskCount;
    private final ConcurrentLinkedQueue<E>[] priorities;
    private final ConcurrentHashMap<E, Integer> priorityMap = new ConcurrentHashMap<>();

    public DynamicPriorityQueue(int priorityCount) {
        this.taskCount = new AtomicIntegerArray(priorityCount);
        //noinspection unchecked
        this.priorities = new ConcurrentLinkedQueue[priorityCount];
        for (int i = 0; i < priorityCount; i++) {
            this.priorities[i] = new ConcurrentLinkedQueue<>();
        }
    }

    public void enqueue(E element, int priority) {
        if (priority < 0 || priority >= priorities.length)
            throw new IllegalArgumentException("Priority out of range");
        if (this.priorityMap.putIfAbsent(element, priority) != null)
            throw new IllegalArgumentException("Element already in queue");

        this.priorities[priority].add(element);
        this.taskCount.incrementAndGet(priority);
    }

    public E dequeue() {
        for (int i = 0; i < this.priorities.length; i ++) {
            if (this.taskCount.get(i) == 0) continue;
            E element = priorities[i].poll();
            if (element != null) {
                this.taskCount.decrementAndGet(i);
                this.priorityMap.remove(element);
                return element;
            }
        }
        return null;
    }

    public boolean contains(E element) {
        return priorityMap.containsKey(element);
    }

    public void remove(E element) {
        final Integer remove = this.priorityMap.remove(element);
        if (remove == null) return;
        boolean removed = this.priorities[remove].remove(element); // best-effort
        if (removed) this.taskCount.decrementAndGet(remove);
    }

    public int size() {
        return priorityMap.size();
    }

}
