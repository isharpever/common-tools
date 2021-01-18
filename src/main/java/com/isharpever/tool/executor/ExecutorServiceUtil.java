package com.isharpever.tool.executor;

import com.isharpever.tool.mdc.LogUniqueKeyUtil;
import com.isharpever.tool.utils.AppNameUtil;
import com.isharpever.tool.utils.NetUtil;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
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
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ExecutorServiceUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorServiceUtil.class);

    /** 线程池监控数据默认的measurement */
    private static final String DEFAULT_MEASUREMENT = "isharpever.threadpool";

    public static ExecutorService buildExecutorService(int maximumPoolSize, String poolName) {
        return buildExecutorService(maximumPoolSize, poolName, DEFAULT_MEASUREMENT);
    }

    public static ExecutorService buildExecutorService(int maximumPoolSize, String poolName, String measurement) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, 0L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                new DefaultNamedThreadFactory(poolName));
        return buildExecutorService(executor, poolName, measurement);
    }

    public static ExecutorService buildExecutorService(ThreadPoolExecutor executor, String poolName) {
        return buildExecutorService(executor, poolName, DEFAULT_MEASUREMENT);
    }

    public static ExecutorService buildExecutorService(ThreadPoolExecutor executor, String poolName, String measurement) {
        return getProxy(executor, poolName, measurement);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(int maximumPoolSize, String poolName) {
        return buildScheduledThreadPool(maximumPoolSize, poolName, DEFAULT_MEASUREMENT);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(int maximumPoolSize, String poolName, String measurement) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(maximumPoolSize,
                new DefaultNamedThreadFactory(poolName));
        return buildScheduledThreadPool(executor, poolName, measurement);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(ScheduledThreadPoolExecutor executor, String poolName) {
        return buildScheduledThreadPool(executor, poolName, DEFAULT_MEASUREMENT);
    }

    public static ScheduledExecutorService buildScheduledThreadPool(ScheduledThreadPoolExecutor executor, String poolName, String measurement) {
        return getProxy(executor, poolName, measurement);
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
     * @param measurement
     * @return
     */
    private static <T> T getProxy(final ThreadPoolExecutor executor, String name, String measurement) {
        CustomeInvocationHandler customeInvocationHandler = new CustomeInvocationHandler(executor, name, measurement);

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
        private static final ScheduledExecutorService executorService = ExecutorServiceUtil
                .buildScheduledThreadPool(1, "ThreadPoolStat-");
        private static final ConcurrentHashMap<String, PoolStat> POOL_STAT_REGISTRY = new ConcurrentHashMap<>(8);

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

            /*
             * 把各线程池状态输出到监控日志,并清零,每秒执行一次
             */
            executorService.scheduleAtFixedRate(() -> {
                POOL_STAT_REGISTRY.values().forEach(poolStat -> {
                    if (poolStat == null) {
                        return;
                    }
                    poolStat.writePoint();
                });
            }, 1, 1, TimeUnit.SECONDS);
        }

        private ThreadPoolExecutor threadPoolExecutor;
        private String threadPoolName;
        private String measurement;

        private CustomeInvocationHandler(ThreadPoolExecutor threadPoolExecutor,
                String threadPoolName, String measurement) {
            this.threadPoolExecutor = threadPoolExecutor;
            this.threadPoolName = threadPoolName;
            this.measurement = measurement;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 线程池监控
            this.recordPoolStat();

            if (METHOD_NAME_SUBMIT.contains(method.getName())) {
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
            PoolStat poolStat = this.getPoolStat(this.threadPoolName, this.threadPoolExecutor, this.measurement);
            poolStat.updateNow();
        }

        private PoolStat getPoolStat(String poolName, ThreadPoolExecutor executor, String measurement) {
            PoolStat poolStat = POOL_STAT_REGISTRY.get(poolName);
            if (poolStat == null) {
                POOL_STAT_REGISTRY.putIfAbsent(poolName, new PoolStat(poolName, executor, measurement));
                poolStat = POOL_STAT_REGISTRY.get(poolName);
            }
            return poolStat;
        }
    }

    private static class PoolStat {
//        private static final MonitorUtil monitor = MonitorFactory.connect();

        private ThreadPoolExecutor executor;
        private String poolName;
        private String measurement;
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

        /** 前一次收集的值:核心线程数 */
        private int previousCorePoolSize;
        /** 前一次收集的值:最大线程数 */
        private int previousMaximumPoolSize;
        /** 前一次收集的值:当前线程数 */
        private int previousPoolSize;
        /** 前一次收集的值:最大线程数 */
        private int previousLargestPoolSize;
        /** 前一次收集的值:活动线程数 */
        private int previousActiveCount;
        /** 前一次收集的值:完成的任务数 */
        private long previousCompletedTaskCount;
        /** 前一次收集的值:完成的任务数+正在执行的任务数 */
        private long previousTaskCount;
        /** 前一次收集的值:等待执行的任务数 */
        private int previousQueueSize;

        private PoolStat(String poolName, ThreadPoolExecutor executor, String measurement) {
            this.executor = executor;
            this.poolName = poolName;
            this.measurement = measurement;
            this.corePoolSize = new AtomicInteger(0);
            this.maximumPoolSize = new AtomicInteger(0);
            this.poolSize = new AtomicInteger(0);
            this.largestPoolSize = new AtomicInteger(0);
            this.activeCount = new AtomicInteger(0);
            this.completedTaskCount = new AtomicLong(0);
            this.taskCount = new AtomicLong(0);
            this.queueSize = new AtomicInteger(0);
        }

        /**
         * 只在值变大的时候更新,因此收集当前值后需要清零(getAndClearXxx),否则值只会变大不会变小<br>
         * 没有采取"凡是在值发生变化时都更新"这样策略的原因是:值是每隔一段时间收集一次的,希望收集的值能体现出这段时间内的高峰
         */
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

        public int getAndClearCorePoolSize() {
            return corePoolSize.getAndSet(0);
        }

        public int getAndClearMaximumPoolSize() {
            return maximumPoolSize.getAndSet(0);
        }

        public int getAndClearPoolSize() {
            return poolSize.getAndSet(0);
        }

        public int getAndClearLargestPoolSize() {
            return largestPoolSize.getAndSet(0);
        }

        public int getAndClearActiveCount() {
            return activeCount.getAndSet(0);
        }

        public long getAndClearCompletedTaskCount() {
            return completedTaskCount.getAndSet(0);
        }

        public long getAndClearTaskCount() {
            return taskCount.getAndSet(0);
        }

        public int getAndClearQueueSize() {
            return queueSize.getAndSet(0);
        }

        /**
         * 输出监控日志
         */
        private void writePoint() {
            // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录线程池的状态值
            updateNow();

            // 获取并清零当前状态值
            int corePoolSize = getAndClearCorePoolSize();
            int maximumPoolSize = getAndClearMaximumPoolSize();
            int poolSize = getAndClearPoolSize();
            int largestPoolSize = getAndClearLargestPoolSize();
            int activeCount = getAndClearActiveCount();
            long completedTaskCount = getAndClearCompletedTaskCount();
            long taskCount = getAndClearTaskCount();
            int queueSize = getAndClearQueueSize();

            // 从上次获取并记录后没有发生变化的话,没必要输出监控日志
            if (corePoolSize == previousCorePoolSize
                    && maximumPoolSize == previousMaximumPoolSize
                    && poolSize == previousPoolSize
                    && largestPoolSize == previousLargestPoolSize
                    && activeCount == previousActiveCount
                    // 这两项即使变化,也没啥必要实时输出到监控
//                    && completedTaskCount == previousCompletedTaskCount
//                    && taskCount == previousTaskCount
                    && queueSize == previousQueueSize) {
                return;
            }

//            MonitorPoint point = MonitorPoint
//                    .monitorKey(this.measurement)
//                    .addTag("app", AppNameUtil.getAppName())
//                    .addTag("ip", NetUtil.getLocalHostAddress())
//                    .addTag("name", this.poolName)
//                    .addField("corePoolSize", corePoolSize)
//                    .addField("maximumPoolSize", maximumPoolSize)
//                    .addField("poolSize", poolSize)
//                    .addField("largestPoolSize", largestPoolSize)
//                    .addField("activeCount", activeCount)
//                    .addField("completedTaskCount", completedTaskCount)
//                    .addField("taskCount", taskCount)
//                    .addField("queueSize", queueSize)
//                    .build();
//            monitor.writePoint(point);

            this.previousCorePoolSize = corePoolSize;
            this.previousMaximumPoolSize = maximumPoolSize;
            this.previousPoolSize = poolSize;
            this.previousLargestPoolSize = largestPoolSize;
            this.previousActiveCount = activeCount;
            this.previousCompletedTaskCount = completedTaskCount;
            this.previousTaskCount = taskCount;
            this.previousQueueSize = queueSize;
        }
    }
}
