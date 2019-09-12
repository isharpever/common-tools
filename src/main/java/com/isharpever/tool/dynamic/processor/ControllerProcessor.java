package com.isharpever.tool.dynamic.processor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.handler.AbstractHandlerMethodMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class ControllerProcessor implements DynamicBeanProcessor {

    /** 缓存动态bean的requestMappingInfo */
    private final Map<String, List<RequestMappingInfo>> beanRequestMappingInfoCache = new ConcurrentHashMap<>();

    private Method isHandler;
    private Method getMappingForMethod;
    private Method registerHandlerMethod;
    private Method unregisterMapping;
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /**
     * spring BeanFactory
     */
    private DefaultListableBeanFactory springBeanFactory;

    public ControllerProcessor(DefaultListableBeanFactory beanFactory) {
        this.springBeanFactory = beanFactory;
    }

    @Override
    public Object processRegister(String beanName, Object bean, Class<?> beanClass) {
        // 如果创建的bean是controller,新request mapping
        if (isController(beanClass)) {
            detectRequestMappingHandlerMethods(beanName, beanClass);
        }
        return bean;
    }

    @Override
    public Object processUnregister(String beanName, Object bean, Class<?> beanClass) {
        // 如果创建的bean是controller,移除request mapping
        if (isController(beanClass)) {
            removeBeanRequestMappings(beanName);
        }
        return bean;
    }

    /**
     * 返回指定class是否是controller
     *
     * @param clazz
     * @return
     */
    private boolean isController(Class<?> clazz) {
        if (this.isHandler == null) {
            try {
                this.isHandler = RequestMappingHandlerMapping.class
                        .getDeclaredMethod("isHandler", Class.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
            }
        }
        try {
            this.isHandler.setAccessible(true);
            return (boolean)this.isHandler.invoke(getRequestMappingHandlerMapping(), clazz);
        } catch (Exception e) {
            throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
        } finally {
            this.isHandler.setAccessible(false);
        }
    }

    /**
     * 更新指定controller的request mapping
     *
     * @param controllerBeanName
     */
    private void detectRequestMappingHandlerMethods(String controllerBeanName, Class<?> clazz) {
        removeBeanRequestMappings(controllerBeanName);

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (!method.isAnnotationPresent(RequestMapping.class)) {
                continue;
            }

            RequestMappingInfo requestMappingInfo = getRequestMappingForMethod(method, clazz);
            if (requestMappingInfo == null) {
                continue;
            }

            Method invocableMethod = AopUtils.selectInvocableMethod(method, clazz);
            registerRequestMappingHandlerMethod(controllerBeanName, invocableMethod, requestMappingInfo);
        }
    }

    private void removeBeanRequestMappings(String beanName) {
        List<RequestMappingInfo> requestMappingInfoList = this.beanRequestMappingInfoCache.get(beanName);
        if (requestMappingInfoList == null) {
            return;
        }
        for (RequestMappingInfo requestMappingInfo : requestMappingInfoList) {
            unregisterRequestMapping(requestMappingInfo);
        }
        requestMappingInfoList.clear();
    }

    private RequestMappingInfo getRequestMappingForMethod(Method method, Class<?> clazz) {
        if (this.getMappingForMethod == null) {
            try {
                this.getMappingForMethod = RequestMappingHandlerMapping.class
                        .getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
            }
        }
        try {
            this.getMappingForMethod.setAccessible(true);
            return (RequestMappingInfo) this.getMappingForMethod
                    .invoke(getRequestMappingHandlerMapping(), method, clazz);
        } catch (Exception e) {
            return null;
        } finally {
            this.getMappingForMethod.setAccessible(false);
        }
    }

    private void unregisterRequestMapping(RequestMappingInfo requestMappingInfo) {
        if (this.unregisterMapping == null) {
            try {
                this.unregisterMapping = AbstractHandlerMethodMapping.class
                        .getDeclaredMethod("unregisterMapping", Object.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
            }
        }
        try {
            this.unregisterMapping.setAccessible(true);
            this.unregisterMapping
                    .invoke(getRequestMappingHandlerMapping(), requestMappingInfo);
        } catch (Exception e) {
            throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
        } finally {
            this.unregisterMapping.setAccessible(false);
        }
    }

    private void registerRequestMappingHandlerMethod(String controllerBeanName,
            Method invocableMethod, RequestMappingInfo requestMappingInfo) {
        if (this.registerHandlerMethod == null) {
            try {
                this.registerHandlerMethod = AbstractHandlerMethodMapping.class
                        .getDeclaredMethod("registerHandlerMethod", Object.class, Method.class, Object.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
            }
        }
        try {
            this.registerHandlerMethod.setAccessible(true);
            this.registerHandlerMethod
                    .invoke(getRequestMappingHandlerMapping(), controllerBeanName, invocableMethod, requestMappingInfo);
            storeBeanRequestMappingInfo(controllerBeanName, requestMappingInfo);
        } catch (Exception e) {
            throw new IllegalStateException(ExceptionUtils.getStackTrace(e));
        } finally {
            this.registerHandlerMethod.setAccessible(false);
        }
    }

    private RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        if (this.requestMappingHandlerMapping == null) {
            this.requestMappingHandlerMapping = this.springBeanFactory.getBean(RequestMappingHandlerMapping.class);
        }
        if (this.requestMappingHandlerMapping == null) {
            throw new IllegalStateException("未找到requestMappingHandlerMapping");
        }
        return this.requestMappingHandlerMapping;
    }

    private void storeBeanRequestMappingInfo(String beanName, RequestMappingInfo requestMappingInfo) {
        List<RequestMappingInfo> requestMappingInfoList = this.beanRequestMappingInfoCache.get(beanName);
        if (requestMappingInfoList == null) {
            this.beanRequestMappingInfoCache.putIfAbsent(beanName, new ArrayList<>());
        }
        this.beanRequestMappingInfoCache.get(beanName).add(requestMappingInfo);
    }
}