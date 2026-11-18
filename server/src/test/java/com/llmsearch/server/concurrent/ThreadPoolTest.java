package com.llmsearch.server.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class ThreadPoolTest {
    @Test
    @Timeout(10)
    void runAllTasks() throws InterruptedException {
        ThreadPool pool = new ThreadPool(4);
        AtomicInteger counter = new AtomicInteger();

        for (int i = 0; i < 1000; i++) {
            pool.submit(new Runnable() {
                public void run() {
                    counter.incrementAndGet();
                }
            });
        }

        pool.awaitCompletion();

        assertEquals(1000, counter.get());
        pool.shutdown();
    }

    @Test
    @Timeout(10)
    void runAllTasksInParallel() throws InterruptedException {
        ThreadPool pool = new ThreadPool(4);
        Set<String> threadNames = ConcurrentHashMap.newKeySet();

        long start = System.nanoTime();
        for (int i = 0; i < 8; i++) {
            pool.submit(new Runnable() {
                public void run() {
                    threadNames.add(Thread.currentThread().getName());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }
        pool.awaitCompletion();
        long ms = (System.nanoTime() - start) / 1000000;

        assertTrue(ms < 400, "took " + ms + " ms, the task did not run in parallel");
        assertTrue(threadNames.size() >= 2, "only " + threadNames.size() + " threads run tasks");
        pool.shutdown();
    }

    @Test 
    @Timeout(10)
    void fallingTaskDoesNotBreakPool() throws InterruptedException {
        ThreadPool pool = new ThreadPool(1);
        AtomicInteger counter = new AtomicInteger();

        pool.submit(new Runnable() {
            public void run() {
                throw new RuntimeException("exception failure in test");
            }
        });

        pool.submit(new Runnable() {
            public void run() {
                counter.incrementAndGet();
            }
        });

        pool.awaitCompletion();

        assertEquals(1, counter.get());
        pool.shutdown();
    }

    @Test
    @Timeout(10)
    void rejectsTasksAfterShutdown() {
        ThreadPool pool = new ThreadPool(2);
        pool.shutdown();

        assertThrows(IllegalStateException.class, () -> pool.submit(() -> {}));
    }

    @Test  
    @Timeout(10)
    void canBeReused() throws InterruptedException {
        ThreadPool pool = new ThreadPool(3);
        AtomicInteger counter = new AtomicInteger();

        for (int round = 1; round <= 5; round++) {
            for (int i = 0; i < 100; i++) {
                pool.submit(new Runnable() {
                    public void run() {
                        counter.incrementAndGet();
                    }
                });
            } 
            pool.awaitCompletion();
            assertEquals(round * 100, counter.get());
        }
        pool.shutdown();
    }
}
