package com.isharpever.tool.datasource.routing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;


/**
 * 自定义路由数据源
 */
public class ChooseDataSource extends AbstractRoutingDataSource {
    private Map<Object, Object> targetDataSources = new ConcurrentHashMap<>();

    public ChooseDataSource() {
        this.setTargetDataSources(targetDataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return HandleDataSource.getDataSource();
    }

    /**
     * 添加数据源
     * @param key
     * @param dataSource
     * @param isDefault
     */
    public void addTargetDataSource(String key, DataSource dataSource, boolean isDefault) {
        this.targetDataSources.put(key, dataSource);
        if (isDefault) {
            this.targetDataSources.put(DbTypeEn.DEFAULT.getMean(), dataSource);
            this.setDefaultTargetDataSource(dataSource);
        }
        super.afterPropertiesSet();
    }
}
