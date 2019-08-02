package com.isharpever.tool.datasource.routing;

import com.isharpever.tool.datasource.routing.DataSource.Propagation;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 切换数据源切面
 */
@Slf4j
public class ChooseDataSourceInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object lookupKey = retrieveLookupKey(invocation);
        DataSourceLookupKeyHolder.put(lookupKey);
        try {
            return invocation.proceed();
        }finally {
            DataSourceLookupKeyHolder.pop();
        }
    }

    private Object retrieveLookupKey(MethodInvocation invocation) {
        try {
            Method m = invocation.getMethod();
            if (m != null && m.isAnnotationPresent(DataSource.class)) {
                DataSource data = m.getAnnotation(DataSource.class);
                if (data.propagation() == Propagation.INHERIT
                        && DataSourceLookupKeyHolder.get() != null) {
                    return DataSourceLookupKeyHolder.get();
                }
                return data.value();
            }
        } catch (Exception e) {
            log.warn("--- 选择数据源发生异常", e);
        }
        return null;
    }
}