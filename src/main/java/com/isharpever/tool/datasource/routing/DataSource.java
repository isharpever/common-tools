package com.isharpever.tool.datasource.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DataSource {
    /**
     * 数据源lookupKey, 可使用常量{@link LookupKeyConstant}
     */
    String value();

    /**
     * 传播属性
     */
    Propagation propagation() default Propagation.NEW;

    public enum Propagation {
        /**
         * 使用当前线程上下文中已有的数据源(一般是由于外层方法上指定了@DataSource),若没有则新选择一个
         */
        INHERIT,
        /**
         * 永远重新选择数据源
         */
        NEW
    }
}
