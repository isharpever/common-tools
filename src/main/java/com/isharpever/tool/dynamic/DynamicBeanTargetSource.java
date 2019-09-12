package com.isharpever.tool.dynamic;

import org.springframework.aop.TargetSource;

/**
 * {@code DynamicBeanAutoProxyCreator}所创建代理的TargetSource
 * <br>根据bean name是否已注册相应的动态bean,决定返回原始bean,还是动态bean
 *
 * @see DynamicBeanAutoProxyCreator
 */
public class DynamicBeanTargetSource implements TargetSource {

    private Object originalBean;
    private String targetBeanName;
    private DynamicBeanRegistry dynamicBeanRegistry;

    public DynamicBeanTargetSource(Object originalBean, String targetBeanName, DynamicBeanRegistry dynamicBeanRegistry) {
        this.originalBean = originalBean;
        this.targetBeanName = targetBeanName;
        this.dynamicBeanRegistry = dynamicBeanRegistry;
    }

    /**
     * 根据bean name是否已注册相应的动态bean,决定返回原始bean,还是动态bean
     * <li>若尚未注册相应的动态bean,则返回原始bean
     * <li>若已注册相应的动态bean,则返回动态bean
     *
     * @return
     * @throws Exception
     */
    @Override
    public Object getTarget() throws Exception {
        if (this.dynamicBeanRegistry == null) {
            return this.originalBean;
        }
        String beanName = this.targetBeanName;
        if (beanName.startsWith(DynamicBeanRegistry.DYNAMIC_BEAN_NAME_PREFIX)) {
            beanName = beanName.substring(DynamicBeanRegistry.DYNAMIC_BEAN_NAME_PREFIX.length());
        }
        DynamicBeanInfo dynamicBeanInfo = this.dynamicBeanRegistry.getDyanmicBean(beanName);
        if (dynamicBeanInfo == null || dynamicBeanInfo.getBean() == null) {
            return this.originalBean;
        }
        return dynamicBeanInfo.getBean();
    }

    @Override
    public Class<?> getTargetClass() {
        return originalBean.getClass();
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public void releaseTarget(Object target) throws Exception {

    }
}
