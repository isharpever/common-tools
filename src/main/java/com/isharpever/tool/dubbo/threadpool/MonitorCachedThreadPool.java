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

public class MonitorCachedThreadPool implements ThreadPool {

    @Override
    public Executor getExecutor(URL url) {
        String name = url.getParameter(CommonConstants.THREAD_NAME_KEY, CommonConstants.DEFAULT_THREAD_NAME);
        int cores = url.getParameter(CommonConstants.CORE_THREADS_KEY, CommonConstants.DEFAULT_THREADS);
        int threads = url.getParameter(CommonConstants.THREADS_KEY, Integer.MAX_VALUE);
        if (threads < cores) {
            threads = Integer.min(cores * 2, Integer.MAX_VALUE);
        }
        int queues = url.getParameter(CommonConstants.QUEUES_KEY, CommonConstants.DEFAULT_QUEUES);
        int alive = url.getParameter(CommonConstants.ALIVE_KEY, CommonConstants.DEFAULT_ALIVE);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(cores, threads, alive, TimeUnit.MILLISECONDS,
                queues == 0 ? new SynchronousQueue<>() :
                        (queues < 0 ? new LinkedBlockingQueue<>()
                                : new LinkedBlockingQueue<>(queues)),
                new NamedThreadFactory(name, true), new AbortPolicyWithReport(name, url));
        return ExecutorServiceUtil.buildExecutorService(executor, name, "isharpever.threadpool.dubbo");
    }
}
