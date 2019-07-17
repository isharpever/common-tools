package com.isharpever.tool.datasource.routing;

import java.util.HashMap;
import java.util.Map;
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
    protected Object determineCurrentLookupKey() {
        return DataSourceLookupKeyHolder.get();
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
}
