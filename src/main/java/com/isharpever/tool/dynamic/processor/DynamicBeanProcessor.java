package com.isharpever.tool.dynamic.processor;

public interface DynamicBeanProcessor {

    Object processRegister(String beanName, Object bean, Class<?> beanClass);

    Object processUnregister(String beanName, Object bean, Class<?> beanClass);
}
