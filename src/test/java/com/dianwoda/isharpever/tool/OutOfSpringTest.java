package com.dianwoda.isharpever.tool;

import com.dianwoda.isharpever.tool.executor.ExecutorServiceUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class OutOfSpringTest {

    @Test
    public void testExecutorServiceUtil() throws Exception {
        ScheduledExecutorService scheduledExecutorService = ExecutorServiceUtil
                .buildScheduledThreadPool(1, "test2-");
        scheduledExecutorService.schedule(() -> {
            System.out.println("just a test2");
        }, 5, TimeUnit.SECONDS);

        ExecutorService executorService = ExecutorServiceUtil.buildExecutorService(1, "test1-");
        executorService.submit(() -> {
            System.out.println("just a test1");
        });

        synchronized (this) {
            this.wait();
        }
    }

}
