package com.isharpever.tool.dynamic;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.annotation.Resource;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.beans.BeansException;

/**
 * 创建代理:将原始spring bean的方法调用 代理到 动态bean
 */
public class DynamicBeanAutoProxyCreator extends AbstractAutoProxyCreator {

    public static final String BEAN_NAME = "dynamicBeanAutoProxyCreator";

    private final Set<Object> earlyProxyReferences =
            Collections.newSetFromMap(new ConcurrentHashMap<Object, Boolean>(16));
    private String[] basePackages;
    private Class<?>[] classes;

    @Resource
    private DynamicBeanRegistry dynamicBeanRegistry;

    public DynamicBeanAutoProxyCreator(String[] basePackages, Class<?>[] classes) {
        super();
        this.basePackages = basePackages;
        this.classes = classes;
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
        if (beanName.startsWith(DynamicBeanRegistry.DYNAMIC_BEAN_NAME_PREFIX)) {
            return bean;
        }
        if (bean != null) {
            // spring early reference机制:触发了getEarlyBeanReference后,跳过postProcessAfterInitialization处理
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                this.earlyProxyReferences.add(cacheKey);
            }
            return wrapIfNecessary(bean, beanName);
        }
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (beanName.startsWith(DynamicBeanRegistry.DYNAMIC_BEAN_NAME_PREFIX)) {
            return bean;
        }
        if (bean != null) {
            // spring early reference机制:触发了getEarlyBeanReference后,跳过postProcessAfterInitialization处理
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                return wrapIfNecessary(bean, beanName);
            }
        }
        return bean;
    }

    private Object wrapIfNecessary(Object bean, String beanName) {
        if (bean == null) {
            return null;
        }
        // 无法cglib代理的跳过
        if (Modifier.isFinal(bean.getClass().getModifiers())) {
            return bean;
        }
        // 指定package和class以外的跳过
        if (!isTypeMatch(bean.getClass())) {
            return bean;
        }
        // 动态bean本身跳过
        if (beanName.startsWith(DynamicBeanRegistry.DYNAMIC_BEAN_NAME_PREFIX)) {
            return bean;
        }
        return createProxy(bean.getClass(), beanName, null,
                new DynamicBeanTargetSource(bean, beanName, dynamicBeanRegistry));
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName,
            TargetSource customTargetSource) throws BeansException {
        return null;
    }

    private boolean isTypeMatch(Class<?> clazz) {
        if (ArrayUtils.isNotEmpty(this.classes)) {
            return Stream.of(this.classes).anyMatch(clazz::equals);
        }
        if (ArrayUtils.isNotEmpty(this.basePackages)) {
            return Stream.of(this.basePackages).anyMatch(clazz.getName()::startsWith);
        }
        return false;
    }
}
