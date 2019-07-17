package com.isharpever.tool.datasource.monitor;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.isharpever.tool.executor.ExecutorServiceUtil;
import com.isharpever.tool.utils.AppNameUtil;
import com.isharpever.tool.utils.NetUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.PooledConnection;

@Slf4j
public class MonitorConnectionPool extends ConnectionPool {
    private static final ScheduledExecutorService executorService = ExecutorServiceUtil
            .buildScheduledThreadPool(1, "ConnectionPoolStat-");
    private static final ConcurrentHashMap<String, PoolStat> POOL_STAT_REGISTRY = new ConcurrentHashMap<>(2);

    /*
     * 把各连接池状态输出到监控日志,并清零,每秒执行一次
     */
    static {
        executorService.scheduleAtFixedRate(() -> {
            POOL_STAT_REGISTRY.values().forEach(poolStat -> {
                if (poolStat == null) {
                    return;
                }
                poolStat.writePoint();
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Instantiate a connection pool. This will create connections if initialSize is larger than 0.
     * The {@link PoolProperties} should not be reused for another connection pool.
     * @param prop PoolProperties - all the properties for this connection pool
     * @throws SQLException Pool initialization error
     */
    public MonitorConnectionPool(PoolConfiguration prop) throws SQLException {
        super(prop);
    }

    @Override
    protected Connection setupConnection(PooledConnection con) throws SQLException {
        this.recordPoolStat();
        return super.setupConnection(con);
    }

    @Override
    protected void returnConnection(PooledConnection con) {
        this.recordPoolStat();
        super.returnConnection(con);
    }

    @Override
    protected void release(PooledConnection con) {
        this.recordPoolStat();
        super.release(con);
    }

    /**
     * 获取连接池的状态值,如果比当前值更大则更新当前值<br>
     * 执行的时机为:(a)从连接池获取连接时 (b)向连接池返还连接时 (c)释放连接时
     */
    private void recordPoolStat() {
        PoolStat poolStat = this.getPoolStat(this.getName(), this);
        poolStat.updateNow();
    }

    private PoolStat getPoolStat(String poolName, MonitorConnectionPool connectionPool) {
        PoolStat poolStat = POOL_STAT_REGISTRY.get(poolName);
        if (poolStat == null) {
            POOL_STAT_REGISTRY.putIfAbsent(poolName, new PoolStat(poolName, connectionPool));
            poolStat = POOL_STAT_REGISTRY.get(poolName);
        }
        return poolStat;
    }

    private static class PoolStat {
        private static final MonitorUtil monitor = MonitorFactory.connect();

        private MonitorConnectionPool connectionPool;
        private String poolName;
        /** 活跃连接数 */
        private AtomicInteger active;
        /** 空闲连接数 */
        private AtomicInteger idle;

        /** 前一次收集的值:活跃连接数 */
        private int previousActive;
        /** 前一次收集的值:空闲连接数 */
        private int previousIdle;

        private PoolStat(String poolName, MonitorConnectionPool connectionPool) {
            this.connectionPool = connectionPool;
            this.poolName = poolName;
            this.active = new AtomicInteger(0);
            this.idle = new AtomicInteger(0);
        }

        /**
         * 只在值变大的时候更新,因此收集当前值后需要清零(getAndClearXxx),否则值只会变大不会变小<br>
         * 没有采取"凡是在值发生变化时都更新"这样策略的原因是:值是每隔一段时间收集一次的,希望收集的值能体现出这段时间内的高峰
         */
        private void updateNow() {
            updActiveIfLarger(connectionPool.getActive());
            updIdleIfLarger(connectionPool.getIdle());
        }

        private void updActiveIfLarger(int active) {
            int currentActive = this.active.get();
            while (active > currentActive) {
                if (this.active.compareAndSet(currentActive, active)) {
                    return;
                }
                currentActive = this.active.get();
            }
        }

        private void updIdleIfLarger(int idle) {
            int currentActive = this.idle.get();
            while (idle > currentActive) {
                if (this.idle.compareAndSet(currentActive, idle)) {
                    return;
                }
                currentActive = this.idle.get();
            }
        }

        private int getAndClearActive() {
            return this.active.getAndSet(0);
        }

        private int getAndClearIdle() {
            return this.idle.getAndSet(0);
        }

        /**
         * 输出监控日志
         */
        private void writePoint() {
            // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录连接池的状态值
            updateNow();

            // 获取并清零当前状态值
            int currentActive = getAndClearActive();
            int currentIdle = getAndClearIdle();

            // 从上次获取并记录后没有发生变化的话,没必要输出监控日志
            if (currentActive == previousActive
                    && currentIdle == previousIdle) {
                return;
            }

            MonitorPoint point = MonitorPoint
                    .monitorKey("isharpever.datasource.pool")
                    .addTag("app", AppNameUtil.getAppName())
                    .addTag("name", this.poolName)
                    .addTag("ip", NetUtil.getLocalHostAddress())
                    .addField("active", currentActive)
                    .addField("idle", currentIdle).build();
            monitor.writePoint(point);

            this.previousActive = currentActive;
            this.previousIdle = currentIdle;
        }
    }
}
