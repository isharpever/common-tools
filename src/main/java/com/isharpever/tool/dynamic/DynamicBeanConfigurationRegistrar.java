package com.isharpever.tool.dynamic;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * 向spring注册动态bean功能所需配置类
 *
 * @see EnableDynamicBean
 */
public class DynamicBeanConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {

        // 注册DynamicBeanRegistry
        registerDynamicBeanRegistry(importingClassMetadata, registry);

        // 注册DynamicController
        registerDynamicController(importingClassMetadata, registry);

        // 注册DynamicBeanAutoProxyCreator
        registerDynamicBeanAutoProxyCreator(importingClassMetadata, registry);
    }

    private void registerDynamicBeanRegistry(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(DynamicBeanRegistry.class);
        bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        registry.registerBeanDefinition(DynamicBeanRegistry.BEAN_NAME, bd);
    }

    private void registerDynamicController(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(DynamicController.class);
        bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        registry.registerBeanDefinition(DynamicController.BEAN_NAME, bd);
    }

    private void registerDynamicBeanAutoProxyCreator(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        AnnotationAttributes enableDynamicBean = AnnotationAttributes.fromMap(importingClassMetadata
                .getAnnotationAttributes(EnableDynamicBean.class.getName(), false));

        boolean onlyNewBean = enableDynamicBean.getBoolean("onlyNewBean");
        if (onlyNewBean) {
            // 只支持动态新增bean
            return;
        }

        String[] basePackages = enableDynamicBean.getStringArray("basePackages");
        if (basePackages == null || basePackages.length == 0) {
            String currentPackage = importingClassMetadata.getClassName()
                    .substring(0, importingClassMetadata.getClassName().lastIndexOf('.'));
            basePackages = new String[]{currentPackage};
        }

        Class<?>[] classes = enableDynamicBean.getClassArray("classes");

        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(DynamicBeanAutoProxyCreator.class);
        bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        bd.getConstructorArgumentValues().addIndexedArgumentValue(0, basePackages);
        bd.getConstructorArgumentValues().addIndexedArgumentValue(1, classes);
        bd.getPropertyValues().add("proxyTargetClass", true);
        registry.registerBeanDefinition(DynamicBeanAutoProxyCreator.BEAN_NAME, bd);
    }
}
