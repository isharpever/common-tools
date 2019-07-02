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
                    // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录线程池的状态值
                    poolStat.updateNow();

                    // 从上次获取并记录后没有发生变化的话,没必要输出监控日志
                    if (!poolStat.isChanged()) {
                        return;
                    }

                    MonitorPoint point = MonitorPoint
                            .monitorKey("isharpever.threadpool")
                            .addTag("app", AppNameUtil.getAppName())
                            .addTag("ip", NetUtil.getLocalHostAddress())
                            .addTag("name", poolStat.getPoolName())
                            .addField("corePoolSize", poolStat.getAndClearCorePoolSize())
                            .addField("maximumPoolSize", poolStat.getAndClearMaximumPoolSize())
                            .addField("poolSize", poolStat.getAndClearPoolSize())
                            .addField("largestPoolSize", poolStat.getAndClearLargestPoolSize())
                            .addField("activeCount", poolStat.getAndClearActiveCount())
                            .addField("completedTaskCount", poolStat.getAndClearCompletedTaskCount())
                            .addField("taskCount", poolStat.getAndClearTaskCount())
                            .addField("queueSize", poolStat.getAndClearQueueSize())
                            .build();
                    monitor.writePoint(point);
                });
            }, 1, 1, TimeUnit.SECONDS);
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

        /**
         * 只在值变大的时候更新,因此收集当前值后需要清零(getAndClearXxx),否则值只会变大不会变小<br>
         * 没有采取"凡是在值发生变化时都更新"这样策略的原因是:值的收集不是实时的,希望收集的值能体现一段时间的高峰
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

        private String getPoolName() {
            return poolName;
        }

        public int getAndClearCorePoolSize() {
            return previousCorePoolSize = corePoolSize.getAndSet(0);
        }

        public int getAndClearMaximumPoolSize() {
            return previousMaximumPoolSize = maximumPoolSize.getAndSet(0);
        }

        public int getAndClearPoolSize() {
            return previousPoolSize = poolSize.getAndSet(0);
        }

        public int getAndClearLargestPoolSize() {
            return previousLargestPoolSize = largestPoolSize.getAndSet(0);
        }

        public int getAndClearActiveCount() {
            return previousActiveCount = activeCount.getAndSet(0);
        }

        public long getAndClearCompletedTaskCount() {
            return previousCompletedTaskCount = completedTaskCount.getAndSet(0);
        }

        public long getAndClearTaskCount() {
            return previousTaskCount = taskCount.getAndSet(0);
        }

        public int getAndClearQueueSize() {
            return previousQueueSize = queueSize.getAndSet(0);
        }

        /**
         * 具有实时监控意义的指标是否发生了变化
         * @return
         */
        private boolean isChanged() {
            return this.corePoolSize.get() != previousCorePoolSize
                    || this.maximumPoolSize.get() != previousMaximumPoolSize
                    || this.poolSize.get() != previousPoolSize
                    || this.largestPoolSize.get() != previousLargestPoolSize
                    || this.activeCount.get() != previousActiveCount
                    // 这两项实时监控的意义不大
//                    || this.completedTaskCount.get() != previousCompletedTaskCount
//                    || this.taskCount.get() != previousTaskCount
                    || this.queueSize.get() != previousQueueSize;
        }
    }
}
