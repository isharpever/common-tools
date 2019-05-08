package com.dianwoda.isharpever.tool.executor;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.dianwoda.isharpever.tool.mdc.LogUniqueKeyUtil;
import com.dianwoda.isharpever.tool.utils.NetUtil;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public abstract class ExecutorServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceUtil.class);

    private static final MonitorUtil monitor = MonitorFactory.connect();

    public static ExecutorService buildExecutorService(int maximumPoolSize, String namePrefix) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new DefaultNamedThreadFactory(namePrefix));
        return getProxy(executor, namePrefix);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(int maximumPoolSize, String namePrefix) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(maximumPoolSize,
                new DefaultNamedThreadFactory(namePrefix));
        return getProxy(executor, namePrefix);
    }

    private static class DefaultNamedThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private String namePrefix;

        private DefaultNamedThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * 返回代理线程池对象
     * @param executor
     * @param name
     * @return
     */
    private static <T> T getProxy(final ThreadPoolExecutor executor, String name) {
        CustomeInvocationHandler customeInvocationHandler = new CustomeInvocationHandler(executor, name);

        List<Class<?>> interfaceList = ClassUtils.getAllInterfaces(executor.getClass());
        Class[] interfaces = new Class[interfaceList.size()];
        interfaceList.toArray(interfaces);

        return (T) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(), interfaces, customeInvocationHandler);
    }

    /**
     * 代理处理: <li>1.异常日志 <li>2.监控
     */
    private static class CustomeInvocationHandler implements InvocationHandler {

        /**
         * 被代理方法:提交任务
         */
        private static final List<String> METHOD_NAME_SUBMIT = new ArrayList<>(8);
        static {
            METHOD_NAME_SUBMIT.add("submit");
            METHOD_NAME_SUBMIT.add("execute");
            METHOD_NAME_SUBMIT.add("schedule");
            METHOD_NAME_SUBMIT.add("scheduleAtFixedRate");
            METHOD_NAME_SUBMIT.add("scheduleWithFixedDelay");
        }

        private ThreadPoolExecutor threadPoolExecutor;
        private String threadPoolName;

        private CustomeInvocationHandler(ThreadPoolExecutor threadPoolExecutor,
                String threadPoolName) {
            this.threadPoolExecutor = threadPoolExecutor;
            this.threadPoolName = threadPoolName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 线程池监控
            this.monitorThreadPoolExecutor();

            if (METHOD_NAME_SUBMIT.contains(method.getName())) {
                String logKey = MDC.get(LogUniqueKeyUtil.LOG_KEY);
                if (StringUtils.isBlank(logKey)) {
                    logKey = LogUniqueKeyUtil.generateKey();
                }
                final String flogKey = logKey;

                final Object arg0 = args[0];
                if (arg0 instanceof Runnable) {
                    args[0] = (Runnable) () -> {
                        MDC.put(LogUniqueKeyUtil.LOG_KEY, flogKey);
                        final Runnable task = (Runnable) arg0;
                        try {
                            task.run();
                            if (task instanceof Future<?>) {
                                final Future<?> future = (Future<?>) task;

                                if (future.isDone()) {
                                    try {
                                        future.get();
                                    } catch (final CancellationException | ExecutionException ce) {
                                        logger.error(String.format("Thread:%s catch exception.",
                                                Thread.currentThread().getName()), ce);
                                    } catch (final InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                    }
                                }
                            }
                        } catch (final RuntimeException re) {
                            logger.error(String.format("Thread:%s catch exception.",
                                    Thread.currentThread().getName()), re);
                        } catch (final Error e) {
                            logger.error(String.format("Thread:%s catch error.",
                                    Thread.currentThread().getName()), e);
                            throw e;
                        } finally {
                            // 线程池监控
                            this.monitorThreadPoolExecutor();
                        }
                    };
                } else if (arg0 instanceof Callable<?>) {
                    args[0] = (Callable<Object>) () -> {
                        MDC.put(LogUniqueKeyUtil.LOG_KEY, flogKey);
                        final Callable<?> task = (Callable<?>) arg0;
                        try {
                            return task.call();
                        } catch (final Exception e) {
                            logger.error(String.format("Thread:%s catch exception.",
                                    Thread.currentThread().getName()), e);
                            throw e;
                        } catch (final Error e) {
                            logger.error(String.format("Thread:%s catch error.",
                                    Thread.currentThread().getName()), e);
                            throw e;
                        } finally {
                            // 线程池监控
                            this.monitorThreadPoolExecutor();
                        }
                    };
                }
            }
            return method.invoke(threadPoolExecutor, args);
        }

        /**
         * 线程池监控
         */
        private void monitorThreadPoolExecutor() {
            try {
                ThreadPoolExecutor executor = this.threadPoolExecutor;
                int corePoolSize = executor.getCorePoolSize();
                int maximumPoolSize = executor.getMaximumPoolSize();
                // 当前线程数
                int poolSize = executor.getPoolSize();
                // 最大线程数
                int largestPoolSize = executor.getLargestPoolSize();
                // 活动线程数
                int activeCount = executor.getActiveCount();
                // 完成的任务数
                long completedTaskCount = executor.getCompletedTaskCount();
                // 完成的任务数+正在执行的任务数
                long taskCount = executor.getTaskCount();
                // 等待执行的任务数
                int queueSize = executor.getQueue().size();

                MonitorPoint point = MonitorPoint
                        .monitorKey("isharpever.threadpool")
                        .addTag("ip", NetUtil.getLocalHostAddress())
                        .addTag("name", this.threadPoolName)
                        .addField("corePoolSize", corePoolSize)
                        .addField("maximumPoolSize", maximumPoolSize)
                        .addField("poolSize", poolSize)
                        .addField("largestPoolSize", largestPoolSize)
                        .addField("activeCount", activeCount)
                        .addField("completedTaskCount", completedTaskCount)
                        .addField("taskCount", taskCount)
                        .addField("queueSize", queueSize)
                        .build();
                monitor.writePoint(point);
            } catch (Exception e) {
                logger.warn("--- 线程池监控异常 threadPoolName={}", this.threadPoolName, e);
            }
        }
    }

}
