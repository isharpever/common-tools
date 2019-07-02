package com.isharpever.tool.executor;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.isharpever.tool.mdc.LogUniqueKeyUtil;
import com.isharpever.tool.utils.AppNameUtil;
import com.isharpever.tool.utils.NetUtil;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecutorServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceUtil.class);

    public static ExecutorService buildExecutorService(int maximumPoolSize, String poolName) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new DefaultNamedThreadFactory(poolName));
        return getProxy(executor, poolName);
    }

    public static ExecutorService buildExecutorService(int maximumPoolSize, String poolName,
            BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
            RejectedExecutionHandler rejectedPolicy) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, 0L,
                TimeUnit.MILLISECONDS, workQueue, threadFactory, rejectedPolicy);
        return getProxy(executor, poolName);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(int maximumPoolSize, String poolName) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(maximumPoolSize,
                new DefaultNamedThreadFactory(poolName));
        return getProxy(executor, poolName);
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
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
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
                Thread.currentThread().getContextClassLoader(), interfaces,
                customeInvocationHandler);
    }

    /**
     * 代理处理: <li>1.异常日志 <li>2.监控
     */
    private static class CustomeInvocationHandler implements InvocationHandler {
        private static final MonitorUtil monitor = MonitorFactory.connect();
        private static final ScheduledExecutorService executorService = ExecutorServiceUtil
                .buildScheduledThreadPool(2, "ThreadPoolStat-");
        private static final ConcurrentHashMap<String, PoolStat> POOL_STAT_REGISTRY = new ConcurrentHashMap<>(8);

        /*
         * 把记录输出到监控日志,并清零,每秒执行一次
         */
        static {
            executorService.scheduleAtFixedRate(() -> {
                POOL_STAT_REGISTRY.values().forEach(poolStat -> {
                    if (poolStat == null) {
                        return;
                    }
                    // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录线程池的状态值
                    poolStat.updateNow();

                    MonitorPoint point = MonitorPoint
                            .monitorKey("isharpever.threadpool")
                            .addTag("app", AppNameUtil.getAppName())
                            .addTag("ip", NetUtil.getLocalHostAddress())
                            .addTag("name", poolStat.getPoolName())
                            .addField("corePoolSize", poolStat.getCorePoolSize())
                            .addField("maximumPoolSize", poolStat.getMaximumPoolSize())
                            .addField("poolSize", poolStat.getPoolSize())
                            .addField("largestPoolSize", poolStat.getLargestPoolSize())
                            .addField("activeCount", poolStat.getActiveCount())
                            .addField("completedTaskCount", poolStat.getCompletedTaskCount())
                            .addField("taskCount", poolStat.getTaskCount())
                            .addField("queueSize", poolStat.getQueueSize())
                            .build();
                    monitor.writePoint(point);
                });
            }, 1, 1, TimeUnit.SECONDS);
        }

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
            this.recordPoolStat();

            if (METHOD_NAME_SUBMIT.contains(method.getName())) {
                LogUniqueKeyUtil.generateKeyToLogIfAbsent();
                final String logKey = LogUniqueKeyUtil.getKeyFromLog();

                final Object arg0 = args[0];
                if (arg0 instanceof Runnable) {
                    args[0] = (Runnable) () -> {
                        LogUniqueKeyUtil.generateKeyToLog(logKey);
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
                            this.recordPoolStat();
                        }
                    };
                } else if (arg0 instanceof Callable<?>) {
                    args[0] = (Callable<Object>) () -> {
                        LogUniqueKeyUtil.generateKeyToLog(logKey);
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
                            this.recordPoolStat();
                        }
                    };
                }
            }
            return method.invoke(threadPoolExecutor, args);
        }

        /**
         * 获取线程池的状态值,如果比当前值更大则更新当前值<br>
         * 执行的时机为:(a)向线程池提交任务时 (b)任务执行结束时
         */
        private void recordPoolStat() {
            PoolStat poolStat = this.getPoolStat(this.threadPoolName, this.threadPoolExecutor);
            poolStat.updateNow();
        }

        private PoolStat getPoolStat(String poolName, ThreadPoolExecutor executor) {
            PoolStat poolStat = POOL_STAT_REGISTRY.get(poolName);
            if (poolStat == null) {
                POOL_STAT_REGISTRY.putIfAbsent(poolName, new PoolStat(poolName, executor));
                poolStat = POOL_STAT_REGISTRY.get(poolName);
            }
            return poolStat;
        }
    }

    private static class PoolStat {
        private ThreadPoolExecutor executor;
        private String poolName;
        /** 核心线程数 */
        private AtomicInteger corePoolSize;
        /** 最大线程数 */
        private AtomicInteger maximumPoolSize;
        /** 当前线程数 */
        private AtomicInteger poolSize;
        /** 最大线程数 */
        private AtomicInteger largestPoolSize;
        /** 活动线程数 */
        private AtomicInteger activeCount;
        /** 完成的任务数 */
        private AtomicLong completedTaskCount;
        /** 完成的任务数+正在执行的任务数 */
        private AtomicLong taskCount;
        /** 等待执行的任务数 */
        private AtomicInteger queueSize;

        private PoolStat(String poolName, ThreadPoolExecutor executor) {
            this.executor = executor;
            this.poolName = poolName;
            this.corePoolSize = new AtomicInteger(0);
            this.maximumPoolSize = new AtomicInteger(0);
            this.poolSize = new AtomicInteger(0);
            this.largestPoolSize = new AtomicInteger(0);
            this.activeCount = new AtomicInteger(0);
            this.completedTaskCount = new AtomicLong(0);
            this.taskCount = new AtomicLong(0);
            this.queueSize = new AtomicInteger(0);
        }

        private void updateNow() {
            updCorePoolSizeIfLarger(executor.getCorePoolSize());
            updMaximumPoolSizeIfLarger(executor.getMaximumPoolSize());
            updPoolSizeIfLarger(executor.getPoolSize());
            updLargestPoolSizeIfLarger(executor.getLargestPoolSize());
            updActiveCountIfLarger(executor.getActiveCount());
            updCompletedTaskCountIfLarger(executor.getCompletedTaskCount());
            updTaskCountIfLarger(executor.getTaskCount());
            updQueueSizeIfLarger(executor.getQueue().size());
        }

        private void updCorePoolSizeIfLarger(int corePoolSize) {
            int current = this.corePoolSize.get();
            while (corePoolSize > current) {
                if (this.corePoolSize.compareAndSet(current, corePoolSize)) {
                    return;
                }
                current = this.corePoolSize.get();
            }
        }

        private void updMaximumPoolSizeIfLarger(int maximumPoolSize) {
            int current = this.maximumPoolSize.get();
            while (maximumPoolSize > current) {
                if (this.maximumPoolSize.compareAndSet(current, maximumPoolSize)) {
                    return;
                }
                current = this.maximumPoolSize.get();
            }
        }

        private void updPoolSizeIfLarger(int poolSize) {
            int current = this.poolSize.get();
            while (poolSize > current) {
                if (this.poolSize.compareAndSet(current, poolSize)) {
                    return;
                }
                current = this.poolSize.get();
            }
        }

        private void updLargestPoolSizeIfLarger(int largestPoolSize) {
            int current = this.largestPoolSize.get();
            while (largestPoolSize > current) {
                if (this.largestPoolSize.compareAndSet(current, largestPoolSize)) {
                    return;
                }
                current = this.largestPoolSize.get();
            }
        }

        private void updActiveCountIfLarger(int activeCount) {
            int current = this.activeCount.get();
            while (activeCount > current) {
                if (this.activeCount.compareAndSet(current, activeCount)) {
                    return;
                }
                current = this.activeCount.get();
            }
        }

        private void updCompletedTaskCountIfLarger(long completedTaskCount) {
            long current = this.completedTaskCount.get();
            while (completedTaskCount > current) {
                if (this.completedTaskCount.compareAndSet(current, completedTaskCount)) {
                    return;
                }
                current = this.completedTaskCount.get();
            }
        }

        private void updTaskCountIfLarger(long taskCount) {
            long current = this.taskCount.get();
            while (taskCount > current) {
                if (this.taskCount.compareAndSet(current, taskCount)) {
                    return;
                }
                current = this.taskCount.get();
            }
        }

        private void updQueueSizeIfLarger(int queueSize) {
            int current = this.queueSize.get();
            while (queueSize > current) {
                if (this.queueSize.compareAndSet(current, queueSize)) {
                    return;
                }
                current = this.queueSize.get();
            }
        }

        private String getPoolName() {
            return poolName;
        }

        public int getCorePoolSize() {
            return corePoolSize.get();
        }

        public int getMaximumPoolSize() {
            return maximumPoolSize.get();
        }

        public int getPoolSize() {
            return poolSize.get();
        }

        public int getLargestPoolSize() {
            return largestPoolSize.get();
        }

        public int getActiveCount() {
            return activeCount.get();
        }

        public long getCompletedTaskCount() {
            return completedTaskCount.get();
        }

        public long getTaskCount() {
            return taskCount.get();
        }

        public int getQueueSize() {
            return queueSize.get();
        }
    }
}
