package com.llmsearch.server.concurrent;

import java.util.ArrayDeque;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadPool {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition allDone  = lock.newCondition();
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private final Thread[] workers;
    private int unfinished = 0;
    private int failedTasks = 0;
    private boolean shutdown = false;

    public ThreadPool(int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("Less than 1 thread set!");
        }

        workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(new Runnable() {
                public void run() {workerLoop();}
            }, "worker-" + (i + 1));

            workers[i].start();
        }
    }

    private void workerLoop() {
        while (true) {
            Runnable task;

            // get task from the queue
            lock.lock();
            try {
                while (queue.isEmpty() && !shutdown) {
                    notEmpty.await(); // lock here is released by await()
                }
                if (queue.isEmpty()) {
                    return;
                }
                task = queue.poll();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                lock.unlock();
            }

            // run task
            boolean failed = false;
            try {
                task.run();
            } catch (Throwable t) {
                failed = true;
                System.err.println(Thread.currentThread().getName() + ": task failed " + t);
            } finally {
                lock.lock();
                try {
                    // counted before unfinished reaches 0, so awaitCompletion() callers see every failure
                    if (failed) {
                        failedTasks++;
                    }
                    unfinished--;
                    if (unfinished == 0) {
                        allDone.signalAll();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null");
        }

        lock.lock();
        try {
            if (shutdown) {
                throw new IllegalStateException("Can't add, during shutdown sequence!");
            }
            queue.add(task);
            unfinished++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public void awaitCompletion() throws InterruptedException {
        lock.lock();
        try {
            while (unfinished > 0) {
                allDone.await();
            }
        } finally {
            lock.unlock();
        }
    }

    public int failedTasks() {
        lock.lock();
        try {
            return failedTasks;
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            shutdown = true;
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }
}