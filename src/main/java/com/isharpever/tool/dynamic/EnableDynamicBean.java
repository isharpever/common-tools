package com.isharpever.tool.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * 开启动态bean功能
 *
 * @author yinxiaolin
 * @since 2019/8/14
 * @see DynamicBeanConfigurationRegistrar
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import({DynamicBeanConfigurationRegistrar.class})
public @interface EnableDynamicBean {

    /**
     * true:只支持动态新增bean<br>
     * false:支持新增bean, 也支持动态更新已有的bean (范围受basePackages和classes限制)
     */
    boolean onlyNewBean() default true;

    /**
     * 指明哪些package下的bean可以被动态更新, 未指定时是本注解所在类的package<br>
     * onlyNewBean=false时有效
     * @see DynamicBeanAutoProxyCreator#isTypeMatch
     */
    String[] basePackages() default {};

    /**
     * 指明哪些class的bean可以被动态更新<br>
     * onlyNewBean=false时有效
     * @see DynamicBeanAutoProxyCreator#isTypeMatch
     */
    Class<?>[] classes() default {};
}
