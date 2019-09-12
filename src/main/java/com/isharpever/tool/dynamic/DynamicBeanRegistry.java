package com.isharpever.tool.dynamic;

import com.isharpever.tool.dynamic.compile.CompileUtil;
import com.isharpever.tool.dynamic.processor.ControllerProcessor;
import com.isharpever.tool.dynamic.processor.DynamicBeanProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;

/**
 * 动态bean工厂
 *
 * @author yinxiaolin
 * @since 2019/8/14
 */
public class DynamicBeanRegistry implements BeanFactoryAware, InitializingBean {

    public static final Logger logger = LoggerFactory.getLogger(DynamicBeanRegistry.class);

    public static final String BEAN_NAME = "dynamicBeanRegistry";

    /** 缓存 动态bean信息 */
    private final Map<String, DynamicBeanInfo> dynamicBeanCache = new HashMap<>();

    /**
     * 记录class name对应的动态bean name
     * <li>key:original full class name
     * <li>value:bean name
     */
    private static final Map<String, String> CLASS_REGISTERED_BEAN_MAP = new HashMap<>();

    /** 动态bean name prefix */
    public static final String DYNAMIC_BEAN_NAME_PREFIX = "db@";

    /** spring BeanFactory */
    private DefaultListableBeanFactory springBeanFactory;

    private List<DynamicBeanProcessor> dynamicBeanProcessors = new ArrayList<>();

    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    public DynamicBeanRegistry() {
        super();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.springBeanFactory = (DefaultListableBeanFactory)beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        dynamicBeanProcessors.add(new ControllerProcessor(this.springBeanFactory));
    }

    /**
     * 注册一个动态bean
     *
     * @param code 源码
     * @return 注册后得到的动态bean信息
     */
    public DynamicBeanInfo registerBean(String code) {
        // 初始化编译工具类
        CompileUtil.init(this.springBeanFactory.getBeanClassLoader());

        code = code.replaceAll("\r\n", "\n");
        code = code.replace("\\r\\n", "\n");

        // 检查类是否合法
        String originalFullClassName = CompileUtil.getQualifiedName(code);
        validateClass(originalFullClassName);

        String finalFullClassName = CompileUtil.addSource(code);

        return registerBean(originalFullClassName, finalFullClassName);
    }

    /**
     * 注册一个动态bean
     *
     * @param originalFullClassName 原始类全限定名
     * @param fullClassName 类全限定名
     * @return 注册后得到的动态bean信息
     */
    public DynamicBeanInfo registerBean(String originalFullClassName, String fullClassName) {
        // 初始化编译工具类
        CompileUtil.init(this.springBeanFactory.getBeanClassLoader());

        DynamicBeanInfo result = new DynamicBeanInfo();
        result.setOriginalFullClassName(originalFullClassName);
        result.setFullClassName(fullClassName);

        // 用spring beanClassLoader加载指定类
        Class<?> clazz = CompileUtil
                .load(this.springBeanFactory.getBeanClassLoader(), fullClassName);

        String name = getBeanName(clazz);
        if (StringUtils.isBlank(name)) {
            // 之前注册过、且本次不需要注册动态bean,卸载之前注册的动态bean
            if (CLASS_REGISTERED_BEAN_MAP.containsKey(originalFullClassName)) {
                unregisterBean(CLASS_REGISTERED_BEAN_MAP.get(originalFullClassName));
            }
            return result;
        }

        Object bean = null;
        String beanName = null;
        synchronized (name.intern()) {
            // 改bean name
            beanName = transformedBeanName(name);

            // 严格杜绝的情况：name是已存在的spring bean, 但新类型不能[cast to 原始类型]
            validateClass(name, clazz);
//            validateClass(beanName, clazz);

            // 触发spring创建bean
            bean = retrieveDynamicBean(beanName, clazz);
        }

        CLASS_REGISTERED_BEAN_MAP.put(originalFullClassName, name);

        result.setBean(bean);
        result.setOriginalBeanName(name);
        result.setBeanName(beanName);
        this.dynamicBeanCache.put(beanName, result);
        return result;
    }

    /**
     * 检查类是否合法
     * @param originalFullClassName
     */
    private void validateClass(String originalFullClassName) {
        try {
            Class.forName(originalFullClassName, false, this.springBeanFactory.getBeanClassLoader());
            if (!CompileUtil.isLoadedDynamicClass(originalFullClassName)) {
                throw new IllegalStateException("不允许覆盖加载已存在的非动态类,请修改类名!");
            }
        } catch (ClassNotFoundException e) {
        }
    }

    /**
     * 验证新类型是否合法
     * <br>严格杜绝的情况：name是已存在的spring bean, 但新类型不能[cast to 原始类型]
     *
     * @param name
     * @param clazz
     */
    private void validateClass(String name, Class<?> clazz) {
        if (!this.springBeanFactory.containsBean(name)) {
            return;
        }
        Class<?> originClazz = this.springBeanFactory.getType(name);
        if (originClazz == null) {
            return;
        }
        if (originClazz.isAssignableFrom(clazz)) {
            return;
        }
        Class<?> originSuperClass = lookBackSuperClass(originClazz);
        if (originSuperClass != null && originSuperClass.isAssignableFrom(clazz)) {
            return;
        }
        throw new IllegalStateException(
                String.format("不允许注册,ClassCastException: %s cannot be cast to %s", clazz,
                        originSuperClass == null ? originClazz : originSuperClass));
    }

    /**
     * 回溯父类
     * @param clazz
     * @return
     */
    private Class<?> lookBackSuperClass(Class<?> clazz) {
        if (clazz.getSuperclass() == Object.class) {
            return null;
        }
        Class<?> result = lookBackSuperClass(clazz.getSuperclass());
        if (result == null) {
            return clazz.getSuperclass();
        }
        return result;
    }

    /**
     * 创建动态bean的工厂方法
     * @param clazz
     * @return
     * @throws Exception
     */
    public Object getObject(Class<?> clazz) throws Exception {
        return clazz.newInstance();
    }

    /**
     * 触发spring创建bean
     *
     * @param beanName
     * @return
     */
    private Object retrieveDynamicBean(String beanName, Class<?> clazz) {
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
        bd.setFactoryBeanName(BEAN_NAME);
        bd.setFactoryMethodName("getObject");
        bd.getConstructorArgumentValues().clear();
        bd.getConstructorArgumentValues().addGenericArgumentValue(clazz);
        this.springBeanFactory.registerBeanDefinition(beanName, bd);

        Object bean = this.springBeanFactory.getBean(beanName);
        for (DynamicBeanProcessor processor : dynamicBeanProcessors) {
            Object processed =  processor.processRegister(beanName, bean, clazz);
            if (processed == null) {
                break;
            }
            bean = processed;
        }
        return bean;
    }

    /**
     * 返回指定name的已创建动态bean
     * @param name
     * @return null表示动态bean还未创建
     */
    public DynamicBeanInfo getDyanmicBean(String name) {
        return this.dynamicBeanCache.get(transformedBeanName(name));
    }

    /**
     * 卸载一个动态bean
     *
     * @param name
     */
    public void unregisterBean(String name) {
        String beanName = transformedBeanName(name);

        DynamicBeanInfo dynamicBeanInfo = dynamicBeanCache.remove(beanName);
        if (dynamicBeanInfo != null) {
            CLASS_REGISTERED_BEAN_MAP.remove(dynamicBeanInfo.getOriginalFullClassName());

            Object bean = dynamicBeanInfo.getBean();
            for (DynamicBeanProcessor processor : dynamicBeanProcessors) {
                Object processed = processor.processUnregister(beanName, bean, bean.getClass());
                if (processed == null) {
                    break;
                }
                bean = processed;
            }
        }

        if (this.springBeanFactory.containsBean(beanName)) {
            this.springBeanFactory.removeBeanDefinition(beanName);
        }
    }

    /**
     * 返回全部动态bean
     * @return
     */
    public Map<String, DynamicBeanInfo> allDynamicBeans() {
        return this.dynamicBeanCache;
    }

    private String transformedBeanName(String name) {
        return DYNAMIC_BEAN_NAME_PREFIX + name;
    }

    private String getBeanName(Class<?> beanClass) {
        AnnotationAttributes annotationAttributes = AnnotatedElementUtils
                .getMergedAnnotationAttributes(beanClass, Component.class);
        if (annotationAttributes == null) {
            return null;
        }

        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
        return this.beanNameGenerator.generateBeanName(abd, this.springBeanFactory);
    }
}
