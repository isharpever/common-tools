package com.isharpever.tool.dubbo.threadpool;

import com.isharpever.tool.executor.ExecutorServiceUtil;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.threadpool.ThreadPool;
import org.apache.dubbo.common.threadpool.support.AbortPolicyWithReport;
import org.apache.dubbo.common.utils.NamedThreadFactory;

public class MonitorFixedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(CommonConstants.THREAD_NAME_KEY, CommonConstants.DEFAULT_THREAD_NAME);
        int threads = url.getParameter(CommonConstants.THREADS_KEY, CommonConstants.DEFAULT_THREADS);
        int queues = url.getParameter(CommonConstants.QUEUES_KEY, CommonConstants.DEFAULT_QUEUES);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<>() :
                        (queues < 0 ? new LinkedBlockingQueue<>()
                                : new LinkedBlockingQueue<>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
        return ExecutorServiceUtil.buildExecutorService(executor, name, "isharpever.threadpool.dubbo");
    }
}
