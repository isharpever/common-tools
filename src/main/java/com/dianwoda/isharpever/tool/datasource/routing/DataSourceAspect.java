package com.dianwoda.isharpever.tool.datasource.routing;

import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 切换数据源切面
 */
public class DataSourceAspect implements MethodInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceAspect.class);

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            Method m = invocation.getMethod();
            if (m != null && m.isAnnotationPresent(DataSource.class)) {
                DataSource data = m.getAnnotation(DataSource.class);
                HandleDataSource.putDataSource(data.value());
            }else {
                // 没有注解的默认 都走默认数据源
                HandleDataSource.putDataSource(DbTypeEn.DEFAULT.getMean());
            }
            logger.debug(m.toString() + " execute with datasource is " + HandleDataSource.getDataSource());
            return invocation.proceed();
        }finally {
            HandleDataSource.clear();
            logger.info("restore database connection");
        }
    }
}