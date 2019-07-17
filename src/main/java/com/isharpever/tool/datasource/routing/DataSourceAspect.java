package com.isharpever.tool.datasource.routing;

import com.isharpever.tool.datasource.routing.DataSource.Propagation;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 切换数据源切面
 */
@Component
@Aspect
@Slf4j
public class DataSourceAspect {

    @Pointcut("@annotation(DataSource)")
    public void pointcut() {}

    @Around("pointcut()")
    public Object arround(ProceedingJoinPoint joinPoint) throws Throwable {
        Object lookupKey = retrieveLookupKey(joinPoint);
        DataSourceLookupKeyHolder.put(lookupKey);
        try {
            return joinPoint.proceed();
        }finally {
            DataSourceLookupKeyHolder.pop();
        }
    }

    private Object retrieveLookupKey(ProceedingJoinPoint joinPoint) {
        try {
            Method m = ((MethodSignature)joinPoint.getSignature()).getMethod();
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