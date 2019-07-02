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
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.PooledConnection;

public class MonitorConnectionPool extends ConnectionPool {

    private static final MonitorUtil monitor = MonitorFactory.connect();
    private static final ScheduledExecutorService executorService = ExecutorServiceUtil
            .buildScheduledThreadPool(2, "ConnectionPoolStat-");
    private static final ConcurrentHashMap<String, PoolStat> POOL_STAT_REGISTRY = new ConcurrentHashMap<>(2);

    /*
     * 把记录输出到监控日志,并清零,每秒执行一次
     */
    static {
        executorService.scheduleAtFixedRate(() -> {
            POOL_STAT_REGISTRY.values().forEach(poolStat -> {
                if (poolStat == null) {
                    return;
                }
                // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录连接池的活跃和空闲数
                poolStat.updateNow();

                MonitorPoint point = MonitorPoint
                        .monitorKey("isharpever.datasource.pool")
                        .addTag("app", AppNameUtil.getAppName())
                        .addTag("name", poolStat.getPoolName())
                        .addTag("ip", NetUtil.getLocalHostAddress())
                        .addField("active", poolStat.getAndClearActive())
                        .addField("idle", poolStat.getAndClearIdle()).build();
                monitor.writePoint(point);
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
     * 获取连接池的活跃和空闲数,如果比当前值更大则更新当前值<br>
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
        private MonitorConnectionPool connectionPool;
        private String poolName;
        private AtomicInteger active;
        private AtomicInteger idle;

        private PoolStat(String poolName, MonitorConnectionPool connectionPool) {
            this.connectionPool = connectionPool;
            this.poolName = poolName;
            this.active = new AtomicInteger(0);
            this.idle = new AtomicInteger(0);
        }

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

        private String getPoolName() {
            return poolName;
        }

        private int getAndClearActive() {
            return this.active.getAndSet(0);
        }

        private int getAndClearIdle() {
            return this.idle.getAndSet(0);
        }
    }
}
