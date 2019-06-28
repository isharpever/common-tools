package com.isharpever.tool.datasource.monitor;

import java.sql.SQLException;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;

public class MonitorDataSource extends DataSource {

    /**
     * Constructor for reflection only. A default set of pool properties will be created.
     */
    public MonitorDataSource() {
        super();
    }

    /**
     * Constructs a DataSource object wrapping a connection
     * @param poolProperties The pool properties
     */
    public MonitorDataSource(PoolConfiguration poolProperties) {
        super(poolProperties);
    }

    @Override
    public ConnectionPool createPool() throws SQLException {
        if (pool != null) {
            return pool;
        } else {
            return pCreateMonitorPool();
        }
    }

    /**
     * Sets up the connection pool, by creating a pooling driver.
     */
    private synchronized ConnectionPool pCreateMonitorPool() throws SQLException {
        if (pool != null) {
            return pool;
        } else {
            pool = new MonitorConnectionPool(poolProperties);
            return pool;
        }
    }
}
