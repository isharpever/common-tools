package com.isharpever.tool.datasource.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.jdbc.datasource.lookup.MapDataSourceLookup;
import org.springframework.util.Assert;


/**
 * 自定义路由数据源
 */
public class ChooseDataSource extends AbstractRoutingDataSource {
    private final MapDataSourceLookup dataSourceLookup = new MapDataSourceLookup();
    private Map<Object, Object> dataSourceBeanNames;

    private ChooseDataSource() {
        super.setDataSourceLookup(dataSourceLookup);
    }

    /**
     * key: DataSource bean name<br>
     * value: DataSource bean
     * @param dataSources DataSource bean name与DataSource bean的映射关系
     */
    @Autowired
    public void setDataSources(Map<String, DataSource> dataSources) {
        dataSourceLookup.setDataSources(dataSources);
    }

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        this.dataSourceBeanNames = targetDataSources;
        super.setTargetDataSources(targetDataSources);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceLookupKeyHolder.get();
    }

    /**
     * 重置DataSource参数
     * @param lookupKey
     * @param url
     * @param userName
     * @param password
     * @return
     */
    public void update(String lookupKey, String url, String userName, String password) {
        Object dataSourceBeanName = dataSourceBeanNames.get(lookupKey);
        if (dataSourceBeanName == null) {
            throw new IllegalArgumentException("未找到对应库 lookupKey=" + lookupKey);
        }
        DataSource dataSource = dataSourceLookup.getDataSource(dataSourceBeanName.toString());
        if (!(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource)) {
            throw new IllegalArgumentException("只支持org.apache.tomcat.jdbc.pool.DataSource lookupKey=" + lookupKey);
        }
        ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setUrl(url);
        ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setUsername(userName);
        ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).setPassword(password);
        ((org.apache.tomcat.jdbc.pool.DataSource) dataSource).close();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private Map<Object, Object> dataSourceBeanNames = new HashMap<>(4);
        private Object defaultDataSourceBeanName;

        /**
         * 添加数据源
         *
         * @param lookupKey 与{@code @DataSource}的{@code value()}参数对应, 可使用常量{@link LookupKeyConstant}
         * @param dataSourceBeanName DataSource bean name
         * @return
         */
        public Builder addDataSource(String lookupKey, String dataSourceBeanName) {
            Assert.notNull(lookupKey, "lookupKey must not be null");
            Assert.notNull(dataSourceBeanName, "DataSource bean name must not be null");
            this.dataSourceBeanNames.put(lookupKey, dataSourceBeanName);
            return this;
        }

        /**
         * 设置默认数据源
         * @param defaultDataSourceBeanName 默认DataSource bean name
         * @return
         */
        public Builder defaultDataSourceBeanName(String defaultDataSourceBeanName) {
            this.defaultDataSourceBeanName = defaultDataSourceBeanName;
            return this;
        }

        public ChooseDataSource build() {
            Assert.notEmpty(dataSourceBeanNames, "未设置数据源");
            Assert.notNull(defaultDataSourceBeanName, "未设置默认数据源");
            ChooseDataSource chooseDataSource = new ChooseDataSource();
            chooseDataSource.setTargetDataSources(this.dataSourceBeanNames);
            chooseDataSource.setDefaultTargetDataSource(this.defaultDataSourceBeanName);
            return chooseDataSource;
        }
    }

    /**
     * 在指定库执行一个查询(增删改查)
     * @param dataSourceLookupKey 选择指定库
     * @param query 表示一个查询(增删改查)
     * @return 本次查询的结果(可能是void)
     */
    public static <T> T execute(String dataSourceLookupKey, Supplier<T> query) {
        DataSourceLookupKeyHolder.put(dataSourceLookupKey);
        try {
            return query.get();
        } finally {
            DataSourceLookupKeyHolder.pop();
        }
    }
}
