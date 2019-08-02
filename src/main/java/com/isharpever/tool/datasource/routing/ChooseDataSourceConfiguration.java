package com.isharpever.tool.datasource.routing;

import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

@Configuration
public class ChooseDataSourceConfiguration implements ImportAware {
    public static final int DEFAULT_ORDER = 1;
    private AnnotationAttributes enableChooseDataSource;

    @Bean
    public DefaultBeanFactoryPointcutAdvisor chooseDataSourceAdvisor() {
        DefaultBeanFactoryPointcutAdvisor advisor = new DefaultBeanFactoryPointcutAdvisor();
        advisor.setPointcut(AnnotationMatchingPointcut.forMethodAnnotation(DataSource.class));
        advisor.setAdvice(chooseDataSourceInterceptor());
        advisor.setOrder(order());
        return advisor;
    }

    @Bean
    public ChooseDataSourceInterceptor chooseDataSourceInterceptor() {
        return new ChooseDataSourceInterceptor();
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
        enableChooseDataSource = AnnotationAttributes.fromMap(importMetadata
                .getAnnotationAttributes(EnableChooseDataSource.class.getName(), false));
    }

    private int order() {
        if (enableChooseDataSource != null) {
            try {
                return enableChooseDataSource.<Integer>getNumber("order");
            } catch (Exception e) {
            }
        }
        return DEFAULT_ORDER;
    }
}
