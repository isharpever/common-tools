package com.dianwoda.isharpever.tool.datasource.monitor;

import com.dianwoba.monitor.client.MonitorFactory;
import com.dianwoba.monitor.client.MonitorPoint;
import com.dianwoba.monitor.client.MonitorUtil;
import com.dianwoda.isharpever.tool.executor.ExecutorServiceUtil;
import com.dianwoda.isharpever.tool.utils.AppNameUtil;
import com.dianwoda.isharpever.tool.utils.NetUtil;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorConnectionPool extends ConnectionPool {

    public static final ConcurrentHashMap<String, PoolStat> poolStats = new ConcurrentHashMap<>(2);

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
     * 执行的时机为:(a)从连接池获取连接时 (b)向连接池返还连接时 (c)释放连接时 (d)把记录输出到监控日志之前
     */
    private void recordPoolStat() {
        PoolStat poolStat = this.getPoolStat(this.getName());
        poolStat.updActiveIfLarger(this.getActive());
        poolStat.updIdleIfLarger(this.getIdle());
    }

    private PoolStat getPoolStat(String name) {
        PoolStat poolStat = poolStats.get(name);
        if (poolStat == null) {
            synchronized (this) {
                poolStat = poolStats.get(name);
                if (poolStat == null) {
                    poolStats.putIfAbsent(name, new PoolStat(name, this));
                    poolStat = poolStats.get(name);
                }
            }
        }
        return poolStat;
    }

    private static class PoolStat {
        private static final Logger logger = LoggerFactory.getLogger(PoolStat.class);
        private static final MonitorUtil monitor = MonitorFactory.connect();
        private static final ScheduledExecutorService executorService = ExecutorServiceUtil
                .buildScheduledThreadPool(2, "PoolStat-");

        private MonitorConnectionPool monitorConnectionPool;
        private String poolName;
        private AtomicInteger active;
        private AtomicInteger idle;

        private PoolStat(String poolName, MonitorConnectionPool monitorConnectionPool) {
            this.monitorConnectionPool = monitorConnectionPool;
            this.poolName = poolName;
            this.active = new AtomicInteger(0);
            this.idle = new AtomicInteger(0);

            /*
             * 把记录输出到监控日志,并清零,每秒执行一次
             */
            executorService.scheduleAtFixedRate(() -> {
                // 为保证监控结果的相对实时性,在输出监控日志之前再次获取并记录连接池的活跃和空闲数
                monitorConnectionPool.recordPoolStat();

                MonitorPoint point = MonitorPoint
                        .monitorKey("isharpever.datasource.pool")
                        .addTag("app", AppNameUtil.getAppName())
                        .addTag("name", this.poolName)
                        .addTag("ip", NetUtil.getLocalHostAddress())
                        .addField("active", this.getAndClearActive())
                        .addField("idle", this.getAndClearIdle()).build();
                monitor.writePoint(point);
            }, 1, 1, TimeUnit.SECONDS);
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
    }
}
