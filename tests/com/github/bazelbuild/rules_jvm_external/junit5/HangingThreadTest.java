package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

public class HangingThreadTest {

    // This thread isn't a daemon thread, so after the test is complete
    // the JVM will wait for it to exit. However, all the tests are now
    // done, so the junit runner should exit cleanly.
    private final static AtomicBoolean STARTED = new AtomicBoolean(false);
    private final static Thread SNAGGING_THREAD = new Thread(() -> {
        STARTED.set(true);
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            // Swallow
        }
    });
    static {
        SNAGGING_THREAD.start();
    }

    // We need this to get the thread to start running.
    @Test
    public void hangForever() throws InterruptedException {
        while (!STARTED.get()) {
            Thread.sleep(100);
        }
    }

}
