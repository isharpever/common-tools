package com.isharpever.tool.dynamic;

/**
 * 动态bean信息
 *
 * @author yinxiaolin
 * @since 2019/9/10
 */
public class DynamicBeanInfo {

    /**
     * 动态bean对象
     */
    private Object bean;

    /**
     * 原始动态bean name
     */
    private String originalBeanName;

    /**
     * 实际动态bean name
     */
    private String beanName;

    /**
     * 原始动态bean class name
     */
    private String originalFullClassName;

    /**
     * 动态bean class name
     */
    private String fullClassName;

    public boolean registered() {
        return bean != null;
    }

    public Object getBean() {
        return bean;
    }

    public void setBean(Object bean) {
        this.bean = bean;
    }

    public String getOriginalBeanName() {
        return originalBeanName;
    }

    public void setOriginalBeanName(String originalBeanName) {
        this.originalBeanName = originalBeanName;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getOriginalFullClassName() {
        return originalFullClassName;
    }

    public void setOriginalFullClassName(String originalFullClassName) {
        this.originalFullClassName = originalFullClassName;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    @Override
    public String toString() {
        return "DynamicBeanInfo{" +
                "bean=" + bean +
                ", originalBeanName='" + originalBeanName + '\'' +
                ", beanName='" + beanName + '\'' +
                ", originalFullClassName='" + originalFullClassName + '\'' +
                ", fullClassName='" + fullClassName + '\'' +
                '}';
    }
}
